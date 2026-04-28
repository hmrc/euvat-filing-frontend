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
import navigation.FakeNavigator
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.inject.bind
import play.api.mvc.Call

class RefundingCountryControllerSpec extends SpecBase {

  val onwardRoute = Call("GET", "/foo")

  "RefundingCountry Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, routes.RefundingCountryController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[views.html.RefundingCountryView]
        val formProvider = application.injector.instanceOf[forms.RefundingCountryFormProvider]
        val form = formProvider()
        val countries: Seq[(String, String)] = application.configuration.getOptional[Seq[String]]("eu.member-states").getOrElse(Seq.empty).map { s =>
          s.split("\\|") match {
            case Array(name, code) => (name.trim, code.trim)
            case Array(name)       => (name.trim, "")
            case _                 => (s, "")
          }
        }

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, countries)(request, messages(application)).toString
      }
    }

    "must return OK and the correct view for a GET when no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, routes.RefundingCountryController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[views.html.RefundingCountryView]
        val formProvider = application.injector.instanceOf[forms.RefundingCountryFormProvider]
        val form = formProvider()
        val countries: Seq[(String, String)] = application.configuration.getOptional[Seq[String]]("eu.member-states").getOrElse(Seq.empty).map { s =>
          s.split("\\|") match {
            case Array(name, code) => (name.trim, code.trim)
            case Array(name)       => (name.trim, "")
            case _                 => (s, "")
          }
        }

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, countries)(request, messages(application)).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[navigation.Navigator].toInstance(new FakeNavigator(onwardRoute)))
        .build()

      running(application) {
        val request = FakeRequest(POST, routes.RefundingCountryController.onSubmit().url)
          .withFormUrlEncodedBody(("value", "DE"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
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
