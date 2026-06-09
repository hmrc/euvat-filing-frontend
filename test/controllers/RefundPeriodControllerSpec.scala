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
import org.mockito.Mockito.*
import org.scalatestplus.mockito.MockitoSugar
import pages.*
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*

class RefundPeriodControllerSpec extends SpecBase with MockitoSugar {

  val formProviderBeforeSept30 = new forms.RefundPeriodFormProvider() {
    override protected def today: java.time.LocalDate = java.time.LocalDate.of(2024, 6, 1)
  }

  val formProviderAfterSept30 = new forms.RefundPeriodFormProvider() {
    override protected def today: java.time.LocalDate = java.time.LocalDate.of(2024, 10, 1)
  }

  val onwardRoute = Call("GET", "/foo")

  "RefundPeriod Controller" - {

    ".onPageLoad" - {

      "must return OK and the correct view for a GET" in {
        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, routes.RefundPeriodController.onPageLoad(models.NormalMode).url)
          val result = route(application, request).value
          val view = application.injector.instanceOf[views.html.RefundPeriodView]
          implicit val msgs = messages(application)
          val form = application.injector.instanceOf[forms.RefundPeriodFormProvider].apply()

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(
            form,
            models.NormalMode,
            routes.RefundingLanguageController.onPageLoad(models.NormalMode),
            None,
            None,
            Set.empty[String],
            Map.empty[String, String]
          )(request, msgs).toString
        }
      }

