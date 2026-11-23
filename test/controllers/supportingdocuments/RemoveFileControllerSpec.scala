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
import forms.supportingdocuments.RemoveFileFormProvider
import models.responses.{ApplicationResponse, AttachmentDetailsResponse, DeleteAttachmentResponse}
import models.UserAnswers
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{never, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.{ClaimApplicationResponseQuery, RemoveFileAttachmentQuery, UpdateSequenceNumberQuery}
import repositories.SessionRepository
import views.html.supportingdocuments.RemoveFileView

import scala.concurrent.Future

class RemoveFileControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute = Call("GET", "/foo")

  val formProvider = new RemoveFileFormProvider()
  val form = formProvider()

  val attachmentNumber = 1
  val updateSequenceNumber = 1

  lazy val removeFileRoute = routes.RemoveFileController.onPageLoad(attachmentNumber).url

  "RemoveFile Controller" - {

    val populatedAnswers: UserAnswers = emptyUserAnswers
      .set(ClaimApplicationResponseQuery, ApplicationResponse(123, "APP123", 1))
      .success
      .value
      .set(UpdateSequenceNumberQuery, updateSequenceNumber)
      .success
      .value

    val getAttachmentDetailsResponse: AttachmentDetailsResponse = AttachmentDetailsResponse(
      attachmentNumber     = attachmentNumber,
      fileName             = None,
      fileType             = None,
      fileDescription      = None,
      fileSize             = None,
      updateSequenceNumber = updateSequenceNumber
    )

    "must return OK and the correct view for a GET" in {

      when(mockEuVatRefundsService.getAttachmentDetails(any(), any())(any()))
        .thenReturn(Future.successful(getAttachmentDetailsResponse))

      val application = applicationBuilder(userAnswers = Some(populatedAnswers))
        .build()

      running(application) {
        val request = FakeRequest(GET, removeFileRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[RemoveFileView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, getAttachmentDetailsResponse)(request, messages(application)).toString

        verify(mockEuVatRefundsService).getAttachmentDetails(any(), any())(any())
      }
    }

    "must update UpdateSequenceNumberQuery after calling the getAttachmentDetails for a GET" in {

      when(mockEuVatRefundsService.getAttachmentDetails(any(), any())(any()))
        .thenReturn(Future.successful(getAttachmentDetailsResponse))

      val application = applicationBuilder(userAnswers = Some(populatedAnswers))
        .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
        .build()

      running(application) {
        val request = FakeRequest(GET, removeFileRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[RemoveFileView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, getAttachmentDetailsResponse)(request, messages(application)).toString

        val savedAnswersCaptor = ArgumentCaptor.forClass(classOf[UserAnswers])
        verify(mockSessionRepository).set(savedAnswersCaptor.capture())
        savedAnswersCaptor.getValue.get(UpdateSequenceNumberQuery) mustBe Some(getAttachmentDetailsResponse.updateSequenceNumber)

        verify(mockEuVatRefundsService).getAttachmentDetails(any(), any())(any())
      }
    }

    "must redirect to JourneyRecoveryController when the getAttachmentDetails fails for a GET" in {
      when(mockEuVatRefundsService.getAttachmentDetails(any(), any())(any()))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      val application = applicationBuilder(userAnswers = Some(populatedAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, removeFileRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[RemoveFileView]

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url

        verify(mockEuVatRefundsService).getAttachmentDetails(any(), any())(any())
      }
    }

    "must not call the getAttachmentDetails when the specific attachment details in session for a GET" in {

      val attachmentInSession = AttachmentDetailsResponse(
        attachmentNumber     = attachmentNumber,
        fileName             = None,
        fileType             = None,
        fileDescription      = None,
        fileSize             = None,
        updateSequenceNumber = updateSequenceNumber
      )

      val userAnswers: UserAnswers = emptyUserAnswers
        .set(ClaimApplicationResponseQuery, ApplicationResponse(123, "APP123", 1))
        .success
        .value
        .set(RemoveFileAttachmentQuery, attachmentInSession)
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
        .build()

      running(application) {
        val request = FakeRequest(GET, removeFileRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[RemoveFileView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, attachmentInSession)(request, messages(application)).toString

        verify(mockEuVatRefundsService, never()).getAttachmentDetails(any(), any())(any())
      }
    }

    "must redirect to the UploadedFiles page when valid data (true) is submitted" in {

      when(mockEuVatRefundsService.deleteAttachment(any())(any()))
        .thenReturn(Future.successful(DeleteAttachmentResponse(updateSequenceNumber + 1)))

      val application =
        applicationBuilder(userAnswers = Some(populatedAnswers))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, removeFileRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.PurchaseImportSummaryController.onPageLoad.url

        verify(mockEuVatRefundsService).deleteAttachment(any())(any())
      }
    }

    "must update UpdateSequenceNumberQuery when valid data (true) is submitted" in {

      when(mockEuVatRefundsService.deleteAttachment(any())(any()))
        .thenReturn(Future.successful(DeleteAttachmentResponse(updateSequenceNumber + 1)))

      val application =
        applicationBuilder(userAnswers = Some(populatedAnswers))
          .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, removeFileRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.PurchaseImportSummaryController.onPageLoad.url

        val savedAnswersCaptor = ArgumentCaptor.forClass(classOf[UserAnswers])
        verify(mockSessionRepository).set(savedAnswersCaptor.capture())
        savedAnswersCaptor.getValue.get(UpdateSequenceNumberQuery) mustBe Some(getAttachmentDetailsResponse.updateSequenceNumber + 1)
      }
    }

    "must redirect to Journey Recovery if deleteAttachment fail when valid data (true) is submitted" in {

      when(mockEuVatRefundsService.deleteAttachment(any())(any()))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      val application =
        applicationBuilder(userAnswers = Some(populatedAnswers))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, removeFileRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url

        verify(mockEuVatRefundsService).deleteAttachment(any())(any())
      }
    }

    "must redirect to the UploadedFiles page when valid data (false) is submitted" in {
      val application =
        applicationBuilder(userAnswers = Some(populatedAnswers))
          .build()

      running(application) {
        val request =
        FakeRequest(POST, removeFileRoute)
          .withFormUrlEncodedBody(("value", "false"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.PurchaseImportSummaryController.onPageLoad.url

        verify(mockEuVatRefundsService, never()).deleteAttachment(any())(any())
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      when(mockEuVatRefundsService.getAttachmentDetails(any(), any())(any()))
        .thenReturn(Future.successful(getAttachmentDetailsResponse))

      val application = applicationBuilder(userAnswers = Some(populatedAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, removeFileRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[RemoveFileView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, getAttachmentDetailsResponse)(request, messages(application)).toString

        verify(mockEuVatRefundsService).getAttachmentDetails(any(), any())(any())
      }
    }

    "must not call the getAttachmentDetails if the specific attachment details are in session when invalid data is submitted" in {

      val attachmentInSession = AttachmentDetailsResponse(
        attachmentNumber = attachmentNumber,
        fileName = None,
        fileType = None,
        fileDescription = None,
        fileSize = None,
        updateSequenceNumber = updateSequenceNumber
      )

      val userAnswers: UserAnswers = emptyUserAnswers
        .set(ClaimApplicationResponseQuery, ApplicationResponse(123, "APP123", 1))
        .success
        .value
        .set(UpdateSequenceNumberQuery, updateSequenceNumber)
        .success
        .value
        .set(RemoveFileAttachmentQuery, attachmentInSession)
        .success
        .value

      when(mockEuVatRefundsService.getAttachmentDetails(any(), any())(any()))
        .thenReturn(Future.successful(getAttachmentDetailsResponse))

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, removeFileRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[RemoveFileView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, getAttachmentDetailsResponse)(request, messages(application)).toString

        verify(mockEuVatRefundsService, never).getAttachmentDetails(any(), any())(any())
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, removeFileRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url

        verify(mockEuVatRefundsService, never()).getAttachmentDetails(any(), any())(any())
      }
    }

    "must redirect to Journey Recovery for a GET if applicationId is not found in session" in {

      when(mockEuVatRefundsService.getAttachmentDetails(any(), any())(any()))
        .thenReturn(Future.successful(getAttachmentDetailsResponse))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .build()

      running(application) {
        val request = FakeRequest(GET, removeFileRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[RemoveFileView]

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url

        verify(mockEuVatRefundsService, never()).getAttachmentDetails(any(), any())(any())
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, removeFileRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url

        verify(mockEuVatRefundsService, never()).getAttachmentDetails(any(), any())(any())
      }
    }

    "must redirect to Journey Recovery for a POST if applicationId is not found in session" in {

      val userAnswers: UserAnswers = emptyUserAnswers
        .set(UpdateSequenceNumberQuery, updateSequenceNumber)
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, removeFileRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url

        verify(mockEuVatRefundsService, never()).getAttachmentDetails(any(), any())(any())
      }
    }

    "must redirect to Journey Recovery for a POST if UpdateSequenceNumberQuery is not found in session" in {

      val userAnswers: UserAnswers = emptyUserAnswers
        .set(ClaimApplicationResponseQuery, ApplicationResponse(123, "APP123", 1))
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, removeFileRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url

        verify(mockEuVatRefundsService, never()).getAttachmentDetails(any(), any())(any())
      }
    }

  }
}
