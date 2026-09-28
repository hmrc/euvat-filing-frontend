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

package controllers.claim

import base.SpecBase
import config.FrontendAppConfig
import forms.claim.CheckYourStateDetailsFormProvider
import models.responses.ApplicationResponse
import models.{NormalMode, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.CheckYourStateDetailsPage
import play.api.data.Form
import play.api.inject.bind
import play.api.libs.json.Json
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.ClaimApplicationResponseQuery
import repositories.SessionRepository
import views.html.claim.CheckYourStateDetailsView

import scala.concurrent.Future

class CheckYourStateDetailsControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute: Call = Call("GET", "/foo")

  val formProvider = new CheckYourStateDetailsFormProvider()
  val form: Form[Boolean] = formProvider()

  lazy val checkYourStateDetailsRoute: String = routes.CheckYourStateDetailsController.onPageLoad(NormalMode).url

  "CheckYourStateDetails Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, checkYourStateDetailsRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[CheckYourStateDetailsView]

        status(result) mustEqual OK
        contentAsString(result) must include(routes.CheckYourClaimDetailsController.onPageLoad().url)
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = UserAnswers(userAnswersId).set(CheckYourStateDetailsPage, true).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, checkYourStateDetailsRoute)

        val view = application.injector.instanceOf[CheckYourStateDetailsView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) must include(routes.CheckYourClaimDetailsController.onPageLoad().url)
      }
    }

    "must call delete and redirect to the management frontend when 'Yes' is submitted" in {

      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
      when(mockEuVatRefundsService.deleteApplication(any())(any())) thenReturn Future.successful(())

      val populatedAnswers = emptyUserAnswers
        .set(ClaimApplicationResponseQuery, ApplicationResponse(123, "APP123", 1))
        .success
        .value

      val application =
        applicationBuilder(userAnswers = Some(populatedAnswers))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, checkYourStateDetailsRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val appConfig = application.injector.instanceOf[FrontendAppConfig]

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual appConfig.claimDashboardUrl

        verify(mockEuVatRefundsService).deleteApplication(any())(any())
        val captor = ArgumentCaptor.forClass(classOf[UserAnswers])
        verify(mockSessionRepository).set(captor.capture())
        captor.getValue.data mustBe Json.obj()
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, checkYourStateDetailsRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[CheckYourStateDetailsView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) must include(routes.CheckYourClaimDetailsController.onPageLoad().url)
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, checkYourStateDetailsRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, checkYourStateDetailsRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
