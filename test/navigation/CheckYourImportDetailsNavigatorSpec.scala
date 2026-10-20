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
import controllers.imports.routes
import models.{CheckMode, Fuel, ImportSupplierAddress, Other, UserAnswers}
import pages.claim.RefundingCountryPage
import pages.imports.*
import navigation.CheckYourImportDetailsNavigator
import play.api.Configuration
import utils.{ConfigPurchaseOrImportMapping, CurrencyConfig}
import java.time.LocalDate

class CheckYourImportDetailsNavigatorSpec extends SpecBase {
  private val config = new ConfigPurchaseOrImportMapping(Configuration(
    "purchase-or-import-mapping" -> Map("AT" -> Seq("fuel|1.1|sub.fuel.1", "fuel|1.1.1|sub.fuel.1.1"))
  ))
  private val navigator = new CheckYourImportDetailsNavigator(new CurrencyConfig(Configuration(
    "currency.mapping" -> Map("AT" -> Seq("euro|EUR|€"), "EE" -> Seq("euro|EUR|€", "estonianKroon|EEK|kr"))
  )), config)
  private def complete: UserAnswers = emptyUserAnswers.set(RefundingCountryPage, "AT")
    .flatMap(_.set(ImportTypePage, Other)).flatMap(_.set(DescribeItemsOnImportDocPage, "Equipment"))
    .flatMap(_.set(SadReferenceCheckPage, true)).flatMap(_.set(SadReferenceNumberPage, "SAD123"))
    .flatMap(_.set(ImportDatePage, LocalDate.of(2026, 10, 1)))
    .flatMap(_.set(ImportSuppliersNamePage, "Supplier"))
    .flatMap(_.set(ImportSupplierAddressPage, ImportSupplierAddress("10 Street", None, None, "IN")))
    .flatMap(_.set(TotalAmountWithoutVatPage, BigDecimal(100)))
    .flatMap(_.set(TotalVatPaidImportPage, BigDecimal(20)))
    .flatMap(_.set(ImportTotalVatClaimPage, BigDecimal(10))).success.value

  "CheckYourImportDetailsNavigator" - {
    "must accept optional address lines and a single implicit currency" in {
      navigator.firstMissingPage(complete) mustBe None
    }
    "must use Journey Recovery as the temporary complete destination" in {
      navigator.navigateFromCheckYourImportDetails(complete) mustBe controllers.routes.JourneyRecoveryController.onPageLoad()
    }
    "must require category before all other missing answers" in {
      val answers = complete.remove(ImportTypePage).flatMap(_.remove(ImportSuppliersNamePage)).success.value
      navigator.firstMissingPage(answers) mustBe Some(routes.ImportTypeController.onPageLoad(CheckMode))
    }
    "must require an applicable sub-code" in {
      val answers = complete.set(ImportTypePage, Fuel).success.value
      navigator.firstMissingPage(answers) mustBe Some(routes.ImportSubCodeController.onPageLoad("fuel", CheckMode))
    }
    "must require an applicable sub-category" in {
      val answers = complete.set(ImportTypePage, Fuel).flatMap(_.set(ImportSubCodePage, "1.1")).success.value
      navigator.firstMissingPage(answers) mustBe Some(routes.ImportSubCategoryController.onPageLoad(CheckMode))
    }
    "must require Other description without selectable sub-codes" in {
      navigator.firstMissingPage(complete.remove(DescribeItemsOnImportDocPage).success.value) mustBe
        Some(routes.DescribeItemsOnImportDocController.onPageLoad(CheckMode))
    }
    "must require SAD reference for Yes" in {
      navigator.firstMissingPage(complete.remove(SadReferenceNumberPage).success.value) mustBe
        Some(routes.SadReferenceNumberController.onPageLoad(CheckMode))
    }
    "must require document information for No" in {
      navigator.firstMissingPage(complete.set(SadReferenceCheckPage, false).success.value) mustBe
        Some(routes.ImportDetailsInfoController.onPageLoad(CheckMode))
    }
    "must ignore the non-applicable SAD reference" in {
      val answers = complete.set(SadReferenceCheckPage, false).flatMap(_.remove(SadReferenceNumberPage))
        .flatMap(_.set(ImportDetailsInfoPage, "Document 123")).success.value
      navigator.firstMissingPage(answers) mustBe None
    }
    "must require currency only for a multi-currency state" in {
      val answers = complete.set(RefundingCountryPage, "EE").success.value
      navigator.firstMissingPage(answers) mustBe Some(routes.ImportCurrencyController.onPageLoad(CheckMode))
    }
    "must skip completed pages and pick the earliest missing supplier field" in {
      val answers = complete.remove(ImportSuppliersNamePage).flatMap(_.remove(ImportTotalVatClaimPage)).success.value
      navigator.firstMissingPage(answers) mustBe Some(routes.ImportSuppliersNameController.onPageLoad(CheckMode))
    }
    "must require VAT claim after the other steps are complete" in {
      navigator.firstMissingPage(complete.remove(ImportTotalVatClaimPage).success.value) mustBe
        Some(routes.TotalVatClaimController.onPageLoad(CheckMode))
    }
  }
}
