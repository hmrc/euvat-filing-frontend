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
import forms.imports.TotalVatPaidImportFormProvider
import models.{CheckMode, Mode, NormalMode, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.imports.{TotalAmountWithoutVatPage, TotalVatPaidImportPage}
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import play.api.data.Form
import repositories.SessionRepository
import views.html.purchasesOrImports.PurchaseOrImportTotalVatPaidView

import scala.concurrent.Future

class TotalVatPaidImportControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute: Call = Call("GET", "/foo")

  val formProvider = new TotalVatPaidImportFormProvider()
  val form: Form[BigDecimal] = formProvider()
  val validVatAmount: BigDecimal = BigDecimal("678.90")
  val amountGreaterThanVat: BigDecimal = validVatAmount + BigDecimal("0.01")
  val amountLessThanVat: BigDecimal = validVatAmount - BigDecimal("0.01")

  lazy val totalVatPaidImportRoute: String = routes.TotalVatPaidImportController.onPageLoad(NormalMode).url

  def backLink(mode: Mode): Call =
    routes.TotalAmountWithoutVatController.onPageLoad(mode)

  "TotalVatPaidImport Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, totalVatPaidImportRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[PurchaseOrImportTotalVatPaidView]

        status(result) mustEqual OK
        normalizeHtml(contentAsString(result)) mustEqual normalizeHtml(
          view(form,
               routes.TotalVatPaidImportController.onSubmit(NormalMode),
               backLink(NormalMode),
               "import.caption",
               "totalVatPaidImport.p1",
               "€",
               "Euro"
              )(
            request,
            messages(application)
          ).toString
        )
      }
    }

    "must return OK and the correct view for a GET in CheckMode" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, routes.TotalVatPaidImportController.onPageLoad(CheckMode).url)

        val view = application.injector.instanceOf[PurchaseOrImportTotalVatPaidView]

        val result = route(application, request).value

        status(result) mustEqual OK
        normalizeHtml(contentAsString(result)) mustEqual normalizeHtml(
          view(form,
               routes.TotalVatPaidImportController.onSubmit(CheckMode),
               backLink(CheckMode),
               "import.caption",
               "totalVatPaidImport.p1",
               "€",
               "Euro"
              )(
            request,
            messages(application)
          ).toString
        )
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = emptyUserAnswers.set(TotalVatPaidImportPage, validVatAmount).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, totalVatPaidImportRoute)

        val view = application.injector.instanceOf[PurchaseOrImportTotalVatPaidView]

        val result = route(application, request).value

        status(result) mustEqual OK
        normalizeHtml(contentAsString(result)) mustEqual normalizeHtml(
          view(
            form.fill(validVatAmount),
            routes.TotalVatPaidImportController.onSubmit(NormalMode),
            backLink(NormalMode),
            "import.caption",
            "totalVatPaidImport.p1",
            "€",
            "Euro"
          )(
            request,
            messages(application)
          ).toString
        )
      }
    }

    "must redirect to the next page when VAT submitted is less than Total import amount without VAT" in {

      val userAnswers = emptyUserAnswers.set(TotalAmountWithoutVatPage, amountGreaterThanVat).success.value

      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, totalVatPaidImportRoute)
            .withFormUrlEncodedBody(("value", validVatAmount.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must redirect to the warning page when Total VAT paid is > Total import amount without VAT" in {

      val userAnswers = emptyUserAnswers.set(TotalAmountWithoutVatPage, amountLessThanVat).success.value

      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, totalVatPaidImportRoute)
            .withFormUrlEncodedBody(("value", validVatAmount.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.warning.routes.VatPaidWarningController.onPageLoad(NormalMode).url
      }
    }

    "must redirect to the warning page when Total VAT paid is equal to the Total import amount without VAT" in {

      val userAnswers = emptyUserAnswers.set(TotalAmountWithoutVatPage, validVatAmount).success.value

      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, totalVatPaidImportRoute)
            .withFormUrlEncodedBody(("value", validVatAmount.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.warning.routes.VatPaidWarningController.onPageLoad(NormalMode).url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, totalVatPaidImportRoute)
            .withFormUrlEncodedBody(("value", "invalid value"))

        val boundForm = form.bind(Map("value" -> "invalid value"))

        val view = application.injector.instanceOf[PurchaseOrImportTotalVatPaidView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        normalizeHtml(contentAsString(result)) mustEqual normalizeHtml(
          view(boundForm,
               routes.TotalVatPaidImportController.onSubmit(NormalMode),
               backLink(NormalMode),
               "import.caption",
               "totalVatPaidImport.p1",
               "€",
               "Euro"
              )(
            request,
            messages(application)
          ).toString
        )
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, totalVatPaidImportRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, totalVatPaidImportRoute)
            .withFormUrlEncodedBody(("value", validVatAmount.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
