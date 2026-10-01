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
import com.typesafe.config.ConfigFactory
import controllers.claim.routes as claimRoutes
import controllers.purchase.routes as purchaseRoutes
import controllers.imports.routes as importRoutes
import models.*
import models.PurchaseOrImport.{Import, Purchase}
import pages.*
import play.api.Configuration
import play.api.mvc.Call
import utils.{ConfigLanguageMapping, ConfigPurchaseOrImportMapping, CurrencyConfig}

class NavigatorSpec extends SpecBase {

  val navigator = new Navigator(
    new ClaimNavigator(
      new ConfigLanguageMapping(
        Configuration(
          ConfigFactory.parseString("""
              language.mapping {
                AT = ["german", "english"]
                BE = ["english", "german", "french", "dutch"]
                CZ = ["czech"]
              }
            """)
        )
      )
    ),
    new PurchaseNavigator(
      new CurrencyConfig(
        Configuration(
          ConfigFactory.parseString("""
              currency.mapping {
                BG = ["euro|EUR|€", "bulgarianLev|BGN|лв"]
                EE = ["euro|EUR|€", "estonianKroon|EEK|kr"]
                AT = ["euro|EUR|€"]
              }
            """)
        )
      ),
      new ConfigPurchaseOrImportMapping(
        Configuration(
          ConfigFactory.parseString("""
              purchase.mapping {
                DE = ["parent|sub1|purchase.sub.parent.sub1"]
              }
            """)
        )
      )
    ),
    new ImportNavigator(
      new CurrencyConfig(
        Configuration(
          ConfigFactory.parseString("""
              currency.mapping {
                BG = ["euro|EUR|€", "bulgarianLev|BGN|лв"]
                EE = ["euro|EUR|€", "estonianKroon|EEK|kr"]
                AT = ["euro|EUR|€"]
              }
            """)
        )
      ),
      new ConfigPurchaseOrImportMapping(
        Configuration(
          ConfigFactory.parseString("""
              purchase.mapping {
                DE = ["parent|sub1|purchase.sub.parent.sub1"]
              }
            """)
        )
      )
    )
  )
  private val userAnswers: UserAnswers = emptyUserAnswers

