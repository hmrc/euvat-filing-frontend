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
import models.responses.{ApplicationResponse, CreateUploadTrackingResponse}
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.{any, eq as mockitoEq}
import org.mockito.Mockito.{never, verify, when}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.ClaimApplicationResponseQuery
import uk.gov.hmrc.http.UpstreamErrorResponse

import scala.concurrent.Future

class UploadFileControllerSpec extends SpecBase {

  private val applicationId = 133L
  private lazy val onPageLoadUrl = routes.UploadFileController.onPageLoad().url

  private val uploadTracking = CreateUploadTrackingResponse(
    reference = "ref-123",
    uploadUrl = "http://localhost:9570/upscan/upload-proxy",
    fields    = Map("key" -> "ref-123", "x-amz-algorithm" -> "AWS4-HMAC-SHA256")
  )

  private val userAnswersWithApplication =
    emptyUserAnswers
      .set(ClaimApplicationResponseQuery, ApplicationResponse(applicationId = applicationId, applicationNumber = "GB123456789", updateSeqNumber = 1))
      .success
      .value

  // SpecBase binds mockEuVatRefundsService into every application and resets it before each test,
  // so each test only needs to stub it
  private def uploadTrackingReturns(result: Future[CreateUploadTrackingResponse]): Unit =
    when(mockEuVatRefundsService.createUploadTracking(any[Long]())(any())).thenReturn(result)

  private def applicationWithUpload = {
    uploadTrackingReturns(Future.successful(uploadTracking))
    applicationBuilder(userAnswers = Some(userAnswersWithApplication)).build()
  }

  "UploadFileController.onPageLoad" - {

    "must start an upscan upload for the application" in {
      val application = applicationWithUpload

      running(application) {
        status(route(application, FakeRequest(GET, onPageLoadUrl)).value) mustEqual OK
        verify(mockEuVatRefundsService).createUploadTracking(mockitoEq(applicationId))(any())
      }
    }

    "must display the upload page as designed" in {
      val application = applicationWithUpload

      running(application) {
        val result = route(application, FakeRequest(GET, onPageLoadUrl)).value
        val content = contentAsString(result)

        status(result) mustEqual OK
        header("Cache-Control", result).value mustEqual "no-store"
        onPageLoadUrl mustEqual "/file-eu-vat/supporting-documents/upload-file"
        content must include("<title>Upload file - EU VAT - GOV.UK</title>")
        content must include("<h1 class=\"govuk-label-wrapper\">")
        content must include(s"""<span class="govuk-caption-l">${messages(application)("supportingDocuments.caption")}</span>""")
        content must include(messages(application)("supportingDocuments.uploadFile.file.hint"))
        content must include("data-module=\"govuk-file-upload\"")
        content must include("""href="#" class="govuk-back-link" data-module="hmrc-back-link">Back</a>""")
        content must not include "fileuploadDescription"
      }
    }

    "must post the file straight to upscan with the upscan fields" in {
      val application = applicationWithUpload

      running(application) {
        val content = contentAsString(route(application, FakeRequest(GET, onPageLoadUrl)).value)

        content must include(s"""action="${uploadTracking.uploadUrl}"""")
        content must include("""enctype="multipart/form-data"""")
        content must include("""name="key" value="ref-123"""")
        content must include("""name="x-amz-algorithm" value="AWS4-HMAC-SHA256"""")
        content must not include "csrfToken"
      }
    }

    "must make the file the last field in the upscan form, with the button inside the form" in {
      val application = applicationWithUpload

      running(application) {
        val upscanForm = Jsoup.parse(contentAsString(route(application, FakeRequest(GET, onPageLoadUrl)).value)).getElementById("upscan-form")
        val fieldNames = upscanForm.select("input[name]").eachAttr("name")

        fieldNames.get(fieldNames.size - 1) mustBe "file"
        upscanForm.select("button[type=submit]").size mustBe 1
      }
    }

    "must show an error when upscan sends the user back with an error code" in {
      val application = applicationWithUpload

      running(application) {
        val result = route(application, FakeRequest(GET, s"$onPageLoadUrl?key=ref-123&errorCode=EntityTooLarge")).value
        val content = contentAsString(result)

        status(result) mustEqual OK
        content must include(
          s"""<title>${messages(application)("error.title.prefix")} ${messages(application)("supportingDocuments.uploadFile.title")}"""
        )
        content must include(messages(application)("supportingDocuments.uploadFile.error.size"))
        content must include("""id="file-error"""")
        content must include("""aria-describedby="file-error file-hint"""")
        content must include("""href="#file"""")
      }
    }

    "must show the empty-file error when upscan reports the file is too small" in {
      val application = applicationWithUpload

      running(application) {
        val content = contentAsString(route(application, FakeRequest(GET, s"$onPageLoadUrl?errorCode=EntityTooSmall")).value)

        content must include(messages(application)("supportingDocuments.uploadFile.error.empty"))
        content must not include messages(application)("supportingDocuments.uploadFile.error.required")
      }
    }

    "must redirect to Journey Recovery when the upload cannot be started" in {
      uploadTrackingReturns(Future.failed(UpstreamErrorResponse("No application found", 404)))
      val application = applicationBuilder(userAnswers = Some(userAnswersWithApplication)).build()

      running(application) {
        val result = route(application, FakeRequest(GET, onPageLoadUrl)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery without starting an upload when there is no application id" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val result = route(application, FakeRequest(GET, onPageLoadUrl)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
        verify(mockEuVatRefundsService, never()).createUploadTracking(any[Long]())(any())
      }
    }
  }
}
