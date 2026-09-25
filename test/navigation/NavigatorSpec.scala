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
import controllers.imports.routes as importRoutes
import controllers.purchase.routes as purchaseRoutes
import models.*
import models.PurchaseOrImport.{Import, Purchase}
import pages.*
import play.api.Configuration
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

      "must go from PurchaseOrImportPage to ImportTypeController when Import is selected" in {
        val answers = userAnswers.set(PurchaseOrImportPage, Import).success.value

        navigator.nextPage(PurchaseOrImportPage, NormalMode, answers) mustBe
          importRoutes.ImportTypeController.onPageLoad(NormalMode)
      }

      "must go from PurchaseOrImportPage to Journey Recovery when nothing is selected" in {
        navigator.nextPage(PurchaseOrImportPage, NormalMode, userAnswers) mustBe
          controllers.routes.JourneyRecoveryController.onPageLoad()
      }

      "must go from SadReferenceCheckPage to SadReferenceNumberController when the user has a SAD reference" in {
        val answers = userAnswers.set(SadReferenceCheckPage, true).success.value

        navigator.nextPage(SadReferenceCheckPage, NormalMode, answers) mustBe
          importRoutes.SadReferenceNumberController.onPageLoad(NormalMode)
      }

      "must go from SadReferenceCheckPage to ImportDetailsInfoController when the user has no SAD reference" in {
        val answers = userAnswers.set(SadReferenceCheckPage, false).success.value

        navigator.nextPage(SadReferenceCheckPage, NormalMode, answers) mustBe
          importRoutes.ImportDetailsInfoController.onPageLoad(NormalMode)
      }

      "must go from SadReferenceCheckPage to Journey Recovery when the question is unanswered" in {
        navigator.nextPage(SadReferenceCheckPage, NormalMode, userAnswers) mustBe
          controllers.routes.JourneyRecoveryController.onPageLoad()
      }

      "must go from SadReferenceNumberPage to ImportDetailsInfoController when SadReferenceCheck is not yes" in {
        navigator.nextPage(SadReferenceNumberPage, NormalMode, userAnswers) mustBe
          importRoutes.ImportDetailsInfoController.onPageLoad(NormalMode)
      }

      "must go from SadReferenceNumberPage to ImportSuppliersNameController when SadReferenceCheck is yes" in {
        val answers = userAnswers.set(SadReferenceCheckPage, true).success.value

        navigator.nextPage(SadReferenceNumberPage, NormalMode, answers) mustBe
          importRoutes.ImportSuppliersNameController.onPageLoad(NormalMode)
      }

      "must go from ImportDetailsInfoPage to ImportSuppliersNameController" in {
        navigator.nextPage(ImportDetailsInfoPage, NormalMode, userAnswers) mustBe
          importRoutes.ImportSuppliersNameController.onPageLoad(NormalMode)
      }

      "must go from ImportSuppliersNamePage to ImportCurrencyController when the country requires currency selection" in {
        val answers = userAnswers.set(RefundingCountryPage, "EE").success.value

        navigator.nextPage(ImportSuppliersNamePage, NormalMode, answers) mustBe
          importRoutes.ImportCurrencyController.onPageLoad(NormalMode)
      }

      "must go from ImportSuppliersNamePage to TotalAmountWithoutVatController when the country has one currency" in {
        val answers = userAnswers.set(RefundingCountryPage, "AT").success.value

        navigator.nextPage(ImportSuppliersNamePage, NormalMode, answers) mustBe
          importRoutes.TotalAmountWithoutVatController.onPageLoad(NormalMode)
      }

      "must go from ImportSuppliersNamePage to Journey Recovery when no country is in session" in {
        navigator.nextPage(ImportSuppliersNamePage, NormalMode, userAnswers) mustBe
          controllers.routes.JourneyRecoveryController.onPageLoad()
      }

      "must go from ImportCurrencyPage to TotalAmountWithoutVatController" in {
        navigator.nextPage(ImportCurrencyPage, NormalMode, userAnswers) mustBe
          importRoutes.TotalAmountWithoutVatController.onPageLoad(NormalMode)
      }

      "must go from TotalAmountWithoutVatPage to TotalVatPaidImportController" in {
        navigator.nextPage(TotalAmountWithoutVatPage, NormalMode, userAnswers) mustBe
         importRoutes.TotalVatPaidImportController.onPageLoad(NormalMode)
      }

      "must go from TotalVatPaidImportPage to JourneyRecoveryController" in {
        navigator.nextPage(TotalVatPaidImportPage, NormalMode, userAnswers) mustBe
          controllers.routes.JourneyRecoveryController.onPageLoad()
      }

      "must go from a page that doesn't exist in the route map to Index" in {
        case object UnknownPage extends Page
        navigator.nextPage(UnknownPage, NormalMode, userAnswers) mustBe controllers.routes.IndexController.onPageLoad()
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


      "must go from ImportDetailsInfoPage to ImportDateController" in {
        navigator.nextPage(ImportDetailsInfoPage, CheckMode, userAnswers) mustBe
          purchaseRoutes.ImportDateController.onPageLoad(CheckMode)
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
        navigator.nextPage(SuppliersNamePage, CheckMode, userAnswers) mustBe
          purchaseRoutes.SupplierAddressController.onPageLoad(CheckMode)
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

      "must go from DescribeItemsOnImportDocPage to SadReferenceCheckController" in {
        navigator.nextPage(DescribeItemsOnImportDocPage, CheckMode, userAnswers) mustBe
          importRoutes.SadReferenceCheckController.onPageLoad(CheckMode)
      }

      "must go from SadReferenceCheckPage to SadReferenceNumberController when the user has a SAD reference" in {
        val answers = userAnswers.set(SadReferenceCheckPage, true).success.value

        navigator.nextPage(SadReferenceCheckPage, CheckMode, answers) mustBe
          importRoutes.SadReferenceNumberController.onPageLoad(CheckMode)
      }

      "must go from SadReferenceNumberPage to ImportSuppliersNameController when SadReferenceCheck is yes" in {
        val answers = userAnswers.set(SadReferenceCheckPage, true).success.value

        navigator.nextPage(SadReferenceNumberPage, CheckMode, answers) mustBe
          importRoutes.ImportSuppliersNameController.onPageLoad(CheckMode)
      }

      "must go from ImportDetailsInfoPage to ImportSuppliersNameController" in {
        navigator.nextPage(ImportDetailsInfoPage, CheckMode, userAnswers) mustBe
          importRoutes.ImportSuppliersNameController.onPageLoad(CheckMode)
      }

      "must go from ImportSuppliersNamePage to ImportCurrencyController when the country requires currency selection" in {
        val answers = userAnswers.set(RefundingCountryPage, "EE").success.value

        navigator.nextPage(ImportSuppliersNamePage, CheckMode, answers) mustBe
          importRoutes.ImportCurrencyController.onPageLoad(CheckMode)
      }

      "must go from ImportCurrencyPage to TotalAmountWithoutVatController" in {
        navigator.nextPage(ImportCurrencyPage, CheckMode, userAnswers) mustBe
          importRoutes.TotalAmountWithoutVatController.onPageLoad(CheckMode)
      }

      "must go from TotalAmountWithoutVatPage to TotalVatPaidImportController" in {
        navigator.nextPage(TotalAmountWithoutVatPage, CheckMode, userAnswers) mustBe
          importRoutes.TotalVatPaidImportController.onPageLoad(CheckMode)
      }

      "must go from TotalVatPaidImportPage to JourneyRecoveryController" in {
        navigator.nextPage(TotalVatPaidImportPage, CheckMode, userAnswers) mustBe
          controllers.routes.JourneyRecoveryController.onPageLoad()
      }

      "must go from a page that doesn't exist in the edit route map to IndexController" in {
        case object UnknownPage extends Page
        navigator.nextPage(UnknownPage, CheckMode, userAnswers) mustBe controllers.routes.IndexController.onPageLoad()
      }
    }
  }
}
