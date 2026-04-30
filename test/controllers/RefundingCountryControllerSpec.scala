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
import utils.CountryList
import pages.RefundingCountryPage
import play.api.mvc.Call

class RefundingCountryControllerSpec extends SpecBase with MockitoSugar {

  val onwardRoute = Call("GET", "/foo")

  "RefundingCountry Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, routes.RefundingCountryController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[views.html.RefundingCountryView]
        val formProvider = application.injector.instanceOf[forms.RefundingCountryFormProvider]
        val countries: Seq[(String, String)] = CountryList.fromConfig(application.configuration)
        val allowed: Set[String] = countries.flatMap { case (n, c) => Seq(n, c) }.toSet
        val form = formProvider(allowed)

        status(result) mustEqual OK
        val body = contentAsString(result)
        val backUrl = application.configuration.get[String]("urls.loginContinue") + controllers.routes.TaskListDashboardController.onPageLoad().url
        body must include(s"href=\"$backUrl\"")
        body mustEqual view(form, countries, Some(backUrl))(request, messages(application)).toString
      }
    }

    "must redirect to JourneyRecovery when no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, routes.RefundingCountryController.onPageLoad().url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must return OK and an empty form when arriving from the task list (Referer)" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, routes.RefundingCountryController.onPageLoad().url)
          .withHeaders("Referer" -> controllers.routes.TaskListDashboardController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[views.html.RefundingCountryView]
        val formProvider = application.injector.instanceOf[forms.RefundingCountryFormProvider]
        val countries: Seq[(String, String)] = CountryList.fromConfig(application.configuration)
        val allowed: Set[String] = countries.flatMap { case (n, c) => Seq(n, c) }.toSet
        val form = formProvider(allowed)

        status(result) mustEqual OK
        val body = contentAsString(result)
        val backUrl = application.configuration.get[String]("urls.loginContinue") + controllers.routes.TaskListDashboardController.onPageLoad().url
        body must include(s"href=\"$backUrl\"")
        body mustEqual view(form, countries, Some(backUrl), cameFromTaskList = true)(request, messages(application)).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {

      val mockSessionRepository = mock[repositories.SessionRepository]
      when(mockSessionRepository.set(any())) thenReturn scala.concurrent.Future.successful(true)

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[navigation.Navigator].toInstance(new FakeNavigator(onwardRoute)),
          bind[repositories.SessionRepository].toInstance(mockSessionRepository)
        )
        .build()

      running(application) {
        val request = FakeRequest(POST, routes.RefundingCountryController.onSubmit().url)
          .withFormUrlEncodedBody(("value", "DE"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url

        verify(mockSessionRepository, times(1)).set(any())
      }
    }

    "must pre-fill the form when arriving from the task list and a saved value exists" in {

      val userAnswers = emptyUserAnswers.set(RefundingCountryPage, "DE").success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, routes.RefundingCountryController.onPageLoad().url)
          .withHeaders("Referer" -> controllers.routes.TaskListDashboardController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[views.html.RefundingCountryView]
        val formProvider = application.injector.instanceOf[forms.RefundingCountryFormProvider]
        val countries: Seq[(String, String)] = CountryList.fromConfig(application.configuration)
        val allowed: Set[String] = countries.flatMap { case (n, c) => Seq(n, c) }.toSet
        val form = formProvider(allowed).fill("DE")

        status(result) mustEqual OK
        val body = contentAsString(result)
        val backUrl = application.configuration.get[String]("urls.loginContinue") + controllers.routes.TaskListDashboardController.onPageLoad().url
        body must include(s"href=\"$backUrl\"")
        body mustEqual view(form, countries, Some(backUrl), cameFromTaskList = true)(request, messages(application)).toString
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(POST, routes.RefundingCountryController.onSubmit().url)
          .withFormUrlEncodedBody(("value", ""))

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST

        val body = contentAsString(result)
        body must include(messages(application)("refundingCountry.error.required"))
        body must include(messages(application)("refundingCountry.error.summary"))

        // typed-but-unmatched input should show invalid message
        val typedRequest = FakeRequest(POST, routes.RefundingCountryController.onSubmit().url)
          .withFormUrlEncodedBody(("value", ""), ("valueTyped", "NotACountry"))

        val typedResult = route(application, typedRequest).value
        status(typedResult) mustEqual BAD_REQUEST
        val typedBody = contentAsString(typedResult)
        typedBody must include(messages(application)("refundingCountry.error.invalid"))
        typedBody must include(messages(application)("refundingCountry.error.invalid.summary"))

        // non-existent code should also show invalid message
        val rawInvalidRequest = FakeRequest(POST, routes.RefundingCountryController.onSubmit().url)
          .withFormUrlEncodedBody(("value", "ZZ"))

        val rawInvalidResult = route(application, rawInvalidRequest).value
        status(rawInvalidResult) mustEqual BAD_REQUEST
        val rawInvalidBody = contentAsString(rawInvalidResult)
        rawInvalidBody must include(messages(application)("refundingCountry.error.invalid"))
        rawInvalidBody must include(messages(application)("refundingCountry.error.invalid.summary"))
      }
    }
  }
}
