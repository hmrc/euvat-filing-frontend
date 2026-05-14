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

package controllers

import base.SpecBase
import forms.BusinessActivityFormProvider
import models.{BusinessActivity, NormalMode}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{times, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.BusinessActivityPage
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import views.html.BusinessActivityView

import scala.concurrent.Future

class BusinessActivityControllerSpec extends SpecBase with MockitoSugar {

  private val onwardRoute = Call("GET", "/foo")

  private lazy val pageLoadRoute = routes.BusinessActivityController.onPageLoad(NormalMode).url
  private lazy val submitRoute = routes.BusinessActivityController.onSubmit(NormalMode).url
  private lazy val backLink: Call = routes.RefundingCountryController.onPageLoad(NormalMode)

  "BusinessActivity Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, pageLoadRoute)
        val result = route(application, request).value

        val view = application.injector.instanceOf[BusinessActivityView]
        val formProvider = application.injector.instanceOf[BusinessActivityFormProvider]
        val form = formProvider()

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode, backLink)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery when no existing data is found on GET" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, pageLoadRoute)
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must populate the view with the saved value on GET" in {

      val userAnswers = emptyUserAnswers.set(BusinessActivityPage, BusinessActivity.Yes).success.value
      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, pageLoadRoute)
        val result = route(application, request).value

        val view = application.injector.instanceOf[BusinessActivityView]
        val formProvider = application.injector.instanceOf[BusinessActivityFormProvider]
        val form = formProvider().fill(BusinessActivity.Yes)

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode, backLink)(request, messages(application)).toString
      }
    }

    "must redirect to the next page and persist the answer when valid data is submitted (Yes)" in {

      val mockSessionRepository = mock[SessionRepository]
      when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
          bind[SessionRepository].toInstance(mockSessionRepository)
        )
        .build()

      running(application) {
        val request = FakeRequest(POST, submitRoute)
          .withFormUrlEncodedBody("value" -> BusinessActivity.Yes.toString)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
        verify(mockSessionRepository, times(1)).set(any())
      }
    }

    "must redirect to the next page and persist the answer when valid data is submitted (No)" in {

      val mockSessionRepository = mock[SessionRepository]
      when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
          bind[SessionRepository].toInstance(mockSessionRepository)
        )
        .build()

      running(application) {
        val request = FakeRequest(POST, submitRoute)
          .withFormUrlEncodedBody("value" -> BusinessActivity.No.toString)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
        verify(mockSessionRepository, times(1)).set(any())
      }
    }

    "must return Bad Request and errors when no value is submitted" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(POST, submitRoute).withFormUrlEncodedBody("value" -> "")

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) must include(messages(application)("businessActivity.error.required"))
      }
    }

    "must return Bad Request and errors when an invalid value is submitted" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(POST, submitRoute).withFormUrlEncodedBody("value" -> "maybe")

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) must include(messages(application)("businessActivity.error.required"))
      }
    }

    "must redirect to Journey Recovery when no existing data is found on POST" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(POST, submitRoute)
          .withFormUrlEncodedBody("value" -> BusinessActivity.Yes.toString)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
