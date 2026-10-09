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
import forms.purchase.TotalVatClaimFormProvider
import models.{NormalMode, UserAnswers}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when}
import org.scalatest.TryValues.*
import org.scalatestplus.mockito.MockitoSugar
import pages.claim.RefundingCountryPage
import pages.purchase.RefundingCurrencyPage
import pages.imports.ImportTotalVatClaimPage
import play.api.data.Form
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import views.html.purchasesOrImports.PurchaseOrImportTotalVatClaimView

import scala.concurrent.Future

class TotalVatClaimControllerSpec extends SpecBase with MockitoSugar {

  val form: Form[BigDecimal] = new TotalVatClaimFormProvider()()
  lazy val totalVatClaimRoute: String = controllers.imports.routes.TotalVatClaimController.onPageLoad(NormalMode).url
  lazy val backLink: Call = controllers.imports.routes.TotalAmountWithoutVatController.onPageLoad(NormalMode)

  "TotalVatClaim import controller" - {

    "must return OK and the correct view for a GET" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, totalVatClaimRoute)
        val result = route(application, request).value
        val view = application.injector.instanceOf[PurchaseOrImportTotalVatClaimView]

        status(result) mustEqual OK
        val expectedBack = controllers.imports.routes.TotalVatPaidImportController.onPageLoad(NormalMode)
        normalizeHtml(contentAsString(result)) mustEqual normalizeHtml(
          view(form, NormalMode, controllers.imports.routes.TotalVatClaimController.onSubmit(NormalMode), expectedBack, "import.caption", "€")(
            request,
            messages(application)
          ).toString
        )
      }
    }

    "must prefill saved answers and use the selected currency symbol" in {
      val userAnswers = UserAnswers(userAnswersId)
        .set(RefundingCountryPage, "BG")
        .success
        .value
        .set(RefundingCurrencyPage, "BGN")
        .success
        .value
        .set(ImportTotalVatClaimPage, BigDecimal("12.34"))
        .success
        .value
      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, totalVatClaimRoute)
        val result = route(application, request).value
        val view = application.injector.instanceOf[PurchaseOrImportTotalVatClaimView]

        status(result) mustEqual OK
        val expectedBack = controllers.imports.routes.TotalVatPaidImportController.onPageLoad(NormalMode)
        normalizeHtml(contentAsString(result)) mustEqual normalizeHtml(
          view(
            form.fill(BigDecimal("12.34")),
            NormalMode,
            controllers.imports.routes.TotalVatClaimController.onSubmit(NormalMode),
            expectedBack,
            "import.caption",
            "лв"
          )(request, messages(application)).toString
        )
      }
    }

    "must save a valid claim and continue to check purchase details" in {
      val mockSessionRepository = mock[SessionRepository]
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
          .build()

      running(application) {
        val request = FakeRequest(POST, totalVatClaimRoute).withFormUrlEncodedBody(("value", "123.45"))
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
        verify(mockSessionRepository).set(any())
      }
    }

    "must return a Bad Request for invalid and out-of-range values" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val invalidRequest = FakeRequest(POST, totalVatClaimRoute).withFormUrlEncodedBody(("value", "invalid value"))
        val invalidResult = route(application, invalidRequest).value
        status(invalidResult) mustEqual BAD_REQUEST
        contentAsString(invalidResult) must include(messages(application)("totalVatClaim.error.invalidNumeric"))

        val outOfRangeRequest = FakeRequest(POST, totalVatClaimRoute).withFormUrlEncodedBody(("value", "1000000000"))
        val outOfRangeResult = route(application, outOfRangeRequest).value
        status(outOfRangeResult) mustEqual BAD_REQUEST
        contentAsString(outOfRangeResult) must include(messages(application)("totalVatClaim.error.aboveMaximum"))
      }
    }
  }
}