      "must pre-fill the form when saved answers exist" in {
        val savedPeriod = models.RefundPeriod(
          java.time.YearMonth.of(2025, 3).atDay(1).atStartOfDay(),
          java.time.YearMonth.of(2025, 8).atEndOfMonth().atTime(23, 59, 59, 999000000)
        )
        val userAnswers = emptyUserAnswers.set(RefundPeriodPage, savedPeriod).success.value
        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, routes.RefundPeriodController.onPageLoad(models.NormalMode).url)
          val result = route(application, request).value
          val view = application.injector.instanceOf[views.html.RefundPeriodView]
          val formProvider = application.injector.instanceOf[forms.RefundPeriodFormProvider]
          implicit val msgs = messages(application)
          val start = java.time.YearMonth.of(savedPeriod.startDate.getYear, savedPeriod.startDate.getMonthValue)
          val end = java.time.YearMonth.of(savedPeriod.endDate.getYear, savedPeriod.endDate.getMonthValue)
          val form = formProvider().fill(forms.RefundPeriodData(start, end))

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(
            form,
            models.NormalMode,
            routes.RefundingLanguageController.onPageLoad(models.NormalMode),
            None,
            None,
            Set.empty[String],
            Map.empty[String, String]
          )(request, msgs).toString
        }
      }

      "must use RefundingCurrencyController as back link when country has two currencies" in {
        val userAnswers = emptyUserAnswers.set(pages.RefundingCountryPage, "BG").success.value
        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, routes.RefundPeriodController.onPageLoad(models.NormalMode).url)
          val result = route(application, request).value

          status(result) mustEqual OK
          contentAsString(result) must include(routes.RefundingCurrencyController.onPageLoad(models.NormalMode).url)
        }
      }

      "must use RefundingLanguageController as back link when country has one currency" in {
        val userAnswers = emptyUserAnswers.set(pages.RefundingCountryPage, "AT").success.value
        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, routes.RefundPeriodController.onPageLoad(models.NormalMode).url)
          val result = route(application, request).value

          status(result) mustEqual OK
          contentAsString(result) must include(routes.RefundingLanguageController.onPageLoad(models.NormalMode).url)
        }
      }
    }

    ".onSubmit" - {

      "must redirect to the next page when valid data is submitted" in {
        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(bind[navigation.Navigator].toInstance(new FakeNavigator(onwardRoute)))
          .overrides(bind[forms.RefundPeriodFormProvider].toInstance(formProviderAfterSept30))
          .build()

        running(application) {
          val request = FakeRequest(POST, routes.RefundPeriodController.onSubmit(models.NormalMode).url)
            .withFormUrlEncodedBody(
              "start.month" -> "03",
              "start.year"  -> "2024",
              "end.month"   -> "08",
              "end.year"    -> "2024"
            )
          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual onwardRoute.url
        }
      }

      "must redirect to journey recovery when no user answers exist" in {
        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val request = FakeRequest(GET, routes.RefundPeriodController.onPageLoad(models.NormalMode).url)
          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must return a Bad Request when both fields are empty" in {
        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

        running(application) {
          val request = FakeRequest(POST, routes.RefundPeriodController.onSubmit(models.NormalMode).url)
            .withFormUrlEncodedBody(
              "start.month" -> "",
              "start.year"  -> "",
              "end.month"   -> "",
              "end.year"    -> ""
            )
          val result = route(application, request).value

          status(result) mustEqual BAD_REQUEST
        }
      }

      "must show start-before-end error when start date is after end date" in {
        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(bind[forms.RefundPeriodFormProvider].toInstance(formProviderAfterSept30))
          .build()

        running(application) {
          val request = FakeRequest(POST, routes.RefundPeriodController.onSubmit(models.NormalMode).url)
            .withFormUrlEncodedBody(
              "start.month" -> "08",
              "start.year"  -> "2024",
              "end.month"   -> "03",
              "end.year"    -> "2024"
            )
          val result = route(application, request).value

          status(result) mustEqual BAD_REQUEST
          contentAsString(result) must include(messages(application)("refundPeriod.error.periodStartDatenotAfterEndDate"))
        }
      }

      "must show single-year error when start and end are in different years and start is after cutoff" in {
        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(bind[forms.RefundPeriodFormProvider].toInstance(formProviderAfterSept30))
          .build()

        running(application) {
          val request = FakeRequest(POST, routes.RefundPeriodController.onSubmit(models.NormalMode).url)
            .withFormUrlEncodedBody(
              "start.month" -> "03",
              "start.year"  -> "2024",
              "end.month"   -> "08",
              "end.year"    -> "2025"
            )
          val result = route(application, request).value

          status(result) mustEqual BAD_REQUEST
          contentAsString(result) must include(messages(application)("refundPeriod.error.periodEndDaterefundPeriodInSingleYear"))
        }
      }

      "must show September cutoff error when start and end are in different years and start is before cutoff" in {
        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(bind[forms.RefundPeriodFormProvider].toInstance(formProviderAfterSept30))
          .build()

        running(application) {
          val request = FakeRequest(POST, routes.RefundPeriodController.onSubmit(models.NormalMode).url)
            .withFormUrlEncodedBody(
              "start.month" -> "03",
              "start.year"  -> "2023",
              "end.month"   -> "08",
              "end.year"    -> "2024"
            )
          val result = route(application, request).value

          status(result) mustEqual BAD_REQUEST
          contentAsString(result) must include("Refund period start date must be on or after 1 January 2024")
        }
      }

      "must show minimum-length error when period is less than 3 months" in {
        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(bind[forms.RefundPeriodFormProvider].toInstance(formProviderAfterSept30))
          .build()

        running(application) {
          val request = FakeRequest(POST, routes.RefundPeriodController.onSubmit(models.NormalMode).url)
            .withFormUrlEncodedBody(
              "start.month" -> "03",
              "start.year"  -> "2024",
              "end.month"   -> "04",
              "end.year"    -> "2024"
            )
          val result = route(application, request).value

          status(result) mustEqual BAD_REQUEST
          contentAsString(result) must include(messages(application)("refundPeriod.error.periodStartDateperiodNotLessThan3Months"))
        }
      }

      "must show end-date-in-past error when end date is in the future" in {
        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

        running(application) {
          val future = java.time.YearMonth.now().plusMonths(1)
          val request = FakeRequest(POST, routes.RefundPeriodController.onSubmit(models.NormalMode).url)
            .withFormUrlEncodedBody(
              "start.month" -> future.minusMonths(4).getMonthValue.toString,
              "start.year"  -> future.minusMonths(4).getYear.toString,
              "end.month"   -> future.getMonthValue.toString,
              "end.year"    -> future.getYear.toString
            )
          val result = route(application, request).value

          status(result) mustEqual BAD_REQUEST
          contentAsString(result) must include(messages(application)("refundPeriod.error.periodEndDateInvalid"))
        }
      }

      "must allow a short period when it ends in December" in {
        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(bind[navigation.Navigator].toInstance(new FakeNavigator(onwardRoute)))
          .overrides(bind[forms.RefundPeriodFormProvider].toInstance(formProviderAfterSept30))
          .build()

        running(application) {
          val request = FakeRequest(POST, routes.RefundPeriodController.onSubmit(models.NormalMode).url)
            .withFormUrlEncodedBody(
              "start.month" -> "11",
              "start.year"  -> "2024",
              "end.month"   -> "12",
              "end.year"    -> "2024"
            )
          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual onwardRoute.url
        }
      }

      "must allow a period exactly 3 months long" in {
        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(bind[navigation.Navigator].toInstance(new FakeNavigator(onwardRoute)))
          .overrides(bind[forms.RefundPeriodFormProvider].toInstance(formProviderAfterSept30))
          .build()

        running(application) {
          val request = FakeRequest(POST, routes.RefundPeriodController.onSubmit(models.NormalMode).url)
            .withFormUrlEncodedBody(
              "start.month" -> "03",
              "start.year"  -> "2024",
              "end.month"   -> "05",
              "end.year"    -> "2024"
            )
          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual onwardRoute.url
        }
      }

      "must show minimum-length error when start and end are equal" in {
        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(bind[forms.RefundPeriodFormProvider].toInstance(formProviderAfterSept30))
          .build()

        running(application) {
          val request = FakeRequest(POST, routes.RefundPeriodController.onSubmit(models.NormalMode).url)
            .withFormUrlEncodedBody(
              "start.month" -> "03",
              "start.year"  -> "2024",
              "end.month"   -> "03",
              "end.year"    -> "2024"
            )
          val result = route(application, request).value

          status(result) mustEqual BAD_REQUEST
          contentAsString(result) must include(messages(application)("refundPeriod.error.periodStartDateperiodNotLessThan3Months"))
        }
      }

      "must show minimum-length error when start and end are the same month" in {
        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(bind[forms.RefundPeriodFormProvider].toInstance(formProviderAfterSept30))
          .build()

        running(application) {
          val request = FakeRequest(POST, routes.RefundPeriodController.onSubmit(models.NormalMode).url)
            .withFormUrlEncodedBody(
              "start.month" -> "04",
              "start.year"  -> "2024",
              "end.month"   -> "04",
              "end.year"    -> "2024"
            )
          val result = route(application, request).value

          status(result) mustEqual BAD_REQUEST
          contentAsString(result) must include(messages(application)("refundPeriod.error.periodStartDateperiodNotLessThan3Months"))
        }
      }

      "must show start-date-invalid error when start is in the future and end is in the past" in {
        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
        running(application) {
          val future = java.time.YearMonth.now().plusMonths(1)
          val past = java.time.YearMonth.now().minusMonths(3)
          val request = FakeRequest(POST, routes.RefundPeriodController.onSubmit(models.NormalMode).url)
            .withFormUrlEncodedBody(
              "start.month" -> future.getMonthValue.toString,
              "start.year"  -> future.getYear.toString,
              "end.month"   -> past.getMonthValue.toString,
              "end.year"    -> past.getYear.toString
            )
          val result = route(application, request).value
          status(result) mustEqual BAD_REQUEST
          contentAsString(result) must include(messages(application)("refundPeriod.error.periodStartDateInvalid"))
        }
      }

      "must show both-dates-invalid error when both dates are in the future" in {
        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
        running(application) {
          val future = java.time.YearMonth.now().plusMonths(1)
          val request = FakeRequest(POST, routes.RefundPeriodController.onSubmit(models.NormalMode).url)
            .withFormUrlEncodedBody(
              "start.month" -> future.getMonthValue.toString,
              "start.year"  -> future.getYear.toString,
              "end.month"   -> future.plusMonths(3).getMonthValue.toString,
              "end.year"    -> future.plusMonths(3).getYear.toString
            )
          val result = route(application, request).value
          status(result) mustEqual BAD_REQUEST
          contentAsString(result) must include(messages(application)("refundPeriod.error.periodBothDatesInvalid"))
        }
      }

      "must show invalid year error when year is greater than 9999" in {
        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
        running(application) {
          val request = FakeRequest(POST, routes.RefundPeriodController.onSubmit(models.NormalMode).url)
            .withFormUrlEncodedBody(
              "start.month" -> "03",
              "start.year"  -> "10000",
              "end.month"   -> "08",
              "end.year"    -> "2024"
            )
          val result = route(application, request).value
          status(result) mustEqual BAD_REQUEST
          contentAsString(result) must include(messages(application)("refundPeriod.error.periodStartDateinvalidStartEndDateFormat.year"))
        }
      }

      "September cutoff" - {

        "must reject start date before January of current year when today is after 30 September" in {
          val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
            .overrides(bind[forms.RefundPeriodFormProvider].toInstance(formProviderAfterSept30))
            .build()

          running(application) {
            val request = FakeRequest(POST, routes.RefundPeriodController.onSubmit(models.NormalMode).url)
              .withFormUrlEncodedBody(
                "start.month" -> "03",
                "start.year"  -> "2023",
                "end.month"   -> "08",
                "end.year"    -> "2023"
              )
            val result = route(application, request).value

            status(result) mustEqual BAD_REQUEST
            contentAsString(result) must include("Refund period start date must be on or after 1 January 2024")
          }
        }

        "must accept start date in January of current year when today is after 30 September" in {
          val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
            .overrides(
              bind[navigation.Navigator].toInstance(new FakeNavigator(onwardRoute)),
              bind[forms.RefundPeriodFormProvider].toInstance(formProviderAfterSept30)
            )
            .build()

          running(application) {
            val request = FakeRequest(POST, routes.RefundPeriodController.onSubmit(models.NormalMode).url)
              .withFormUrlEncodedBody(
                "start.month" -> "01",
                "start.year"  -> "2024",
                "end.month"   -> "06",
                "end.year"    -> "2024"
              )
            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual onwardRoute.url
          }
        }

        "must reject start date before January of previous year when today is on or before 30 September" in {
          val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
            .overrides(bind[forms.RefundPeriodFormProvider].toInstance(formProviderBeforeSept30))
            .build()

          running(application) {
            val request = FakeRequest(POST, routes.RefundPeriodController.onSubmit(models.NormalMode).url)
              .withFormUrlEncodedBody(
                "start.month" -> "03",
                "start.year"  -> "2022",
                "end.month"   -> "08",
                "end.year"    -> "2022"
              )
            val result = route(application, request).value

            status(result) mustEqual BAD_REQUEST
            contentAsString(result) must include("Refund period start date must be on or after 1 January 2023")
          }
        }

        "must accept start date in January of previous year when today is on or before 30 September" in {
          val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
            .overrides(
              bind[navigation.Navigator].toInstance(new FakeNavigator(onwardRoute)),
              bind[forms.RefundPeriodFormProvider].toInstance(formProviderBeforeSept30)
            )
            .build()

          running(application) {
            val request = FakeRequest(POST, routes.RefundPeriodController.onSubmit(models.NormalMode).url)
              .withFormUrlEncodedBody(
                "start.month" -> "01",
                "start.year"  -> "2023",
                "end.month"   -> "06",
                "end.year"    -> "2023"
              )
            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual onwardRoute.url
          }
        }
      }
    }
  }
}
