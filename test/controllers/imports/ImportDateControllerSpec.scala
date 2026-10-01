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
import forms.imports.ImportDateFormProvider
import models.*
import navigation.FakeNavigator
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.{ImportDatePage, SadReferencePage}
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import views.html.imports.ImportDateView

import java.time.LocalDate
import scala.concurrent.Future

class ImportDateControllerSpec extends SpecBase with MockitoSugar {

  val onwardRoute: Call = Call("GET", "/foo")

  "ImportDate Controller" - {

    ".onPageLoad" - {
      "must return OK and the correct view for a GET in NormalMode when SAD reference is No" in {
        val userAnswers = emptyUserAnswers.set(SadReferencePage, false).success.value

        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, routes.ImportDateController.onPageLoad(models.NormalMode).url)
          val result = route(application, request).value
          val view = application.injector.instanceOf[ImportDateView]
          implicit val msgs: Messages = messages(application)

          status(result) mustEqual OK
          normalizeHtml(contentAsString(result)) mustEqual normalizeHtml(
            view(
              application.injector.instanceOf[ImportDateFormProvider].apply(),
              models.NormalMode,
              controllers.imports.routes.ImportDetailsInfoController.onPageLoad(models.NormalMode)
            )(request, msgs).toString
          )
        }
      }

      "must return OK and the correct view for a GET in NormalMode when SAD reference is Yes" in {
        val userAnswers = emptyUserAnswers.set(SadReferencePage, true).success.value
        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, routes.ImportDateController.onPageLoad(models.NormalMode).url)
          val result = route(application, request).value
          val view = application.injector.instanceOf[ImportDateView]
          implicit val msgs: Messages = messages(application)

