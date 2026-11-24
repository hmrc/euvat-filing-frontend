/*
 * Copyright 2026 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers.supportingdocuments

import base.SpecBase
import models.requests.GetAllAttachmentsRequest
import models.responses.*
import models.{Attachment, UserAnswers}
import org.jsoup.Jsoup
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{any, eq as mockitoEq}
import org.mockito.Mockito.{verify, when}
import play.api.Application
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.{ClaimApplicationResponseQuery, SupportingDocumentsCompletedQuery, UploadedFilesQuery}
import repositories.SessionRepository

import java.util.Base64
import scala.concurrent.Future

class UploadedFilesControllerSpec extends SpecBase {

  private val userAnswers: UserAnswers =
    emptyUserAnswers.set(ClaimApplicationResponseQuery, ApplicationResponse(111, "App1", 1)).success.value

  private val attachments = List(
    Attachment(1, Some("hotel-invoice.pdf"), Some("pdf"), Some("Hotel invoice"), Some(1825)),
    Attachment(2, Some("fuel-receipt.jpg"), Some("jpg"), None, Some(200))
  )

  private def attachment(number: Int): Attachment =
    Attachment(number, Some(s"invoice-$number.pdf"), Some("pdf"), Some(s"Invoice $number"), Some(1024))

  private def stubAttachments(response: GetAllAttachmentsResponse): Unit =
    when(mockEuVatRefundsService.getAllAttachments(any())(any())).thenReturn(Future.successful(response))

  private def application(answers: UserAnswers = userAnswers): Application =
    applicationBuilder(userAnswers = Some(answers))
      .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
      .build()

  "UploadedFiles Controller" - {

    "onPageLoad" - {

      "must return OK and list the uploaded files" in {
        stubAttachments(GetAllAttachmentsResponse(attachments, 2))
        val app = application()

        running(app) {
          val result = route(app, FakeRequest(GET, routes.UploadedFilesController.onPageLoad().url)).value
          val document = Jsoup.parse(contentAsString(result))
          val msgs = messages(app)

          status(result) mustEqual OK
          document.title() must startWith(msgs("supportingDocuments.uploadedFiles.title"))
          document.select("h1").text() mustEqual msgs("supportingDocuments.uploadedFiles.heading")
          document.text() must include(msgs("supportingDocuments.uploadedFiles.hint"))
          document.text() must include(msgs("supportingDocuments.uploadedFiles.count.multiple", 2))
          document.select("table.govuk-table caption").text() mustEqual msgs("supportingDocuments.uploadedFiles.table.caption")
          document.select("table.govuk-table tbody tr").size() mustBe 2
          document.text() must include("hotel-invoice.pdf")
          document.text() must include("1.78MB")
          document.text() must include("0.20MB")
          document.text() must include(msgs("site.notProvided"))
          document.select("a[href='/file-eu-vat/supporting-documents/remove-file/1']").size() mustBe 1
          document.select(s"a[href='${routes.UploadFileController.onPageLoad().url}']").text() mustEqual
            msgs("supportingDocuments.uploadedFiles.uploadAnother")
          document.select("nav.govuk-pagination").size() mustBe 0
        }
      }

      "must save the listed files in session" in {
        stubAttachments(GetAllAttachmentsResponse(attachments, 2))
        val app = application()

        running(app) {
          val result = route(app, FakeRequest(GET, routes.UploadedFilesController.onPageLoad().url)).value
          status(result) mustEqual OK

          val savedAnswers = ArgumentCaptor.forClass(classOf[UserAnswers])
          verify(mockSessionRepository).set(savedAnswers.capture())
          savedAnswers.getValue.get(UploadedFilesQuery) mustEqual Some(attachments)
        }
      }

      "must request the first page of 10 files sorted by sequence number ascending" in {
        stubAttachments(GetAllAttachmentsResponse(attachments, 2))
        val app = application()

        running(app) {
          val result = route(app, FakeRequest(GET, routes.UploadedFilesController.onPageLoad().url)).value
          status(result) mustEqual OK

          val captor = ArgumentCaptor.forClass(classOf[GetAllAttachmentsRequest])
          verify(mockEuVatRefundsService).getAllAttachments(captor.capture())(any())
          captor.getValue mustEqual GetAllAttachmentsRequest(
            applicationId = 111,
            orderBy       = Some(0),
            sortOrder     = Some("ASC"),
            startAt       = Some(0),
            maxNumber     = Some(10)
          )
        }
      }

      "must show the single file count when one file is uploaded" in {
        stubAttachments(GetAllAttachmentsResponse(List(attachments.head), 1))
        val app = application()

        running(app) {
          val result = route(app, FakeRequest(GET, routes.UploadedFilesController.onPageLoad().url)).value
          val document = Jsoup.parse(contentAsString(result))

          status(result) mustEqual OK
          document.text() must include(messages(app)("supportingDocuments.uploadedFiles.count.single"))
        }
      }

      "must show the no files state when nothing is uploaded" in {
        stubAttachments(GetAllAttachmentsResponse(Nil, 0))
        val app = application()

        running(app) {
          val result = route(app, FakeRequest(GET, routes.UploadedFilesController.onPageLoad().url)).value
          val document = Jsoup.parse(contentAsString(result))
          val msgs = messages(app)

          status(result) mustEqual OK
          document.text() must include(msgs("supportingDocuments.uploadedFiles.count.none"))
          document.select(s"a[href='${routes.UploadFileController.onPageLoad().url}']").text() mustEqual
            msgs("supportingDocuments.uploadedFiles.uploadFile")
          document.select("table.govuk-table").size() mustBe 0
        }
      }

      "must show pagination and the page number in the title when there are more than 10 files" in {
        stubAttachments(GetAllAttachmentsResponse((1 to 10).toList.map(attachment), 12))
        val app = application()

        running(app) {
          val result = route(app, FakeRequest(GET, routes.UploadedFilesController.onPageLoad().url)).value
          val document = Jsoup.parse(contentAsString(result))

          status(result) mustEqual OK
          document.select("table.govuk-table tbody tr").size() mustBe 10
          document.select("nav.govuk-pagination").size() mustBe 1
          document.select("a[href$='?page=2']").isEmpty mustBe false
          document.title() must include("(page 1 of 2)")
        }
      }

      "must request the second page of files when page 2 is selected" in {
        stubAttachments(GetAllAttachmentsResponse(List(attachment(11), attachment(12)), 12))
        val app = application()

        running(app) {
          val result = route(app, FakeRequest(GET, routes.UploadedFilesController.onPageLoad(2).url)).value
          status(result) mustEqual OK

          val captor = ArgumentCaptor.forClass(classOf[GetAllAttachmentsRequest])
          verify(mockEuVatRefundsService).getAllAttachments(captor.capture())(any())
          captor.getValue.startAt mustEqual Some(10)
          captor.getValue.maxNumber mustEqual Some(10)
        }
      }

      "must redirect to the last page when the selected page no longer exists" in {
        stubAttachments(GetAllAttachmentsResponse(Nil, 12))
        val app = application()

        running(app) {
          val result = route(app, FakeRequest(GET, routes.UploadedFilesController.onPageLoad(5).url)).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.UploadedFilesController.onPageLoad(2).url
        }
      }

      "must redirect to the first page when the page is below 1" in {
        val app = application()

        running(app) {
          val result = route(app, FakeRequest(GET, routes.UploadedFilesController.onPageLoad(0).url)).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.UploadedFilesController.onPageLoad().url
        }
      }

      "must redirect to Journey Recovery when the applicationId is missing" in {
        val app = application(emptyUserAnswers)

        running(app) {
          val result = route(app, FakeRequest(GET, routes.UploadedFilesController.onPageLoad().url)).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to Journey Recovery when retrieving the attachments fails" in {
        when(mockEuVatRefundsService.getAllAttachments(any())(any())).thenReturn(Future.failed(new RuntimeException("boom")))
        val app = application()

        running(app) {
          val result = route(app, FakeRequest(GET, routes.UploadedFilesController.onPageLoad().url)).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }

    "onSubmit" - {

      "must mark supporting documents as completed and redirect to the task list" in {
        val app = application()

        running(app) {
          val result = route(app, FakeRequest(POST, routes.UploadedFilesController.onSubmit().url)).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.TaskListDashboardController.onPageLoad().url

          val savedAnswers = ArgumentCaptor.forClass(classOf[UserAnswers])
          verify(mockSessionRepository).set(savedAnswers.capture())
          savedAnswers.getValue.get(SupportingDocumentsCompletedQuery) mustEqual Some(true)
        }
      }
    }

    "onDownload" - {

      val fileBytes = "test file content".getBytes("UTF-8")

      val fullAttachment = GetFullAttachmentDetailsResponse(
        attachmentNumber     = 1,
        fileName             = Some("hotel-invoice.pdf"),
        fileType             = Some("pdf"),
        fileDescription      = Some("Hotel invoice"),
        fileSize             = Some(1825),
        fileContent          = Some(Base64.getEncoder.encodeToString(fileBytes)),
        updateSequenceNumber = 1
      )

      def downloadRequest = FakeRequest(GET, routes.UploadedFilesController.onDownload(1).url)

      "must return the decoded file with the correct content type and download header" in {
        when(mockEuVatRefundsService.getFullAttachmentDetails(mockitoEq(111L), mockitoEq(1))(any()))
          .thenReturn(Future.successful(fullAttachment))
        val app = application()

        running(app) {
          val result = route(app, downloadRequest).value

          status(result) mustEqual OK
          contentType(result).value mustEqual "application/pdf"
          header(CONTENT_DISPOSITION, result).value mustEqual """attachment; filename="hotel-invoice.pdf""""
          contentAsBytes(result).toArray mustEqual fileBytes
        }
      }

      "must redirect to Journey Recovery when the applicationId is missing" in {
        val app = application(emptyUserAnswers)

        running(app) {
          val result = route(app, downloadRequest).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to Journey Recovery when retrieving the attachment fails" in {
        when(mockEuVatRefundsService.getFullAttachmentDetails(any(), any())(any()))
          .thenReturn(Future.failed(new RuntimeException("boom")))
        val app = application()

        running(app) {
          val result = route(app, downloadRequest).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to Journey Recovery when the file content is missing" in {
        when(mockEuVatRefundsService.getFullAttachmentDetails(any(), any())(any()))
          .thenReturn(Future.successful(fullAttachment.copy(fileContent = None)))
        val app = application()

        running(app) {
          val result = route(app, downloadRequest).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }
  }
}
