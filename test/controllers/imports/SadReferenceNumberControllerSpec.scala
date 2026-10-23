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

package controllers.imports

import base.SpecBase
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import play.api.inject.bind
import models.{CheckMode, NormalMode, UserAnswers}
import models.responses.{AddImportResponse, AddPurchaseResponse, ApplicationResponse, SadReferenceCountResponse}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import pages.AddImportResponsePage
import pages.purchase.AddPurchaseResponsePage
import play.api.mvc.Call
import queries.ClaimApplicationResponseQuery
import repositories.SessionRepository

import scala.concurrent.Future

class SadReferenceNumberControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute: Call = Call("GET", "/foo")
  val formProvider = new forms.imports.SadReferenceNumberFormProvider()
  lazy val sadReferenceNumberRoute: String = routes.SadReferenceNumberController.onPageLoad(NormalMode).url

  val seededAnswers: UserAnswers = emptyUserAnswers
    .set(ClaimApplicationResponseQuery, ApplicationResponse(134, "GB123134", 1))
    .success
    .value
    .set(AddImportResponsePage, AddImportResponse(1, 2))
    .success
    .value

  "SadReferenceNumber Controller" - {

    "must return OK and the correct view for a GET" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, sadReferenceNumberRoute)
        val result = route(application, request).value

        status(result) mustBe OK
        contentType(result) mustBe Some("text/html")
      }
    }

    "must redirect to the next page when valid data is submitted" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(POST, sadReferenceNumberRoute)
          .withFormUrlEncodedBody("value" -> "ABC123")

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value must not be empty
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(POST, sadReferenceNumberRoute)
          .withFormUrlEncodedBody("value" -> "")

        val result = route(application, request).value

        status(result) mustBe BAD_REQUEST
      }
    }

    "must redirect to the next page when no duplicate is found" in {
      val mockSessionRepository = mock[SessionRepository]
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
      when(mockEuVatRefundsService.getSadReferenceCount(any())(any()))
        .thenReturn(Future.successful(SadReferenceCountResponse(0)))

      val application =
        applicationBuilder(userAnswers = Some(seededAnswers))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, sadReferenceNumberRoute)
            .withFormUrlEncodedBody(("value", "FR123456789"))

        val result = route(application, request).value
        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must redirect to the warning page when a duplicate is found" in {
      val mockSessionRepository = mock[SessionRepository]
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
      when(mockEuVatRefundsService.getSadReferenceCount(any())(any()))
        .thenReturn(Future.successful(SadReferenceCountResponse(1)))

      val application =
        applicationBuilder(userAnswers = Some(seededAnswers))
          .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, sadReferenceNumberRoute)
            .withFormUrlEncodedBody(("value", "FR123456789"))

        val result = route(application, request).value
        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.warning.routes.SadReferenceNumberWarningController.onPageLoad(NormalMode).url
      }
    }

    "must redirect to Journey Recovery when the duplicate check fails" in {
      val mockSessionRepository = mock[SessionRepository]
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
      when(mockEuVatRefundsService.getSadReferenceCount(any())(any()))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      val application =
        applicationBuilder(userAnswers = Some(seededAnswers))
          .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, sadReferenceNumberRoute)
            .withFormUrlEncodedBody(("value", "FR123456789"))

        val result = route(application, request).value
        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery when required cache data is missing" in {
      val mockSessionRepository = mock[SessionRepository]
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, sadReferenceNumberRoute)
            .withFormUrlEncodedBody(("value", "FR123456789"))

        val result = route(application, request).value
        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Check Your Import Details when no duplicate is found in CheckMode" in {
      val mockSessionRepository = mock[SessionRepository]
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
      when(mockEuVatRefundsService.getSadReferenceCount(any())(any()))
        .thenReturn(Future.successful(SadReferenceCountResponse(0)))

      val application = applicationBuilder(userAnswers = Some(seededAnswers))
        .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
        .build()

      running(application) {
        val request = FakeRequest(POST, routes.SadReferenceNumberController.onSubmit(CheckMode).url)
          .withFormUrlEncodedBody(("value", "FR123456789"))

        val result = route(application, request).value
        status(result) mustEqual SEE_OTHER
        // TODO update import check your answer
        redirectLocation(result).value mustEqual routes.ImportDateController.onPageLoad(CheckMode).url
      }
    }

    "must redirect to to the warning page when a duplicate is found in CheckMode" in {
      val mockSessionRepository = mock[SessionRepository]
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
      when(mockEuVatRefundsService.getSadReferenceCount(any())(any()))
        .thenReturn(Future.successful(SadReferenceCountResponse(1)))

      val application = applicationBuilder(userAnswers = Some(seededAnswers))
        .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
        .build()

      running(application) {
        val request = FakeRequest(POST, routes.SadReferenceNumberController.onSubmit(CheckMode).url)
          .withFormUrlEncodedBody(("value", "FR123456789"))

        val result = route(application, request).value
        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.warning.routes.SadReferenceNumberWarningController.onPageLoad(CheckMode).url
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {
      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, sadReferenceNumberRoute)
        val result = route(application, request).value
        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {
      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, sadReferenceNumberRoute)
            .withFormUrlEncodedBody(("value", "FR123456789"))

        val result = route(application, request).value
        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