          status(result) mustEqual OK
          normalizeHtml(contentAsString(result)) mustEqual normalizeHtml(
            view(
              application.injector.instanceOf[ImportDateFormProvider].apply(),
              models.NormalMode,
              controllers.routes.JourneyRecoveryController.onPageLoad() // TODO: replace with SadNumberController once built
            )(request, msgs).toString
          )
        }
      }

      "must return OK and the correct view for a GET in CheckMode" in {
        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, routes.ImportDateController.onPageLoad(models.CheckMode).url)
          val result = route(application, request).value
          val view = application.injector.instanceOf[ImportDateView]
          implicit val msgs: Messages = messages(application)

          status(result) mustEqual OK
          normalizeHtml(contentAsString(result)) mustEqual normalizeHtml(
            view(
              application.injector.instanceOf[ImportDateFormProvider].apply(),
              models.CheckMode,
              controllers.routes.JourneyRecoveryController.onPageLoad() // TODO: replace with CheckYourImportController.onPageLoad() once built
            )(request, msgs).toString
          )
        }
      }

      "must populate the view correctly on a GET when the question has previously been answered " in {
        val userAnswers = emptyUserAnswers
          .set(SadReferencePage, false)
          .success
          .value
          .set(pages.ImportDatePage, LocalDate.of(2025, 3, 14))
          .success
          .value

        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, routes.ImportDateController.onPageLoad(models.NormalMode).url)
          val result = route(application, request).value
          val view = application.injector.instanceOf[ImportDateView]
          implicit val msgs: Messages = messages(application)

          status(result) mustEqual OK
          normalizeHtml(contentAsString(result)) mustEqual normalizeHtml(
            view(
              application.injector.instanceOf[ImportDateFormProvider].apply().fill(LocalDate.of(2025, 3, 14)),
              models.NormalMode,
              controllers.imports.routes.ImportDetailsInfoController.onPageLoad(models.NormalMode)
            )(request, msgs).toString
          )
        }
      }

      "must redirect to Journey Recovery when no user answers exist" in {
        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val request = FakeRequest(GET, routes.ImportDateController.onPageLoad(models.NormalMode).url)
          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }

    ".onSubmit" - {

      "must redirect to the next page when valid past date is submitted" in {
        val mockSessionRepository = mock[repositories.SessionRepository]
        when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[navigation.Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[repositories.SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

        running(application) {
          val request = FakeRequest(POST, routes.ImportDateController.onSubmit(models.NormalMode).url)
            .withFormUrlEncodedBody(
              "value.day"   -> "15",
              "value.month" -> "04",
              "value.year"  -> "2025"
            )
          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual onwardRoute.url
          verify(mockSessionRepository).set(any())
        }
      }

      "must return Bad Request when date is in the future" in {
        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

        running(application) {
          val future = LocalDate.now().plusDays(1)
          val request = FakeRequest(POST, routes.ImportDateController.onSubmit(models.NormalMode).url)
            .withFormUrlEncodedBody(
              "value.day"   -> f"${future.getDayOfMonth}%02d",
              "value.month" -> f"${future.getMonthValue}%02d",
              "value.year"  -> future.getYear.toString
            )
          val result = route(application, request).value

          status(result) mustEqual BAD_REQUEST
          val body = contentAsString(result)
          body must include(messages(application)("importDate.error.past"))
          body must include("href=\"#value.day\"")
        }
      }

      "must return Bad Request and link to month when month is missing" in {
        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

        running(application) {
          val request = FakeRequest(POST, routes.ImportDateController.onSubmit(models.NormalMode).url)
            .withFormUrlEncodedBody(
              "value.day"   -> "15",
              "value.month" -> "",
              "value.year"  -> "2025"
            )
          val result = route(application, request).value

          status(result) mustEqual BAD_REQUEST
          val body = contentAsString(result)
          val expected = messages(application)("importDate.error.required", messages(application)("date.error.month"))
          body must include(expected)
          body must include("href=\"#value.month\"")
          body must include("value=\"15\"")
          body must include("value=\"2025\"")
        }
      }

      "must return Bad Request and link to year when year is missing" in {
        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

        running(application) {
          val request = FakeRequest(POST, routes.ImportDateController.onSubmit(models.NormalMode).url)
            .withFormUrlEncodedBody(
              "value.day"   -> "15",
              "value.month" -> "04",
              "value.year"  -> ""
            )
          val result = route(application, request).value

          status(result) mustEqual BAD_REQUEST
          val body = contentAsString(result)
          val expected = messages(application)("importDate.error.required", messages(application)("date.error.year"))
          body must include(expected)
          body must include("href=\"#value.year\"")
          body must include("value=\"15\"")
          body must include("value=\"04\"")
        }
      }

      "must return Bad Request and link to day when day and month are missing" in {
        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

        running(application) {
          val request = FakeRequest(POST, routes.ImportDateController.onSubmit(models.NormalMode).url)
            .withFormUrlEncodedBody(
              "value.day"   -> "",
              "value.month" -> "",
              "value.year"  -> "2025"
            )
          val result = route(application, request).value

          status(result) mustEqual BAD_REQUEST
          val body = contentAsString(result)
          val expected =
            messages(application)("importDate.error.required.two", messages(application)("date.error.day"), messages(application)("date.error.month"))
          body must include(expected)
          body must include("href=\"#value.day\"")
          body must include("value=\"2025\"")
        }
      }

      "must return Bad Request and link to day when all fields are missing" in {
        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

        running(application) {
          val request = FakeRequest(POST, routes.ImportDateController.onSubmit(models.NormalMode).url)
            .withFormUrlEncodedBody(
              "value.day"   -> "",
              "value.month" -> "",
              "value.year"  -> ""
            )
          val result = route(application, request).value

          status(result) mustEqual BAD_REQUEST
          val body = contentAsString(result)
          val expected = messages(application)("importDate.error.required.all")
          body must include(expected)
          body must include("href=\"#value.day\"")
        }
      }

      "must return Bad Request and link to day when day is invalid and month/year are valid" in {
        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

        running(application) {
          val request = FakeRequest(POST, routes.ImportDateController.onSubmit(models.NormalMode).url)
            .withFormUrlEncodedBody(
              "value.day"   -> "39",
              "value.month" -> "04",
              "value.year"  -> "2025"
            )
          val result = route(application, request).value

          status(result) mustEqual BAD_REQUEST
          val body = contentAsString(result)
          val expected =
            messages(application)("importDate.error.invalid.day")
          body must include(expected)
          body must include("href=\"#value.day\"")
        }
      }

      "must return Bad Request and link to month when month is invalid and day/year are valid" in {
        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

        running(application) {
          val request = FakeRequest(POST, routes.ImportDateController.onSubmit(models.NormalMode).url)
            .withFormUrlEncodedBody(
              "value.day"   -> "14",
              "value.month" -> "45",
              "value.year"  -> "2025"
            )
          val result = route(application, request).value

          status(result) mustEqual BAD_REQUEST
          val body = contentAsString(result)
          val expected =
            messages(application)("importDate.error.invalid.month")
          body must include(expected)
          body must include("href=\"#value.month\"")
        }
      }

      "must return Bad Request and link to year when year is invalid and day/month are valid" in {
        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

        running(application) {
          val request = FakeRequest(POST, routes.ImportDateController.onSubmit(models.NormalMode).url)
            .withFormUrlEncodedBody(
              "value.day"   -> "14",
              "value.month" -> "04",
              "value.year"  -> "dhg"
            )
          val result = route(application, request).value

          status(result) mustEqual BAD_REQUEST
          val body = contentAsString(result)
          val expected = messages(application)("importDate.error.invalid.year")
          body must include(expected)
          body must include("href=\"#value.year\"")
        }
      }

      "must return Bad Request and link to day and month when day and month are invalid" in {
        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

        running(application) {
          val request = FakeRequest(POST, routes.ImportDateController.onSubmit(models.NormalMode).url)
            .withFormUrlEncodedBody(
              "value.day"   -> "45",
              "value.month" -> "67",
              "value.year"  -> "2025"
            )
          val result = route(application, request).value

          status(result) mustEqual BAD_REQUEST
          val body = contentAsString(result)
          val expected =
            messages(application)("importDate.error.invalid.two", messages(application)("date.error.day"), messages(application)("date.error.month"))
          body must include(expected)
          body must include("href=\"#value.day\"")
        }
      }

      "must return Bad Request and link to day when day, month and year are all invalid" in {
        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

        running(application) {
          val request = FakeRequest(POST, routes.ImportDateController.onSubmit(models.NormalMode).url)
            .withFormUrlEncodedBody(
              "value.day"   -> "45",
              "value.month" -> "67",
              "value.year"  -> "dhg"
            )
          val result = route(application, request).value

          status(result) mustEqual BAD_REQUEST
          val body = contentAsString(result)
          val expected = messages(application)("importDate.error.invalid")
          body must include(expected)
          body must include("href=\"#value.day\"")
        }
      }

      "must accept a month entered as a short name" in {
        val mockSessionRepository = mock[repositories.SessionRepository]
        when(mockSessionRepository.set(any())) thenReturn scala.concurrent.Future.successful(true)

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[navigation.Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[repositories.SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

        running(application) {
          val request = FakeRequest(POST, routes.ImportDateController.onSubmit(models.NormalMode).url)
            .withFormUrlEncodedBody(
              "value.day"   -> "16",
              "value.month" -> "Feb",
              "value.year"  -> "2025"
            )
          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual onwardRoute.url
        }
      }

      "must persist and redirect to Journey Recovery when in CheckMode and import date unchanged" in {
        val userAnswers = emptyUserAnswers.set(pages.ImportDatePage, LocalDate.of(2025, 3, 14)).success.value

        val mockSessionRepository = mock[repositories.SessionRepository]
        when(mockSessionRepository.set(any())) thenReturn scala.concurrent.Future.successful(true)

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(
            bind[repositories.SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

        running(application) {
          val request = FakeRequest(POST, routes.ImportDateController.onSubmit(models.CheckMode).url)
            .withFormUrlEncodedBody(
              "value.day"   -> "14",
              "value.month" -> "03",
              "value.year"  -> "2025"
            )

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
          org.mockito.Mockito.verify(mockSessionRepository, org.mockito.Mockito.never()).set(any())
        }
      }

      "must persist and redirect to Journey Recovery when in CheckMode and import date changed" in {
        val userAnswers = emptyUserAnswers.set(ImportDatePage, LocalDate.of(2025, 3, 14)).success.value

        val mockSessionRepository = mock[repositories.SessionRepository]
        when(mockSessionRepository.set(any())) thenReturn scala.concurrent.Future.successful(true)

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(
            bind[repositories.SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

        running(application) {
          val request = FakeRequest(POST, routes.ImportDateController.onSubmit(models.CheckMode).url)
            .withFormUrlEncodedBody(
              "value.day"   -> "16",
              "value.month" -> "03",
              "value.year"  -> "2025"
            )

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController
            .onPageLoad()
            .url // TODO: replace with CheckYourImportController once built

          val captor = org.mockito.ArgumentCaptor.forClass(classOf[models.UserAnswers])
          org.mockito.Mockito.verify(mockSessionRepository).set(captor.capture())
          val saved = captor.getValue
          saved.get(pages.ImportDatePage) mustBe Some(LocalDate.of(2025, 3, 16))
        }
      }
    }
  }
}
