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

package controllers.warning

import base.SpecBase
import controllers.imports
import controllers.purchase
import controllers.warning
import models.{CheckMode, NormalMode, PurchaseOrImport}
import pages.PurchaseOrImportPage
import pages.imports.{TotalAmountWithoutVatPage, TotalVatPaidImportPage}
import pages.purchase.{TotalPurchaseAmountBeforeVatPage, TotalVatPaidPage}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import views.html.warning.VatPaidWarningView

class VatPaidWarningControllerSpec extends SpecBase {

  private val vatPaid = BigDecimal("999")
  private val totalAmount = BigDecimal("1000")
  private val currencySymbol = "€"
  private lazy val purchasesBackLink = purchase.routes.TotalVatPaidController.onPageLoad(NormalMode)
  private lazy val importsBackLink = imports.routes.TotalVatPaidImportController.onPageLoad(NormalMode)

  "VatPaidWarningController Controller" - {

    "must return OK and the correct view for a GET on a Purchase in NormalMode" in {
      val answers = emptyUserAnswers.set(TotalPurchaseAmountBeforeVatPage, totalAmount).success.value
      val userAnswers = answers.set(TotalVatPaidPage, vatPaid).success.value
      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, warning.routes.VatPaidWarningController.onPageLoad(NormalMode).url)
        val result = route(application, request).value
        val view = application.injector.instanceOf[VatPaidWarningView]
        status(result) mustEqual OK

        contentAsString(result) mustEqual view(purchasesBackLink, NormalMode, currencySymbol, vatPaid)(
          request,
          messages(application)
        ).toString
      }
    }

    "must return OK and the correct view for a GET on an Import in NormalMode" in {
      val answers = emptyUserAnswers.set(TotalAmountWithoutVatPage, totalAmount).success.value
      val userAnswers = answers.set(TotalVatPaidImportPage, vatPaid).success.value
      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, warning.routes.VatPaidWarningController.onPageLoad(NormalMode).url)
        val result = route(application, request).value
        val view = application.injector.instanceOf[VatPaidWarningView]
        status(result) mustEqual OK

        contentAsString(result) mustEqual view(importsBackLink, NormalMode, currencySymbol, vatPaid)(
          request,
          messages(application)
        ).toString
      }
    }

    "must return OK and the correct view for a GET on a Purchase in NormalMode with pre-populate form" in {
      val answers = emptyUserAnswers.set(TotalPurchaseAmountBeforeVatPage, totalAmount).success.value
      val userAnswers = answers.set(TotalVatPaidPage, vatPaid).success.value
      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, warning.routes.VatPaidWarningController.onPageLoad(NormalMode).url)
        val result = route(application, request).value
        status(result) mustEqual OK
      }
    }

    "must return OK and the correct view for a GET on an Import in NormalMode with pre-populate form" in {
      val answers = emptyUserAnswers.set(TotalAmountWithoutVatPage, totalAmount).success.value
      val userAnswers = answers.set(TotalVatPaidImportPage, vatPaid).success.value
      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, warning.routes.VatPaidWarningController.onPageLoad(NormalMode).url)
        val result = route(application, request).value
        status(result) mustEqual OK
      }
    }

    "redirect to error page if url hopping without the required session data" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, warning.routes.VatPaidWarningController.onPageLoad(NormalMode).url)
        val result = route(application, request).value
        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to next page on submit for Purchase in NormalMode" in {
      val userAnswers = emptyUserAnswers.set(PurchaseOrImportPage, PurchaseOrImport.Purchase).success.value
      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(POST, warning.routes.VatPaidWarningController.onSubmit(NormalMode).url)
        val result = route(application, request).value
        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual purchase.routes.TotalVatClaimController.onPageLoad(NormalMode).url
      }
    }

    "must redirect to next page on submit for Import in NormalMode" in {
      val userAnswers = emptyUserAnswers.set(PurchaseOrImportPage, PurchaseOrImport.Import).success.value
      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(POST, warning.routes.VatPaidWarningController.onSubmit(NormalMode).url)
        val result = route(application, request).value
        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual imports.routes.TotalVatClaimController.onPageLoad(NormalMode).url
      }
    }

    "must redirect to next page on submit for Purchase in CheckMode" in {
      val userAnswers = emptyUserAnswers.set(PurchaseOrImportPage, PurchaseOrImport.Purchase).success.value
      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(POST, warning.routes.VatPaidWarningController.onSubmit(CheckMode).url)
        val result = route(application, request).value
        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual purchase.routes.TotalVatClaimController.onPageLoad(CheckMode).url
      }
    }

    "must redirect to next page on submit for Import in CheckMode" in {
      val userAnswers = emptyUserAnswers.set(PurchaseOrImportPage, PurchaseOrImport.Import).success.value
      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(POST, warning.routes.VatPaidWarningController.onSubmit(CheckMode).url)
        val result = route(application, request).value
        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual imports.routes.TotalVatClaimController.onPageLoad(CheckMode).url
      }
    }
  }
}