  "Navigator" - {

    "in Normal mode" - {
      "must go from RefundPeriodPage to ContactDetailsController" in {
        navigator.nextPage(pages.RefundPeriodPage, NormalMode, userAnswers) mustBe
          claimRoutes.ContactDetailsController.onPageLoad(NormalMode)
      }

      "must go from ContactDetailsPage to BusinessActivityController" in {
        navigator.nextPage(ContactDetailsPage, NormalMode, userAnswers) mustBe
          claimRoutes.BusinessActivityController.onPageLoad(NormalMode)
      }

      "must go from BusinessActivityCodeThreePage to BusinessActivityThreeController" in {
        navigator.nextPage(BusinessActivityCodeThreePage, NormalMode, userAnswers) mustBe
          claimRoutes.BusinessActivityThreeController.onPageLoad()
      }

      "must go from PurchaseOrImportPage to PurchaseTypeController when Purchase is selected" in {
        val answers = userAnswers.set(PurchaseOrImportPage, Purchase).success.value

        navigator.nextPage(PurchaseOrImportPage, NormalMode, answers) mustBe
          purchaseRoutes.PurchaseTypeController.onPageLoad(NormalMode)
      }

      "must go from DescribeItemsOnInvoicePage to InvoiceTypeController" in {
        navigator.nextPage(DescribeItemsOnInvoicePage, NormalMode, userAnswers) mustBe
          purchaseRoutes.InvoiceTypeController.onPageLoad(NormalMode)
      }

      "must go from InvoiceTypePage to InvoiceNumberController" in {
        navigator.nextPage(InvoiceTypePage, NormalMode, userAnswers) mustBe
          purchaseRoutes.InvoiceNumberController.onPageLoad(NormalMode)
      }

      "must go from InvoiceDatePage to SuppliersNameController" in {
        navigator.nextPage(InvoiceDatePage, NormalMode, userAnswers) mustBe
          purchaseRoutes.SuppliersNameController.onPageLoad(NormalMode)
      }

      "must go from SuppliersNamePage to SupplierAddressController" in {
        navigator.nextPage(SuppliersNamePage, NormalMode, userAnswers) mustBe
          purchaseRoutes.SupplierAddressController.onPageLoad(NormalMode)
      }

      "must go from SupplierTaxIdentifierNumberPage to TotalPurchaseAmountBeforeVatController" in {
        navigator.nextPage(SupplierTaxIdentifierNumberPage, NormalMode, userAnswers) mustBe
          purchaseRoutes.TotalPurchaseAmountBeforeVatController.onPageLoad(NormalMode)
      }

      "must go from TotalPurchaseAmountBeforeVatPage to TotalVatPaidController" in {
        navigator.nextPage(TotalPurchaseAmountBeforeVatPage, NormalMode, userAnswers) mustBe
          purchaseRoutes.TotalVatPaidController.onPageLoad(NormalMode)
      }

      "must go from TotalVatPaidPage to TotalVatClaimController" in {
        navigator.nextPage(TotalVatPaidPage, NormalMode, userAnswers) mustBe
          purchaseRoutes.TotalVatClaimController.onPageLoad(NormalMode)
      }

      "must go from TotalVatClaimPage to CheckYourPurchaseDetailsController" in {
        navigator.nextPage(TotalVatClaimPage, NormalMode, userAnswers) mustBe
          purchaseRoutes.CheckYourPurchaseDetailsController.onPageLoad()
      }

      "must go from a page that doesn't exist in the route map to Index" in {
        case object UnknownPage extends Page
        navigator.nextPage(UnknownPage, NormalMode, userAnswers) mustBe controllers.routes.IndexController.onPageLoad()
      }

      "must go from SadReferenceNumberPage to ImportCurrencyController when the country requires currency selection" in {
        val answers = userAnswers.set(RefundingCountryPage, "EE").success.value

        navigator.nextPage(SadReferenceNumberPage, NormalMode, answers) mustBe
          importRoutes.ImportCurrencyController.onPageLoad(NormalMode)
      }

      "must skip ImportCurrencyController from SadReferenceNumberPage when the country has one currency" in {
        val answers = userAnswers.set(RefundingCountryPage, "AT").success.value

        navigator.nextPage(SadReferenceNumberPage, NormalMode, answers) mustBe
          controllers.routes.JourneyRecoveryController.onPageLoad()
      }

      "must go from SadReferenceNumberPage to JourneyRecoveryController when no country is in session" in {
        navigator.nextPage(SadReferenceNumberPage, NormalMode, userAnswers) mustBe
          controllers.routes.JourneyRecoveryController.onPageLoad()
      }

      "must go from ImportDetailsInfoPage to ImportCurrencyController when the country requires currency selection" in {
        val answers = userAnswers.set(RefundingCountryPage, "EE").success.value

        navigator.nextPage(ImportDetailsInfoPage, NormalMode, answers) mustBe
          importRoutes.ImportCurrencyController.onPageLoad(NormalMode)
      }

      "must skip ImportCurrencyController from ImportDetailsInfoPage when the country has one currency" in {
        val answers = userAnswers.set(RefundingCountryPage, "AT").success.value

        navigator.nextPage(ImportDetailsInfoPage, NormalMode, answers) mustBe
          controllers.routes.JourneyRecoveryController.onPageLoad()
      }

      "must go from ImportCurrencyPage to JourneyRecoveryController" in {
        navigator.nextPage(ImportCurrencyPage, NormalMode, userAnswers) mustBe
          controllers.routes.JourneyRecoveryController.onPageLoad()
      }
    }

    "in Check mode" - {
      "must go from RefundPeriodPage to CheckYourClaimDetailsController" in {
        navigator.nextPage(pages.RefundPeriodPage, CheckMode, userAnswers) mustBe
          claimRoutes.CheckYourClaimDetailsController.onPageLoad()
      }

      "must go from ContactDetailsPage to CheckYourClaimDetailsController" in {
        navigator.nextPage(ContactDetailsPage, CheckMode, userAnswers) mustBe
          claimRoutes.CheckYourClaimDetailsController.onPageLoad()
      }

      "must go from BusinessActivityCodeThreePage to BusinessActivityThreeController" in {
        navigator.nextPage(BusinessActivityCodeThreePage, CheckMode, userAnswers) mustBe
          claimRoutes.BusinessActivityThreeController.onPageLoad()
      }

      "must go from DescribeItemsOnInvoicePage to CheckYourPurchaseDetailsController" in {
        navigator.nextPage(DescribeItemsOnInvoicePage, CheckMode, userAnswers) mustBe
          purchaseRoutes.CheckYourPurchaseDetailsController.onPageLoad()
      }

      "must go from InvoiceTypePage to InvoiceNumberController" in {
        navigator.nextPage(InvoiceTypePage, CheckMode, userAnswers) mustBe
          purchaseRoutes.InvoiceNumberController.onPageLoad(CheckMode)
      }

      "must go from InvoiceDatePage to SuppliersNameController" in {
        navigator.nextPage(InvoiceDatePage, CheckMode, userAnswers) mustBe
          purchaseRoutes.SuppliersNameController.onPageLoad(CheckMode)
      }

      "must go from SuppliersNamePage to SupplierAddressController" in {
        navigator.nextPage(SuppliersNamePage, CheckMode, userAnswers) mustBe purchaseRoutes.SupplierAddressController.onPageLoad(CheckMode)
      }

      "must go from TotalPurchaseAmountBeforeVatPage to TotalVatPaidController" in {
        navigator.nextPage(TotalPurchaseAmountBeforeVatPage, CheckMode, userAnswers) mustBe
          purchaseRoutes.TotalVatPaidController.onPageLoad(CheckMode)
      }

      "must go from TotalVatPaidPage to TotalVatClaimController" in {
        navigator.nextPage(TotalVatPaidPage, CheckMode, userAnswers) mustBe
          purchaseRoutes.TotalVatClaimController.onPageLoad(CheckMode)
      }

      "must go from TotalVatClaimPage to CheckYourPurchaseDetailsController" in {
        navigator.nextPage(TotalVatClaimPage, CheckMode, userAnswers) mustBe
          purchaseRoutes.CheckYourPurchaseDetailsController.onPageLoad()
      }

      "must go from SadReferenceNumberPage to ImportCurrencyController when the country requires currency selection" in {
        val answers = userAnswers.set(RefundingCountryPage, "EE").success.value

        navigator.nextPage(SadReferenceNumberPage, CheckMode, answers) mustBe
          importRoutes.ImportCurrencyController.onPageLoad(CheckMode)
      }

      "must go from ImportCurrencyPage to JourneyRecoveryController" in {
        navigator.nextPage(ImportCurrencyPage, CheckMode, userAnswers) mustBe
          controllers.routes.JourneyRecoveryController.onPageLoad()
      }

      "must go from ImportDetailsInfoPage to ImportCurrencyController when the country requires currency selection" in {
        val answers = userAnswers.set(RefundingCountryPage, "EE").success.value

        navigator.nextPage(ImportDetailsInfoPage, CheckMode, answers) mustBe
          importRoutes.ImportCurrencyController.onPageLoad(CheckMode)
      }

      "must go from a page that doesn't exist in the edit route map to IndexController" in {
        case object UnknownPage extends Page
        navigator.nextPage(UnknownPage, CheckMode, userAnswers) mustBe controllers.routes.IndexController.onPageLoad()
      }
    }
  }

}
