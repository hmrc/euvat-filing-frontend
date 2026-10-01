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
import forms.imports.TotalAmountWithoutVatFormProvider
import models.UserAnswers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.{RefundingCountryPage, RefundingCurrencyPage, SadReferenceNumberPage, SadReferencePage, TotalAmountWithoutVatPage}
import play.api.data.Form
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import views.html.imports.TotalAmountWithoutVatView

import scala.concurrent.Future

class TotalAmountWithoutVatControllerSpec extends SpecBase with MockitoSugar {

  val formProvider = new TotalAmountWithoutVatFormProvider()
  val form: Form[BigDecimal] = formProvider()
  lazy val totalAmountWithoutVatRoute: String = controllers.imports.routes.TotalAmountWithoutVatController.onPageLoad(models.NormalMode).url
  lazy val backLink = controllers.imports.routes.ImportDetailsInfoController.onPageLoad(models.NormalMode)

  "TotalAmountWithoutVat Controller" - {

    "must return OK and the correct view for a GET" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, totalAmountWithoutVatRoute)
        val result = route(application, request).value
        val view = application.injector.instanceOf[TotalAmountWithoutVatView]

        status(result) mustEqual OK
        normalizeHtml(contentAsString(result)) mustEqual normalizeHtml(
          view(form, models.NormalMode, backLink, "€", "Euro")(request, messages(application)).toString
        )
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {
      val userAnswers = UserAnswers(userAnswersId).set(TotalAmountWithoutVatPage, BigDecimal("12.34")).success.value
      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, totalAmountWithoutVatRoute)
        val view = application.injector.instanceOf[TotalAmountWithoutVatView]
        val result = route(application, request).value

        status(result) mustEqual OK
        normalizeHtml(contentAsString(result)) mustEqual normalizeHtml(
          view(form.fill(BigDecimal("12.34")), models.NormalMode, backLink, "€", "Euro")(request, messages(application)).toString
        )
      }
    }

    "must show selected currency symbol for a multi-currency country" in {
      val userAnswers = UserAnswers(userAnswersId)
        .set(RefundingCountryPage, "BG")
        .success
        .value
        .set(RefundingCurrencyPage, "BGN")
        .success
        .value
      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, totalAmountWithoutVatRoute)
        val view = application.injector.instanceOf[TotalAmountWithoutVatView]
        val result = route(application, request).value

        status(result) mustEqual OK
        normalizeHtml(contentAsString(result)) mustEqual normalizeHtml(
          view(form, models.NormalMode, backLink, "лв", "Bulgarian Lev")(request, messages(application)).toString
        )
      }
    }

    "must show back link to ImportDetailsInfo when SadReference was yes and number present" in {
      val userAnswers = UserAnswers(userAnswersId)
        .set(SadReferencePage, true)
        .success
        .value
        .set(SadReferenceNumberPage, "ABC123")
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, totalAmountWithoutVatRoute)
        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) must include(controllers.imports.routes.ImportDetailsInfoController.onPageLoad(models.NormalMode).url)
      }
    }

    "must show back link to ImportDetailsInfo when SadReference was yes but number missing" in {
      val userAnswers = UserAnswers(userAnswersId).set(SadReferencePage, true).success.value
      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, totalAmountWithoutVatRoute)
        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) must include(controllers.imports.routes.ImportDetailsInfoController.onPageLoad(models.NormalMode).url)
      }
    }

    "must redirect to Journey Recovery when valid data is submitted" in {
      val mockSessionRepository = mock[SessionRepository]
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
          .build()

      running(application) {
        val request = FakeRequest(POST, controllers.imports.routes.TotalAmountWithoutVatController.onSubmit(models.NormalMode).url)
          .withFormUrlEncodedBody(("value", "123.45"))
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
        verify(mockSessionRepository).set(any())
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(POST, controllers.imports.routes.TotalAmountWithoutVatController.onSubmit(models.NormalMode).url)
          .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))
        val view = application.injector.instanceOf[TotalAmountWithoutVatView]
        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        normalizeHtml(contentAsString(result)) mustEqual normalizeHtml(
          view(boundForm, models.NormalMode, backLink, "€", "Euro")(request, messages(application)).toString
        )
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {
      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, totalAmountWithoutVatRoute)
        val result = route(application, request).value
        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {
      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(POST, controllers.imports.routes.TotalAmountWithoutVatController.onSubmit(models.NormalMode).url)
          .withFormUrlEncodedBody(("value", "123.45"))
        val result = route(application, request).value
        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
