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
import controllers.purchase.routes as purchaseRoutes
import models.*
import pages.*
import play.api.Configuration
import utils.{ConfigPurchaseOrImportMapping, CurrencyConfig}

class PurchaseNavigatorSpec extends SpecBase {

  val navigator = new PurchaseNavigator(
    new CurrencyConfig(
      Configuration(
        ConfigFactory.parseString("""
          currency.mapping {
            BG = ["bulgarianLev|BGN|лв"]
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
  val userAnswers: UserAnswers = emptyUserAnswers

  "Navigator" - {

    "in Normal mode" - {
      "must go from PurchaseTypePage to DescribeItemsOnInvoiceController" in {
        val ua = userAnswers.set(PurchaseTypePage, PurchaseOrImportType.values.head).success.value
        navigator.navigateFromPurchaseTypePage(NormalMode)(ua) mustBe
          purchaseRoutes.DescribeItemsOnInvoiceController.onPageLoad(NormalMode)
      }

      "must go from PurchaseTypePage to JourneyRecoveryController if no answer is present" in {
        navigator.navigateFromPurchaseTypePage(NormalMode)(userAnswers) mustBe
          controllers.routes.JourneyRecoveryController.onPageLoad()
      }

      "must go from PurchaseTypePage to PurchaseSubTypeController when mapping exists for country" in {
        val fakePurchaseConfig = new ConfigPurchaseOrImportMapping() {
          override def subcodesFor(country: String, parentKey: String): Seq[(String, String)] =
            if (country == "AT" && parentKey == Fuel.toString) Seq(("1", "purchase.sub.fuel.1")) else Seq.empty
        }

        val nav = new PurchaseNavigator(
          new CurrencyConfig(Configuration(ConfigFactory.parseString("""currency.mapping = {}"""))),
          fakePurchaseConfig
        )

        val ua = userAnswers.set(RefundingCountryPage, "AT").success.value.set(PurchaseTypePage, Fuel).success.value
        nav.navigateFromPurchaseTypePage(NormalMode)(ua) mustBe
          play.api.mvc.Call("GET", s"/${PurchaseOrImportType.urlSlugForPurchaseType(Fuel)}")
      }

      "must go from PurchaseTypePage to InvoiceTypeController when mapping is empty for country" in {
        val fakePurchaseConfig: ConfigPurchaseOrImportMapping = new ConfigPurchaseOrImportMapping() {
          override def subcodesFor(country: String, parentKey: String): Seq[Nothing] = Seq.empty
        }

        val nav = new PurchaseNavigator(
          new CurrencyConfig(Configuration(ConfigFactory.parseString("""currency.mapping = {}"""))),
          fakePurchaseConfig
        )

        val ua = userAnswers.set(RefundingCountryPage, "AT").success.value.set(PurchaseTypePage, Fuel).success.value
        nav.navigateFromPurchaseTypePage(NormalMode)(ua) mustBe
          purchaseRoutes.InvoiceTypeController.onPageLoad(NormalMode)
      }

      "must go from PurchaseTypePage to PurchaseSubTypeController when country stored as name-only string is used" in {
        val fakePurchaseConfig = new ConfigPurchaseOrImportMapping() {
          override def subcodesFor(country: String, parentKey: String): Seq[(String, String)] =
            if (country == "Austria" && parentKey == Fuel.toString) Seq(("1", "purchase.sub.fuel.1")) else Seq.empty
        }

        val nav = new PurchaseNavigator(
          new CurrencyConfig(Configuration(ConfigFactory.parseString("""currency.mapping = {}"""))),
          fakePurchaseConfig
        )

        val ua = userAnswers.set(RefundingCountryNamePage, "Austria").success.value.set(PurchaseTypePage, Fuel).success.value
        nav.navigateFromPurchaseTypePage(NormalMode)(ua) mustBe
          play.api.mvc.Call("GET", s"/${PurchaseOrImportType.urlSlugForPurchaseType(Fuel)}")
      }

      "must go from PurchaseTypePage to InvoiceTypeController when country stored as name-only and mapping empty" in {
        val fakePurchaseConfig: ConfigPurchaseOrImportMapping = new ConfigPurchaseOrImportMapping() {
          override def subcodesFor(country: String, parentKey: String): Seq[Nothing] = Seq.empty
        }

        val nav = new PurchaseNavigator(
          new CurrencyConfig(Configuration(ConfigFactory.parseString("""currency.mapping = {}"""))),
          fakePurchaseConfig
        )

        val ua = userAnswers.set(RefundingCountryNamePage, "Austria").success.value.set(PurchaseTypePage, Fuel).success.value
        nav.navigateFromPurchaseTypePage(NormalMode)(ua) mustBe
          purchaseRoutes.InvoiceTypeController.onPageLoad(NormalMode)
      }

      "must go from PurchaseSubCategoryPage to InvoiceTypeController when PurchaseType is Other and subcategory ends with 99" in {
        val ua = userAnswers.set(PurchaseTypePage, Other).success.value.set(PurchaseSubCategoryPage, "1.99").success.value
        navigator.navigateFromPurchaseSubCategoryPage(NormalMode, ua) mustBe
          purchaseRoutes.InvoiceTypeController.onPageLoad(NormalMode)
      }

      "must go from PurchaseSubCategoryPage to InvoiceTypeController when PurchaseType is not Other" in {
        val ua = userAnswers.set(PurchaseTypePage, Fuel).success.value.set(PurchaseSubCategoryPage, "1").success.value
        navigator.navigateFromPurchaseSubCategoryPage(NormalMode, ua) mustBe
          purchaseRoutes.InvoiceTypeController.onPageLoad(NormalMode)
      }

      "must go from InvoiceNumberPage to InvoiceDateController in normal flow (no warning marker)" in {
        val ua = userAnswers.remove(SupplierVatRegistrationWarningPage).success.value
        navigator.navigateFromInvoiceNumberPage(NormalMode)(ua) mustBe
          purchaseRoutes.InvoiceDateController.onPageLoad(NormalMode)
      }

      "must go from SupplierAddressPage to SupplierTaxNumberController if country is Germany" in {
        val ua = userAnswers.set(RefundingCountryPage, "DE").success.value
        navigator.navigateFromSupplierAddressPage(NormalMode)(ua) mustBe purchaseRoutes.SupplierTaxNumberController.onPageLoad(NormalMode)
      }

      "must go from SupplierAddressPage to SimplifiedInvoiceVatRegCheckController if country is not Germany and invoice type is simplified" in {
        val ua = userAnswers.set(RefundingCountryPage, "FR").success.value.set(InvoiceTypePage, InvoiceType.SimplifiedInvoice).success.value
        navigator.navigateFromSupplierAddressPage(NormalMode)(ua) mustBe purchaseRoutes.SimplifiedInvoiceVatRegCheckController.onPageLoad(NormalMode)
      }

      "must go from SupplierAddressPage to SupplierVatRegistrationNumberController if country is not Germany and invoice type is standard" in {
        val ua = userAnswers.set(RefundingCountryPage, "FR").success.value.set(InvoiceTypePage, InvoiceType.StandardInvoice).success.value
        navigator.navigateFromSupplierAddressPage(NormalMode)(ua) mustBe purchaseRoutes.SupplierVatRegistrationNumberController.onPageLoad(NormalMode)
      }

      "must go from SimplifiedInvoiceVatRegCheckPage to RefundingCurrencyController if no selected and the country has more than one currency" in {
        val ua = userAnswers
          .set(pages.SimplifiedInvoiceVatRegCheckPage, false)
          .success
          .value
          .set(pages.RefundingCountryPage, "EE")
          .success
          .value
        navigator.navigateFromSimplifiedInvoiceVatRegCheckPage(NormalMode)(ua) mustBe
          purchaseRoutes.RefundingCurrencyController.onPageLoad(NormalMode)
      }

      "must go from SimplifiedInvoiceVatRegCheckPage to TotalPurchaseAmountBeforeVatController if no selected" in {
        val ua = userAnswers
          .set(SimplifiedInvoiceVatRegCheckPage, false)
          .success
          .value
          .set(pages.RefundingCountryPage, "AT")
          .success
          .value
        navigator.navigateFromSimplifiedInvoiceVatRegCheckPage(NormalMode)(ua) mustBe
          purchaseRoutes.TotalPurchaseAmountBeforeVatController.onPageLoad(NormalMode)
      }

      "must go from SimplifiedInvoiceVatRegCheckPage to SupplierVatRegistrationNumberController if yes selected" in {
        val ua = userAnswers.set(SimplifiedInvoiceVatRegCheckPage, true).success.value
        navigator.navigateFromSimplifiedInvoiceVatRegCheckPage(NormalMode)(ua) mustBe
          purchaseRoutes.SupplierVatRegistrationNumberController.onPageLoad(NormalMode)
      }

      "must go from SupplierTaxNumberPage to SupplierVatRegistrationController if VAT registration number is selected" in {
        val ua = userAnswers.set(SupplierTaxNumberPage, SupplierTaxNumber.Vatregistrationnumber).success.value
        navigator.navigateFromSupplierTaxNumberPage(NormalMode)(ua) mustBe
          purchaseRoutes.SupplierVatRegistrationNumberController.onPageLoad(NormalMode)
      }

      "must go from SupplierTaxNumberPage to SupplierTaxIdentifierNumberController if tax identifier number is selected" in {
        val ua = userAnswers.set(SupplierTaxNumberPage, SupplierTaxNumber.Taxidentifiernumber).success.value
        navigator.navigateFromSupplierTaxNumberPage(NormalMode)(ua) mustBe
          purchaseRoutes.SupplierTaxIdentifierNumberController.onPageLoad(NormalMode)
      }

      "must go from SupplierTaxNumberPage to TotalPurchaseAmountBeforeVatController if neither is selected" in {
        val ua = userAnswers.set(SupplierTaxNumberPage, SupplierTaxNumber.Neither).success.value
        navigator.navigateFromSupplierTaxNumberPage(NormalMode)(ua) mustBe
          purchaseRoutes.TotalPurchaseAmountBeforeVatController.onPageLoad(NormalMode)
      }

      "must go from SupplierTaxNumberPage to JourneyRecoveryController if no answer is present" in {
        navigator.navigateFromSupplierTaxNumberPage(NormalMode)(userAnswers) mustBe
          controllers.routes.JourneyRecoveryController.onPageLoad()
      }

      "must go from SupplierVatRegistrationNumberPage to TotalPurchaseAmountBeforeVatController" in {
        val ua = userAnswers.set(RefundingCountryPage, "AT").success.value
        navigator.navigateFromSupplierVatRegistrationPage()(ua) mustBe
          purchaseRoutes.TotalPurchaseAmountBeforeVatController.onPageLoad(NormalMode)
      }

      "must go from SupplierVatRegistrationNumberPage to RefundingCurrencyController when country has more than one currency" in {
        val ua = userAnswers.set(RefundingCountryPage, "EE").success.value
        navigator.navigateFromSupplierVatRegistrationPage()(ua) mustBe
          purchaseRoutes.RefundingCurrencyController.onPageLoad(NormalMode)
      }

      "must go from navigateFromSupplierTaxIdentifierNumberPage to TotalPurchaseAmountBeforeVatController" in {
        val ua = userAnswers.set(RefundingCountryPage, "AT").success.value
        navigator.navigateFromSupplierTaxIdentifierNumberPage()(ua) mustBe
          purchaseRoutes.TotalPurchaseAmountBeforeVatController.onPageLoad(NormalMode)
      }

      "must go from RefundingCurrencyPage to TotalPurchaseAmountBeforeVatController" in {
        navigator.navigateFromRefundingCurrencyPage(NormalMode)(userAnswers) mustBe
          purchaseRoutes.TotalPurchaseAmountBeforeVatController.onPageLoad(NormalMode)
      }
    }

    "in Check mode" - {
      "must go from PurchaseTypePage to DescribeItemsOnInvoiceController" in {
        val ua = userAnswers.set(PurchaseTypePage, PurchaseOrImportType.values.head).success.value
        navigator.navigateFromPurchaseTypePage(CheckMode)(ua) mustBe
          purchaseRoutes.DescribeItemsOnInvoiceController.onPageLoad(CheckMode)
      }

      "must go from PurchaseTypePage to JourneyRecoveryController if no answer is present" in {
        navigator.navigateFromPurchaseTypePage(CheckMode)(userAnswers) mustBe
          controllers.routes.JourneyRecoveryController.onPageLoad()
      }

      "must go from PurchaseTypePage to PurchaseSubTypeController when mapping exists for country" in {
        val fakePurchaseConfig = new ConfigPurchaseOrImportMapping() {
          override def subcodesFor(country: String, parentKey: String): Seq[(String, String)] =
            if (country == "AT" && parentKey == Fuel.toString) Seq(("1", "purchase.sub.fuel.1")) else Seq.empty
        }

        val nav = new PurchaseNavigator(
          new CurrencyConfig(Configuration(ConfigFactory.parseString("""currency.mapping = {}"""))),
          fakePurchaseConfig
        )

        val ua = userAnswers.set(RefundingCountryPage, "AT").success.value.set(PurchaseTypePage, Fuel).success.value
        nav.navigateFromPurchaseTypePage(CheckMode)(ua) mustBe
          play.api.mvc.Call("GET", s"/${PurchaseOrImportType.urlSlugForPurchaseType(Fuel)}")
      }

      "must go from PurchaseTypePage to CheckYourPurchaseDetailsController when mapping is empty for country" in {
        val fakePurchaseConfig: ConfigPurchaseOrImportMapping = new ConfigPurchaseOrImportMapping() {
          override def subcodesFor(country: String, parentKey: String): Seq[Nothing] = Seq.empty
        }

        val nav = new PurchaseNavigator(
          new CurrencyConfig(Configuration(ConfigFactory.parseString("""currency.mapping = {}"""))),
          fakePurchaseConfig
        )

        val ua = userAnswers.set(RefundingCountryPage, "AT").success.value.set(PurchaseTypePage, Fuel).success.value
        nav.navigateFromPurchaseTypePage(CheckMode)(ua) mustBe
          purchaseRoutes.CheckYourPurchaseDetailsController.onPageLoad()
      }

      "must go from PurchaseSubCategoryPage to InvoiceTypeController when PurchaseType is Other and subcategory ends with 99" in {
        val ua = userAnswers.set(PurchaseTypePage, Other).success.value.set(PurchaseSubCategoryPage, "1.99").success.value
        navigator.navigateFromPurchaseSubCategoryPage(CheckMode, ua) mustBe
          purchaseRoutes.CheckYourPurchaseDetailsController.onPageLoad()
      }

      "must go from InvoiceNumberPage to SupplierTaxIdentifierNumberController" in {
        val ua = userAnswers.set(SupplierTaxIdentifierNumberPage, "456").success.value
        navigator.navigateFromInvoiceNumberPage(CheckMode)(ua) mustBe
          purchaseRoutes.SupplierTaxIdentifierNumberController.onPageLoad(CheckMode)
      }

      "must go from InvoiceNumberPage to the supplier VRN number page when marker is true" in {
        val ua = userAnswers.set(SupplierVatRegistrationNumberPage, "123").success.value
        navigator.navigateFromInvoiceNumberPage(CheckMode)(ua) mustBe
          purchaseRoutes.SupplierVatRegistrationNumberController.onPageLoad(CheckMode)
      }

      "must go from InvoiceNumberPage to CYA purchase page" in {
        navigator.navigateFromInvoiceNumberPage(CheckMode)(userAnswers) mustBe
          purchaseRoutes.CheckYourPurchaseDetailsController.onPageLoad()
      }

      "must go from SupplierAddressPage to SupplierTaxNumberController in CheckMode if country is Germany" in {
        val ua = userAnswers.set(RefundingCountryPage, "DE").success.value
        navigator.navigateFromSupplierAddressPage(CheckMode)(ua) mustBe purchaseRoutes.SupplierTaxNumberController.onPageLoad(CheckMode)
      }

      "must go from SupplierAddressPage to SimplifiedInvoiceVatRegCheckController in CheckMode if country is not Germany and simplified invoice" in {
        val ua = userAnswers
          .set(RefundingCountryPage, "AT")
          .success
          .value
          .set(InvoiceTypePage, InvoiceType.SimplifiedInvoice)
          .success
          .value
        navigator.navigateFromSupplierAddressPage(CheckMode)(ua) mustBe purchaseRoutes.SimplifiedInvoiceVatRegCheckController.onPageLoad(CheckMode)
      }

      "must go from SupplierAddressPage to SupplierVatRegistrationNumberController in CheckMode when country is not DE and invoice type is standard" in {
        val ua = userAnswers.set(RefundingCountryPage, "FR").success.value.set(InvoiceTypePage, InvoiceType.StandardInvoice).success.value
        navigator.navigateFromSupplierAddressPage(CheckMode)(ua) mustBe purchaseRoutes.SupplierVatRegistrationNumberController.onPageLoad(CheckMode)
      }

      "must go from SupplierAddressPage to SimplifiedInvoiceVatRegCheckController if country is missing and no invoice type" in {
        navigator.navigateFromSupplierAddressPage(CheckMode)(userAnswers) mustBe
          purchaseRoutes.SimplifiedInvoiceVatRegCheckController.onPageLoad(CheckMode)
      }

      "must go from SupplierAddressPage to SupplierVatRegistrationNumberController in CheckMode if country is not Germany and standard invoice" in {
        val ua = userAnswers
          .set(RefundingCountryPage, "AT")
          .success
          .value
          .set(InvoiceTypePage, InvoiceType.StandardInvoice)
          .success
          .value
        navigator.navigateFromSupplierAddressPage(CheckMode)(ua) mustBe purchaseRoutes.SupplierVatRegistrationNumberController.onPageLoad(CheckMode)
      }

      "must go from SimplifiedInvoiceVatRegCheckPage to RefundingCurrencyController in CheckMode if no selected and the country has more than one currency" in {
        val ua = userAnswers
          .set(pages.SimplifiedInvoiceVatRegCheckPage, false)
          .success
          .value
          .set(pages.RefundingCountryPage, "EE")
          .success
          .value
        navigator.navigateFromSimplifiedInvoiceVatRegCheckPage(CheckMode)(ua) mustBe
          purchaseRoutes.RefundingCurrencyController.onPageLoad(CheckMode)
      }

      "must go from SimplifiedInvoiceVatRegCheckPage to TotalPurchaseAmountBeforeVatController if no selected" in {
        val ua = userAnswers.set(SimplifiedInvoiceVatRegCheckPage, false).success.value
        navigator.navigateFromSimplifiedInvoiceVatRegCheckPage(CheckMode)(ua) mustBe
          purchaseRoutes.TotalPurchaseAmountBeforeVatController.onPageLoad(CheckMode)
      }

      "must go from SupplierTaxNumberPage to SupplierVatRegistrationNumberController if VAT registration number is selected" in {
        val ua = userAnswers.set(SupplierTaxNumberPage, SupplierTaxNumber.Vatregistrationnumber).success.value
        navigator.navigateFromSupplierTaxNumberPage(CheckMode)(ua) mustBe
          purchaseRoutes.SupplierVatRegistrationNumberController.onPageLoad(CheckMode)
      }

      "must go from SupplierTaxNumberPage to SupplierTaxIdentifierNumberController if tax identifier number is selected" in {
        val ua = userAnswers.set(SupplierTaxNumberPage, SupplierTaxNumber.Taxidentifiernumber).success.value
        navigator.navigateFromSupplierTaxNumberPage(CheckMode)(ua) mustBe
          purchaseRoutes.SupplierTaxIdentifierNumberController.onPageLoad(CheckMode)
      }

      "must go from SupplierTaxNumberPage to JourneyRecoveryController in CheckMode when no answer present" in {
        navigator.navigateFromSupplierTaxNumberPage(CheckMode)(userAnswers) mustBe
          controllers.routes.JourneyRecoveryController.onPageLoad()
      }

      "must go from SupplierVatRegistrationNumberPage to CheckYourPurchaseDetailsController" in {
        val ua = userAnswers.set(TotalPurchaseAmountBeforeVatPage, 123).success.value
        navigator.navigateFromSupplierVatRegistrationPage()(ua) mustBe
          purchaseRoutes.CheckYourPurchaseDetailsController.onPageLoad()
      }

      "must go from navigateFromSupplierTaxIdentifierNumberPage to CheckYourPurchaseDetailsController" in {
        val ua = userAnswers.set(TotalPurchaseAmountBeforeVatPage, 123).success.value
        navigator.navigateFromSupplierTaxIdentifierNumberPage()(ua) mustBe
          purchaseRoutes.CheckYourPurchaseDetailsController.onPageLoad()
      }

      "must go from RefundingCurrencyPage to RefundPeriodController in CheckMode if CountryChangedPage is true" in {
        val ua = userAnswers.set(pages.CountryChangedPage, true).success.value
        navigator.navigateFromRefundingCurrencyPage(CheckMode)(ua) mustBe
          purchaseRoutes.CheckYourPurchaseDetailsController.onPageLoad()
      }

      "must go from RefundingCurrencyPage to CheckYourPurchaseDetailsController in CheckMode if CountryChangedPage is not set" in {
        navigator.navigateFromRefundingCurrencyPage(CheckMode)(userAnswers) mustBe
          purchaseRoutes.CheckYourPurchaseDetailsController.onPageLoad()
      }

      "must go from RefundingCurrencyPage to TotalPurchaseAmountBeforeVatController in CheckMode when country is EE and currency changed" in {
        val ua = userAnswers
          .set(pages.RefundingCountryPage, "EE")
          .success
          .value
          .set(pages.CurrencyChangedPage, true)
          .success
          .value

        navigator.navigateFromRefundingCurrencyPage(CheckMode)(ua) mustBe
          purchaseRoutes.TotalPurchaseAmountBeforeVatController.onPageLoad(CheckMode)
      }
    }

  }
}
