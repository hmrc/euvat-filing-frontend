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
import models.{NormalMode, RefundingCurrency, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{times, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.{ImportCurrencyPage, RefundingCountryPage, RefundingCurrencyPage}
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository

import scala.concurrent.Future

class ImportCurrencyControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute: Call = Call("GET", "/foo")
  lazy val importCurrencyRoute: String = routes.ImportCurrencyController.onPageLoad(NormalMode).url
  val userAnswersWithEstonia: UserAnswers = emptyUserAnswers.set(RefundingCountryPage, "EE").success.value

  "ImportCurrency Controller" - {

    "must return OK and the correct view for a GET when country is Estonia" in {
      val application = applicationBuilder(userAnswers = Some(userAnswersWithEstonia)).build()

      running(application) {
        val request = FakeRequest(GET, importCurrencyRoute)
        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) must include(messages(application)("import.refundingCurrency.heading"))
        contentAsString(result) must include(messages(application)("import.caption"))
        contentAsString(result) must include("Euro (€)")
        contentAsString(result) must include("Estonian Kroon (kr)")
        contentAsString(result) must include(routes.ImportSuppliersNameController.onPageLoad(NormalMode).url)
      }
    }

    "must return OK on a GET when the question has previously been answered" in {
      val userAnswers = userAnswersWithEstonia.set(ImportCurrencyPage, "EUR").success.value
      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, importCurrencyRoute)
        val result = route(application, request).value

        status(result) mustEqual OK
      }
    }

    "must redirect to Journey Recovery for a GET if no country is in session" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, importCurrencyRoute)
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {
      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, importCurrencyRoute)
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must save the currency code to ImportCurrencyPage and redirect to the next page when valid data is submitted" in {
      val mockSessionRepository = mock[SessionRepository]
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(userAnswers = Some(userAnswersWithEstonia))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, importCurrencyRoute)
            .withFormUrlEncodedBody(("value", RefundingCurrency.Euro.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url

        val captor = ArgumentCaptor.forClass(classOf[UserAnswers])
        verify(mockSessionRepository, times(1)).set(captor.capture())
        captor.getValue.get(ImportCurrencyPage) mustBe Some("EUR")
        captor.getValue.get(RefundingCurrencyPage) mustBe None
      }
    }

    "must return a Bad Request and the import error message when no value is submitted" in {
      val application = applicationBuilder(userAnswers = Some(userAnswersWithEstonia)).build()

      running(application) {
        val request =
          FakeRequest(POST, importCurrencyRoute)
            .withFormUrlEncodedBody(("value", ""))

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) must include(messages(application)("import.refundingCurrency.error.required"))
      }
    }

    "must return a Bad Request when invalid data is submitted" in {
      val application = applicationBuilder(userAnswers = Some(userAnswersWithEstonia)).build()

      running(application) {
        val request =
          FakeRequest(POST, importCurrencyRoute)
            .withFormUrlEncodedBody(("value", "invalid value"))

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
      }
    }

    "must redirect to Journey Recovery for a POST if no country is in session" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, importCurrencyRoute)
            .withFormUrlEncodedBody(("value", RefundingCurrency.Euro.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {
      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, importCurrencyRoute)
            .withFormUrlEncodedBody(("value", RefundingCurrency.Euro.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
