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

package navigation

import base.SpecBase
import controllers.routes
import models.*
import pages.*

class NavigatorSpec extends SpecBase {

  val navigator = new Navigator
  val userAnswers: UserAnswers = UserAnswers("id")

  "Navigator" - {

    "in Normal mode" - {
      "must go from a page that doesn't exist in the route map to Index" in {
        case object UnknownPage extends Page
        navigator.nextPage(UnknownPage, NormalMode, userAnswers) mustBe routes.IndexController.onPageLoad()
      }

      "must go from RefundingCountryPage to RefundingLanguageController" in {
        navigator.nextPage(pages.RefundingCountryPage, NormalMode, userAnswers) mustBe
          routes.RefundingLanguageController.onPageLoad(NormalMode)
      }

      "must go from RefundingLanguagePage to RefundPeriodController" in {
        navigator.nextPage(pages.RefundingLanguagePage, NormalMode, userAnswers) mustBe
          routes.RefundPeriodController.onPageLoad(NormalMode)
      }

      "must go from RefundPeriodPage to ContactDetailsController" in {
        navigator.nextPage(pages.RefundPeriodPage, NormalMode, userAnswers) mustBe
          routes.ContactDetailsController.onPageLoad(NormalMode)
      }

      "must go from ContactDetailsPage to BusinessActivityController" in {
        navigator.nextPage(ContactDetailsPage, NormalMode, userAnswers) mustBe
          routes.BusinessActivityController.onPageLoad(NormalMode)
      }

      "must go from BusinessActivityPage to BusinessActivityCodeTwoController if yes selected" in {
        val ua = userAnswers.set(BusinessActivityPage, true).success.value
        navigator.nextPage(BusinessActivityPage, NormalMode, ua) mustBe
          routes.BusinessActivityCodeTwoController.onPageLoad(NormalMode)
      }

      "must go from BusinessActivityPage to CheckYourClaimDetailsPage if no selected" in {
        val ua = userAnswers.set(BusinessActivityPage, false).success.value
        navigator.nextPage(BusinessActivityPage, NormalMode, ua) mustBe
          routes.CheckYourClaimDetailsController.onPageLoad()
      }

      "must go from BusinessActivityTwoPage to BusinessActivityCodeThreeController if yes selected" in {
        val ua = userAnswers.set(BusinessActivityTwoPage, true).success.value
        navigator.nextPage(BusinessActivityTwoPage, NormalMode, ua) mustBe
          routes.BusinessActivityCodeThreeController.onPageLoad(NormalMode)
      }

      "must go from BusinessActivityTwoPage to CheckYourClaimDetailsPage if no selected" in {
        val ua = userAnswers.set(BusinessActivityTwoPage, false).success.value
        navigator.nextPage(BusinessActivityTwoPage, NormalMode, ua) mustBe
          routes.CheckYourClaimDetailsController.onPageLoad()
      }

      "must go from BusinessActivityCodeThreePage to BusinessActivityThreeController" in {
        navigator.nextPage(BusinessActivityCodeThreePage, NormalMode, userAnswers) mustBe
          routes.BusinessActivityThreeController.onPageLoad()
      }

      "must go from PurchaseTypePage to JourneyRecoveryController" in { // TODO - update to about the purchase page when built
        navigator.nextPage(PurchaseTypePage, NormalMode, userAnswers) mustBe
          routes.JourneyRecoveryController.onPageLoad()
      }

      "must go from InvoiceNumberPage to InvoiceDateController" in {
        navigator.nextPage(InvoiceNumberPage, NormalMode, userAnswers) mustBe
          routes.InvoiceDateController.onPageLoad(NormalMode)
      }

      "must go from InvoiceDatePage to SuppliersNameController" in {
        navigator.nextPage(InvoiceDatePage, NormalMode, userAnswers) mustBe
          routes.SuppliersNameController.onPageLoad(NormalMode)
      }

      "must go from SuppliersNamePage to SupplierAddressController" in {
        navigator.nextPage(SuppliersNamePage, NormalMode, userAnswers) mustBe
          routes.SupplierAddressController.onPageLoad(NormalMode)
      }

      "must go from SupplierAddressPage to SimplifiedInvoiceVatRegCheckController" in {
        navigator.nextPage(SupplierAddressPage, NormalMode, userAnswers) mustBe
          routes.SimplifiedInvoiceVatRegCheckController.onPageLoad(NormalMode)
      }

      "must go from SimplifiedInvoiceVatRegCheckPage to JourneyRecoveryController if yes selected" in { // TODO - update to supplier VRN page
        val ua = userAnswers.set(SimplifiedInvoiceVatRegCheckPage, true).success.value
        navigator.nextPage(SimplifiedInvoiceVatRegCheckPage, NormalMode, ua) mustBe
          routes.JourneyRecoveryController.onPageLoad()
      }

      "must go from SimplifiedInvoiceVatRegCheckPage to PurchaseTypeController if no selected" in {
        val ua = userAnswers.set(SimplifiedInvoiceVatRegCheckPage, false).success.value
        navigator.nextPage(SimplifiedInvoiceVatRegCheckPage, NormalMode, ua) mustBe
          routes.PurchaseTypeController.onPageLoad(NormalMode)
      }
    }

    "in Check mode" - {
      "must go from a page that doesn't exist in the edit route map to IndexController" in {
        case object UnknownPage extends Page
        navigator.nextPage(UnknownPage, CheckMode, userAnswers) mustBe routes.IndexController.onPageLoad()
      }

      "must go from RefundingCountryPage to RefundingLanguageController" in {
        navigator.nextPage(pages.RefundingCountryPage, CheckMode, userAnswers) mustBe
          routes.RefundingLanguageController.onPageLoad(CheckMode)
      }

      "must go from RefundingLanguagePage to CheckYourClaimDetailsController" in {
        navigator.nextPage(pages.RefundingLanguagePage, CheckMode, userAnswers) mustBe
          routes.CheckYourClaimDetailsController.onPageLoad()
      }

      "must go from RefundPeriodPage to CheckYourClaimDetailsController" in {
        navigator.nextPage(pages.RefundPeriodPage, CheckMode, userAnswers) mustBe
          routes.CheckYourClaimDetailsController.onPageLoad()
      }

      "must go from ContactDetailsPage to CheckYourClaimDetailsController" in {
        navigator.nextPage(ContactDetailsPage, CheckMode, userAnswers) mustBe
          routes.CheckYourClaimDetailsController.onPageLoad()
      }

      "must go from BusinessActivityPage to BusinessActivityCodeTwoController if yes selected" in {
        val ua = userAnswers.set(BusinessActivityPage, true).success.value
        navigator.nextPage(BusinessActivityPage, CheckMode, ua) mustBe
          routes.BusinessActivityCodeTwoController.onPageLoad(CheckMode)
      }

      "must go from BusinessActivityPage to CheckYourClaimDetailsPage if no selected" in {
        val ua = userAnswers.set(BusinessActivityPage, false).success.value
        navigator.nextPage(BusinessActivityPage, CheckMode, ua) mustBe
          routes.CheckYourClaimDetailsController.onPageLoad()
      }

      "must go from BusinessActivityTwoPage to BusinessActivityCodeThreeController if yes selected" in {
        val ua = userAnswers.set(BusinessActivityTwoPage, true).success.value
        navigator.nextPage(BusinessActivityTwoPage, CheckMode, ua) mustBe
          routes.BusinessActivityCodeThreeController.onPageLoad(CheckMode)
      }

      "must go from BusinessActivityTwoPage to CheckYourClaimDetailsPage if no selected" in {
        val ua = userAnswers.set(BusinessActivityTwoPage, false).success.value
        navigator.nextPage(BusinessActivityTwoPage, CheckMode, ua) mustBe
          routes.CheckYourClaimDetailsController.onPageLoad()
      }

      "must go from BusinessActivityCodeThreePage to BusinessActivityThreeController" in {
        navigator.nextPage(BusinessActivityCodeThreePage, CheckMode, userAnswers) mustBe
          routes.BusinessActivityThreeController.onPageLoad()
      }

      "must go from PurchaseTypePage to IndexController" in {
        navigator.nextPage(PurchaseTypePage, CheckMode, userAnswers) mustBe
          routes.IndexController.onPageLoad()
      }

      "must go from InvoiceNumberPage to InvoiceDateController in CheckMode" in {
        navigator.nextPage(InvoiceNumberPage, CheckMode, userAnswers) mustBe
          routes.InvoiceDateController.onPageLoad(CheckMode)
      }

      "must go from InvoiceDatePage to SuppliersNameController in CheckMode" in {
        navigator.nextPage(InvoiceDatePage, CheckMode, userAnswers) mustBe
          routes.SuppliersNameController.onPageLoad(CheckMode)
      }

      "must go from SuppliersNamePage to SupplierAddressController in CheckMode" in {
        navigator.nextPage(SuppliersNamePage, CheckMode, userAnswers) mustBe
          routes.SupplierAddressController.onPageLoad(CheckMode)
      }

      "must go from SupplierAddressPage to SimplifiedInvoiceVatRegCheckController" in {
        navigator.nextPage(SupplierAddressPage, CheckMode, userAnswers) mustBe
          routes.SimplifiedInvoiceVatRegCheckController.onPageLoad(CheckMode)
      }

      "must go from SimplifiedInvoiceVatRegCheckPage to JourneyRecoveryController if yes selected" in { // TODO - update to supplier VRN page
        val ua = userAnswers.set(SimplifiedInvoiceVatRegCheckPage, true).success.value
        navigator.nextPage(SimplifiedInvoiceVatRegCheckPage, CheckMode, ua) mustBe
          routes.JourneyRecoveryController.onPageLoad()
      }

      "must go from SimplifiedInvoiceVatRegCheckPage to PurchaseTypeController if no selected" in {
        val ua = userAnswers.set(SimplifiedInvoiceVatRegCheckPage, false).success.value
        navigator.nextPage(SimplifiedInvoiceVatRegCheckPage, CheckMode, ua) mustBe
          routes.PurchaseTypeController.onPageLoad(CheckMode)
      }
    }
  }
}
