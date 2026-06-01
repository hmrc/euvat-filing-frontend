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

      "must go from BusinessActivityPage to JourneyRecoveryPage if no selected" in { // TODO - update to Check your claim details page
        val ua = userAnswers.set(BusinessActivityPage, false).success.value
        navigator.nextPage(BusinessActivityPage, NormalMode, ua) mustBe
          routes.JourneyRecoveryController.onPageLoad()
      }

      "must go from BusinessActivityCodeTwoPage to BusinessActivityTwoController" in {
        navigator.nextPage(BusinessActivityCodeTwoPage, NormalMode, userAnswers) mustBe
          routes.BusinessActivityTwoController.onPageLoad(NormalMode)
      }

      "must go from BusinessActivityTwoPage to BusinessActivityCodeThreeController if yes selected" in {
        val ua = userAnswers.set(BusinessActivityTwoPage, true).success.value
        navigator.nextPage(BusinessActivityTwoPage, NormalMode, ua) mustBe
          routes.BusinessActivityCodeThreeController.onPageLoad(NormalMode)
      }

      "must go from BusinessActivityTwoPage to JourneyRecoveryController if no selected" in {
        val ua = userAnswers.set(BusinessActivityTwoPage, false).success.value
        navigator.nextPage(BusinessActivityTwoPage, NormalMode, ua) mustBe
          routes.JourneyRecoveryController.onPageLoad()
      }

      "must go from BusinessActivityCodeThreePage to BusinessActivityThreeController" in {
        navigator.nextPage(BusinessActivityCodeThreePage, NormalMode, userAnswers) mustBe
          routes.BusinessActivityThreeController.onPageLoad()
      }

      "must go from PurchaseTypePage to InvoiceNumberController" in {
        navigator.nextPage(PurchaseTypePage, NormalMode, userAnswers) mustBe
          routes.InvoiceNumberController.onPageLoad(NormalMode)
      }

      "must go from SuppliersNamePage to SupplierAddressController" in {
        navigator.nextPage(SuppliersNamePage, NormalMode, userAnswers) mustBe
          routes.SupplierAddressController.onPageLoad(NormalMode)
      }

      "must go from SupplierAddressPage to JourneyRecoveryController" in {
        navigator.nextPage(SupplierAddressPage, NormalMode, userAnswers) mustBe
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
    }

    "in Check mode" - {
      "must go from a page that doesn't exist in the edit route map to CheckYourAnswers" in {
        case object UnknownPage extends Page
        navigator.nextPage(UnknownPage, CheckMode, userAnswers) mustBe routes.CheckYourAnswersController.onPageLoad()
      }

      "must go from RefundingCountryPage to RefundingLanguageController" in {
        navigator.nextPage(pages.RefundingCountryPage, CheckMode, userAnswers) mustBe
          routes.RefundingLanguageController.onPageLoad(CheckMode)
      }

      "must go from RefundingLanguagePage to RefundPeriodController" in {
        navigator.nextPage(pages.RefundingLanguagePage, CheckMode, userAnswers) mustBe
          routes.RefundPeriodController.onPageLoad(CheckMode)
      }

      "must go from RefundPeriodPage to ContactDetailsController" in {
        navigator.nextPage(pages.RefundPeriodPage, CheckMode, userAnswers) mustBe
          routes.ContactDetailsController.onPageLoad(CheckMode)
      }

      "must go from ContactDetailsPage to BusinessActivityController" in {
        navigator.nextPage(ContactDetailsPage, CheckMode, userAnswers) mustBe
          routes.BusinessActivityController.onPageLoad(CheckMode)
      }

      "must go from BusinessActivityPage to BusinessActivityCodeTwoController if yes selected" in {
        val ua = userAnswers.set(BusinessActivityPage, true).success.value
        navigator.nextPage(BusinessActivityPage, CheckMode, ua) mustBe
          routes.BusinessActivityCodeTwoController.onPageLoad(CheckMode)
      }

      "must go from BusinessActivityPage to JourneyRecoveryController if no selected" in { // TODO - update to Check your claim details page
        val ua = userAnswers.set(BusinessActivityPage, false).success.value
        navigator.nextPage(BusinessActivityPage, CheckMode, ua) mustBe
          routes.JourneyRecoveryController.onPageLoad()
      }

      "must go from BusinessActivityCodeTwoPage to BusinessActivityTwoController" in {
        navigator.nextPage(BusinessActivityCodeTwoPage, CheckMode, userAnswers) mustBe
          routes.BusinessActivityTwoController.onPageLoad(CheckMode)
      }

      "must go from BusinessActivityTwoPage to BusinessActivityCodeThreeController if yes selected" in {
        val ua = userAnswers.set(BusinessActivityTwoPage, true).success.value
        navigator.nextPage(BusinessActivityTwoPage, CheckMode, ua) mustBe
          routes.BusinessActivityCodeThreeController.onPageLoad(CheckMode)
      }

      "must go from BusinessActivityTwoPage to JourneyRecoveryController if no selected" in {
        val ua = userAnswers.set(BusinessActivityTwoPage, false).success.value
        navigator.nextPage(BusinessActivityTwoPage, CheckMode, ua) mustBe
          routes.JourneyRecoveryController.onPageLoad()
      }

      "must go from BusinessActivityCodeThreePage to BusinessActivityThreeController" in {
        navigator.nextPage(BusinessActivityCodeThreePage, CheckMode, userAnswers) mustBe
          routes.BusinessActivityThreeController.onPageLoad()
      }

      "must go from SupplierAddressPage to CheckYourAnswersController" in {
        navigator.nextPage(SupplierAddressPage, CheckMode, userAnswers) mustBe
          routes.CheckYourAnswersController.onPageLoad()
      }

      "must go from PurchaseTypePage to InvoiceNumberController" in {
        navigator.nextPage(PurchaseTypePage, CheckMode, userAnswers) mustBe
          routes.InvoiceNumberController.onPageLoad(CheckMode)
      }

      "must go from SuppliersNamePage to SupplierAddressController in CheckMode" in {
        navigator.nextPage(SuppliersNamePage, CheckMode, userAnswers) mustBe
          routes.SupplierAddressController.onPageLoad(CheckMode)
      }

      "must go from InvoiceDatePage to SuppliersNameController in CheckMode" in {
        navigator.nextPage(InvoiceDatePage, CheckMode, userAnswers) mustBe
          routes.SuppliersNameController.onPageLoad(CheckMode)
      }

    }
  }

}
