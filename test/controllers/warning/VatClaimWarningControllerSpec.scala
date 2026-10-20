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
import controllers.warning.routes
import models.{CheckMode, NormalMode}
import models.PurchaseOrImport.{Import, Purchase}
import navigation.{FakeNavigator, Navigator}
import pages.PurchaseOrImportPage
import pages.purchase.{TotalVatClaimPage, TotalVatPaidPage}
import pages.imports.{ImportTotalVatClaimPage, TotalVatPaidImportPage}
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import controllers.imports.routes as importRoutes
import controllers.purchase.routes as purchaseRoutes
import controllers.routes.JourneyRecoveryController
import views.html.warning.VatClaimWarningView

class VatClaimWarningControllerSpec extends SpecBase {

  def onwardRoute: Call = Call("GET", "/foo")

  "VatClaimWarningController Controller" - {

    "must return OK and the correct view for a GET in NormalMode" in {
      val answers = emptyUserAnswers.set(TotalVatClaimPage, BigDecimal("100")).success.value
      val userAnswers = answers
        .set(TotalVatPaidPage, BigDecimal("99"))
        .success
        .value
        .set(PurchaseOrImportPage, Purchase)
        .success
        .value
      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, routes.VatClaimWarningController.onPageLoad(NormalMode).url)
        val result = route(application, request).value
        val view = application.injector.instanceOf[VatClaimWarningView]
        status(result) mustEqual OK
        contentAsString(result) mustEqual view(purchaseRoutes.TotalVatClaimController.onPageLoad(NormalMode), NormalMode, "€", BigDecimal(100))(
          request,
          messages(application)
        ).toString
      }
    }

    "redirect to error page if url hopping without the required session data" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, routes.VatClaimWarningController.onPageLoad(NormalMode).url)
        val result = route(application, request).value
        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to next page on submit in NormalMode" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers.set(PurchaseOrImportPage, Purchase).success.value)).build()

      running(application) {
        val request = FakeRequest(POST, routes.VatClaimWarningController.onSubmit(NormalMode).url)
        val result = route(application, request).value
        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual purchaseRoutes.CheckYourPurchaseDetailsController.onPageLoad().url
      }
    }

    "must redirect to next page on submit in CheckMode" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers.set(PurchaseOrImportPage, Purchase).success.value)).build()

      running(application) {
        val request = FakeRequest(POST, routes.VatClaimWarningController.onSubmit(CheckMode).url)
        val result = route(application, request).value
        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual purchaseRoutes.CheckYourPurchaseDetailsController.onPageLoad().url
      }
    }

    "must return OK and the correct view for a GET in NormalMode for the import journey" in {
      val userAnswers = emptyUserAnswers
        .set(PurchaseOrImportPage, Import)
        .success
        .value
        .set(ImportTotalVatClaimPage, BigDecimal("100"))
        .success
        .value
        .set(TotalVatPaidImportPage, BigDecimal("99"))
        .success
        .value
      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, routes.VatClaimWarningController.onPageLoad(NormalMode).url)
        val result = route(application, request).value
        val view = application.injector.instanceOf[VatClaimWarningView]
        status(result) mustEqual OK
        contentAsString(result) mustEqual view(importRoutes.TotalVatClaimController.onPageLoad(NormalMode), NormalMode, "€", BigDecimal(100))(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to Journey Recovery for a GET in purchase or import has not been answered" in {
      val userAnswers = emptyUserAnswers
        .set(TotalVatClaimPage, BigDecimal("100"))
        .success
        .value
        .set(TotalVatPaidPage, BigDecimal("99"))
        .success
        .value
      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, routes.VatClaimWarningController.onPageLoad(NormalMode).url)
        val result = route(application, request).value
        val view = application.injector.instanceOf[VatClaimWarningView]
        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to the next page on submit for the import journey" in {
      val userAnswers = emptyUserAnswers.set(PurchaseOrImportPage, Import).success.value
      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(bind[Navigator].toInstance(new FakeNavigator(onwardRoute)))
        .build()

      running(application) {
        val request = FakeRequest(POST, routes.VatClaimWarningController.onSubmit(NormalMode).url)
        val result = route(application, request).value
        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must redirect to Journey Recovery on Submit if purchase or import has not been answered" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(POST, routes.VatClaimWarningController.onSubmit(NormalMode).url)
        val result = route(application, request).value
        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
