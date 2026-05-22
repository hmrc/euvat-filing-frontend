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
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{times, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import navigation.FakeNavigator
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import play.api.inject.bind
import utils.BusinessActivityList
import pages.BusinessActivityCodeTwoPage
import play.api.mvc.Call

class BusinessActivityCodeTwoControllerSpec extends SpecBase with MockitoSugar {

  val onwardRoute: Call = Call("GET", "/foo")

  "BusinessActivityCodeTwo Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, routes.BusinessActivityCodeTwoController.onPageLoad(models.NormalMode).url)
        val result = route(application, request).value

        val view = application.injector.instanceOf[views.html.BusinessActivityCodeTwoView]
        val formProvider = application.injector.instanceOf[forms.BusinessActivityCodeTwoFormProvider]
        val activities: Seq[(String, String)] = BusinessActivityList.fromConfig(application.configuration)
        val allowed: Set[String] = activities.flatMap { case (n, c) => Seq(c, s"$c ($n)") }.toSet
        val form = formProvider(allowed)

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form,
                                               activities,
                                               Some(routes.BusinessActivityController.onPageLoad(models.NormalMode).url),
                                               models.NormalMode
                                              )(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery when no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, routes.BusinessActivityCodeTwoController.onPageLoad(models.NormalMode).url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to next page when valid data is submitted" in {

      val mockSessionRepository = mock[repositories.SessionRepository]
      when(mockSessionRepository.set(any())) thenReturn scala.concurrent.Future.successful(true)

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[navigation.Navigator].toInstance(new FakeNavigator(onwardRoute)),
          bind[repositories.SessionRepository].toInstance(mockSessionRepository)
        )
        .build()

      running(application) {
        val request = FakeRequest(POST, routes.BusinessActivityCodeTwoController.onSubmit(models.NormalMode).url)
          .withFormUrlEncodedBody(("value", "25344"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url

        verify(mockSessionRepository, times(1)).set(any())
      }
    }

    "must pre-fill the form when a saved value exists" in {

      val userAnswers = emptyUserAnswers.set(BusinessActivityCodeTwoPage, "25344").success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, routes.BusinessActivityCodeTwoController.onPageLoad(models.NormalMode).url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[views.html.BusinessActivityCodeTwoView]
        val formProvider = application.injector.instanceOf[forms.BusinessActivityCodeTwoFormProvider]
        val activities: Seq[(String, String)] = BusinessActivityList.fromConfig(application.configuration)
        val allowed: Set[String] = activities.flatMap { case (n, c) => Seq(c, s"$c ($n)") }.toSet
        val form = formProvider(allowed).fill("25344")

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form,
                                               activities,
                                               Some(routes.BusinessActivityController.onPageLoad(models.NormalMode).url),
                                               models.NormalMode
                                              )(request, messages(application)).toString
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(POST, routes.BusinessActivityCodeTwoController.onSubmit(models.NormalMode).url)
          .withFormUrlEncodedBody(("value", ""))

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST

        val body = contentAsString(result)
        body must include(messages(application)("businessActivityCodeTwo.error.required"))
        body must include(messages(application)("businessActivityCodeTwo.error.summary"))

        val typedRequest = FakeRequest(POST, routes.BusinessActivityCodeTwoController.onSubmit(models.NormalMode).url)
          .withFormUrlEncodedBody(("value", ""), ("valueTyped", "NotACode"))

        val typedResult = route(application, typedRequest).value
        status(typedResult) mustEqual BAD_REQUEST
        val typedBody = contentAsString(typedResult)
        typedBody must include(messages(application)("businessActivityCodeTwo.error.invalid"))
        typedBody must include(messages(application)("businessActivityCodeTwo.error.invalid.summary"))

        val rawInvalidRequest = FakeRequest(POST, routes.BusinessActivityCodeTwoController.onSubmit(models.NormalMode).url)
          .withFormUrlEncodedBody(("value", "99999"))

        val rawInvalidResult = route(application, rawInvalidRequest).value
        status(rawInvalidResult) mustEqual BAD_REQUEST
        val rawInvalidBody = contentAsString(rawInvalidResult)
        rawInvalidBody must include(messages(application)("businessActivityCodeTwo.error.invalid"))
        rawInvalidBody must include(messages(application)("businessActivityCodeTwo.error.invalid.summary"))
      }
    }
  }
}
