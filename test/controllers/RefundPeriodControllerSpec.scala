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

class RefundPeriodControllerSpec extends SpecBase {

  val onwardRoute = Call("GET", "/foo")

  "RefundPeriod Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, routes.RefundPeriodController.onPageLoad().url)

        val result = route(application, request).value

        status(result) mustEqual OK

        val body = contentAsString(result)

        // the rules should be rendered as a bullet list with govuk classes
        body must include ("govuk-list govuk-list--bullet")

        // date inputs should use govuk date input markup and width classes
        body must include ("govuk-date-input")
        body must include ("govuk-input--width-2")
        body must include ("govuk-input--width-4")
      }
    }

    "must redirect to the next page when valid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[navigation.Navigator].toInstance(new FakeNavigator(onwardRoute)))
        .build()

      running(application) {
        val request = FakeRequest(POST, routes.RefundPeriodController.onSubmit().url)
          .withFormUrlEncodedBody(
            "start.month" -> "03",
            "start.year" -> "2010",
            "end.month" -> "08",
            "end.year" -> "2010"
          )

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(POST, routes.RefundPeriodController.onSubmit().url)
          .withFormUrlEncodedBody(
            "start.month" -> "",
            "start.year" -> "",
            "end.month" -> "",
            "end.year" -> ""
          )

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
      }
    }

    "must show start-before-end validation and link to start field" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(POST, routes.RefundPeriodController.onSubmit().url)
          .withFormUrlEncodedBody(
            "start.month" -> "05",
            "start.year" -> "2011",
            "end.month" -> "03",
            "end.year" -> "2011"
          )

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST

        val body = contentAsString(result)

        body must include (messages(application)("refundPeriod.error.periodStartDatenotAfterEndDate"))
        body must include ("href=\"#\"")
      }
    }

    "must show single-year validation and link to end field" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(POST, routes.RefundPeriodController.onSubmit().url)
          .withFormUrlEncodedBody(
            "start.month" -> "03",
            "start.year" -> "2010",
            "end.month" -> "08",
            "end.year" -> "2011"
          )

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST

        val body = contentAsString(result)

        body must include (messages(application)("refundPeriod.error.periodEndDaterefundPeriodInSingleYear"))
        body must include ("href=\"#\"")
      }
    }

    "must show minimum-length validation when period < 3 months" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(POST, routes.RefundPeriodController.onSubmit().url)
          .withFormUrlEncodedBody(
            "start.month" -> "03",
            "start.year" -> "2010",
            "end.month" -> "05",
            "end.year" -> "2010"
          )

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST

        val body = contentAsString(result)

        body must include (messages(application)("refundPeriod.error.periodStartDateperiodNotLessThan3Months"))
      }
    }

    "must show end-date-in-past validation when end is in the future" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val future = java.time.YearMonth.now().plusMonths(1)
        val request = FakeRequest(POST, routes.RefundPeriodController.onSubmit().url)
          .withFormUrlEncodedBody(
            "start.month" -> "03",
            "start.year" -> "2010",
            "end.month" -> future.getMonthValue.toString,
            "end.year" -> future.getYear.toString
          )

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST

        val body = contentAsString(result)

        body must include (messages(application)("refundPeriod.error.periodEndDateInvalid"))
      }
    }

    "must allow short period when it ends in December (December exception)" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[navigation.Navigator].toInstance(new FakeNavigator(onwardRoute)))
        .build()

      running(application) {
        val request = FakeRequest(POST, routes.RefundPeriodController.onSubmit().url)
          .withFormUrlEncodedBody(
            "start.month" -> "11",
            "start.year" -> "2010",
            "end.month" -> "12",
            "end.year" -> "2010"
          )

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must allow a period exactly 3 months long" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[navigation.Navigator].toInstance(new FakeNavigator(onwardRoute)))
        .build()

      running(application) {
        val request = FakeRequest(POST, routes.RefundPeriodController.onSubmit().url)
          .withFormUrlEncodedBody(
            "start.month" -> "03",
            "start.year" -> "2010",
            "end.month" -> "06",
            "end.year" -> "2010"
          )

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must reject equal start and end month/year" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(POST, routes.RefundPeriodController.onSubmit().url)
          .withFormUrlEncodedBody(
            "start.month" -> "03",
            "start.year" -> "2010",
            "end.month" -> "03",
            "end.year" -> "2010"
          )

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST

        val body = contentAsString(result)

        body must include (messages(application)("refundPeriod.error.periodStartDatenotAfterEndDate"))
      }
    }
  }
}
