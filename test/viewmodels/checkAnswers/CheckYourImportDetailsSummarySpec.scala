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

package viewmodels.checkAnswers

import base.SpecBase
import controllers.imports.routes
import models.{CheckMode, Fuel, ImportSupplierAddress, Other}
import pages.*
import pages.imports.ImportSupplierAddressPage
import play.api.Configuration
import play.api.i18n.Messages
import play.api.test.Helpers.running
import utils.{ConfigPurchaseOrImportMapping, CurrencyConfig}

import java.time.LocalDate

class CheckYourImportDetailsSummarySpec extends SpecBase {

  private val countries = Map("IN" -> "INDIA")

  private def withMessages(test: Messages => Unit): Unit = {
    val application = applicationBuilder().build()

    running(application) {
      test(messages(application))
    }
  }

  private def mapping =
    new ConfigPurchaseOrImportMapping(
      Configuration(
        "purchase-or-import-mapping" -> Map(
          "EE" -> Seq(
            "fuel|1.1|sub.fuel.1",
            "fuel|1.1.1|sub.fuel.1.1"
          )
        )
      )
    )

  private def currencies =
    new CurrencyConfig(
      Configuration(
        "currency.mapping" -> Map(
          "EE" -> Seq("euro|EUR|€", "kroon|EEK|kr"),
          "DE" -> Seq("euro|EUR|€")
        )
      )
    )

  private def countryAnswers =
    emptyUserAnswers.set(RefundingCountryPage, "EE").success.value

  private def missing(
                       value: CheckYourImportDetailsSummary.Row
                     )(implicit msgs: Messages): Unit = {
    value._2 mustBe Some(msgs("site.notProvided"))
    value._3.head._2 mustBe "site.add"
  }

  "CheckYourImportDetailsSummary" - {

    "must show the formatted import date and its Change link" in {
      withMessages { implicit msgs =>
        val answers = emptyUserAnswers
          .set(ImportDatePage, LocalDate.of(2026, 10, 1))
          .success.value

        val result =
          CheckYourImportDetailsSummary.rowImportDate(answers)

        result._1 mustBe "Import date"
        result._2 mustBe Some("1 October 2026")
        result._3 mustBe Seq(
          (
            routes.ImportDateController.onPageLoad(CheckMode).url,
            "site.change",
            "checkYourImportDetails.importDate.change.hidden"
          )
        )
      }
    }

    "must retain a missing import date as an Add row" in {
      withMessages { implicit msgs =>
        missing(
          CheckYourImportDetailsSummary.rowImportDate(emptyUserAnswers)
        )
      }
    }

    "must show the supplier name with a Change action" in {
      withMessages { implicit msgs =>
        val answers = emptyUserAnswers
          .set(ImportSuppliersNamePage, "Example Supplier Ltd")
          .success.value

        val result =
          CheckYourImportDetailsSummary.rowSupplierName(answers)

        result._1 mustBe "Name"
        result._2 mustBe Some("Example Supplier Ltd")
        result._3.head._2 mustBe "site.change"
      }
    }

    "must use Add for missing or blank supplier names" in {
      withMessages { implicit msgs =>
        val blank = emptyUserAnswers
          .set(ImportSuppliersNamePage, "   ")
          .success.value

        Seq(emptyUserAnswers, blank).foreach { answers =>
          missing(CheckYourImportDetailsSummary.rowSupplierName(answers))
        }
      }
    }

    "must display populated address lines and the country name" in {
      withMessages { implicit msgs =>
        val answers = emptyUserAnswers
          .set(
            ImportSupplierAddressPage,
            ImportSupplierAddress(
              "10 Example Street",
              None,
              Some("Prayagraj"),
              "IN"
            )
          )
          .success.value

        val result = CheckYourImportDetailsSummary
          .rowSupplierAddress(answers, countries)

        result._2 mustBe Some("10 Example Street\nPrayagraj\nINDIA")
        result._3.head._2 mustBe "site.change"
      }
    }

    "must use Add for a missing or incomplete address" in {
      withMessages { implicit msgs =>
        val incomplete = emptyUserAnswers
          .set(
            ImportSupplierAddressPage,
            ImportSupplierAddress(" ", None, None, "IN")
          )
          .success.value

        Seq(emptyUserAnswers, incomplete).foreach { answers =>
          missing(
            CheckYourImportDetailsSummary
              .rowSupplierAddress(answers, countries)
          )
        }
      }
    }

    "must show the SAD reference only when SAD is Yes" in {
      withMessages { implicit msgs =>
        val answers = emptyUserAnswers
          .set(SadReferenceCheckPage, true)
          .flatMap(_.set(SadReferenceNumberPage, "SAD123"))
          .flatMap(_.set(ImportDetailsInfoPage, "Stale document information"))
          .success.value

        val rows = CheckYourImportDetailsSummary.documentRows(answers)

        rows.map(_._1) mustBe Seq(
          "Single Administrative Document (SAD)",
          "SAD reference number",
          "Import date"
        )

        rows(1)._2 mustBe Some("SAD123")
      }
    }

    "must show document information instead of the SAD reference for No" in {
      withMessages { implicit msgs =>
        val answers = emptyUserAnswers
          .set(SadReferenceCheckPage, false)
          .flatMap(_.set(ImportDetailsInfoPage, "Import document 123"))
          .flatMap(_.set(SadReferenceNumberPage, "Stale reference"))
          .success.value

        val rows = CheckYourImportDetailsSummary.documentRows(answers)

        rows.map(_._1) mustBe Seq(
          "Single Administrative Document (SAD)",
          "Import document information",
          "Import date"
        )

        rows(1)._2 mustBe Some("Import document 123")
      }
    }

    "must show Add for a missing conditional SAD reference" in {
      withMessages { implicit msgs =>
        val answers = emptyUserAnswers
          .set(SadReferenceCheckPage, true)
          .success.value


        val rows = CheckYourImportDetailsSummary.documentRows(answers)
        missing(rows(1))
      }
    }

    "must display applicable dynamic category rows even when answers are missing" in {
      withMessages { implicit msgs =>
        val answers = countryAnswers
          .set(ImportTypePage, Fuel)
          .flatMap(_.set(ImportSubCodePage, "1.1"))
          .success.value

        val rows =
          CheckYourImportDetailsSummary.categoryRows(answers, mapping)

        rows.map(_._1) mustBe Seq("Import type", "Fuel use", "Fuel type")
        missing(rows.last)
      }
    }

    "must show Add for a missing applicable sub-code" in {
      withMessages { implicit msgs =>
        val answers = countryAnswers
          .set(ImportTypePage, Fuel)
          .success.value

        val rows =
          CheckYourImportDetailsSummary.categoryRows(answers, mapping)

        rows.map(_._1) mustBe Seq("Import type", "Fuel use")
        missing(rows.last)
      }
    }

    "must show the complete description for Other without sub-codes" in {
      withMessages { implicit msgs =>
        val description = "First item\nSecond item"

        val answers = countryAnswers
          .set(ImportTypePage, Other)
          .flatMap(_.set(DescribeItemsOnImportDocPage, description))
          .success.value

        val rows =
          CheckYourImportDetailsSummary.categoryRows(answers, mapping)

        rows.map(_._1) mustBe Seq("Import type", "Import description")
        rows.last._2 mustBe Some(description)
      }
    }

    "must ignore a stale description for a coded import" in {
      withMessages { implicit msgs =>
        val answers = countryAnswers
          .set(ImportTypePage, Fuel)
          .flatMap(_.set(DescribeItemsOnImportDocPage, "Old description"))
          .success.value

        CheckYourImportDetailsSummary
          .categoryRows(answers, mapping)
          .map(_._1) must not contain "Import description"
      }
    }

    "must show currency only for a country with multiple currencies" in {
      withMessages { implicit msgs =>
        val eeRows =
          CheckYourImportDetailsSummary.amountRows(countryAnswers, currencies)

        eeRows.head._1 mustBe "Currency"
        missing(eeRows.head)

        val deAnswers = emptyUserAnswers
          .set(RefundingCountryPage, "DE")
          .success.value

        CheckYourImportDetailsSummary
          .amountRows(deAnswers, currencies)
          .map(_._1) mustBe Seq(
          "Amount without VAT",
          "VAT paid",
          "VAT claim"
        )
      }
    }

    "must use the import currency and retain two decimal places" in {
      withMessages { implicit msgs =>
        val answers = countryAnswers
          .set(ImportCurrencyPage, "EUR")
          .flatMap(_.set(TotalAmountWithoutVatPage, BigDecimal("100")))
          .flatMap(_.set(TotalVatPaidImportPage, BigDecimal("20.5")))
          .flatMap(_.set(ImportTotalVatClaimPage, BigDecimal("10")))
          .success.value

        val rows =
          CheckYourImportDetailsSummary.amountRows(answers, currencies)

        rows.drop(1).map(_._2) mustBe Seq(
          Some("€100.00"),
          Some("€20.50"),
          Some("€10.00")
        )
      }
    }

    "must show all four sections with mandatory missing rows retained" in {
      withMessages { implicit msgs =>
        val result = CheckYourImportDetailsSummary.sections(
          emptyUserAnswers,
          mapping,
          currencies,
          countries
        )

        result.map(_._1) mustBe Seq(
          "checkYourImportDetails.importCategory",
          "checkYourImportDetails.importDocumentDetails",
          "checkYourImportDetails.supplierDetails",
          "checkYourImportDetails.importAmounts"
        )

        result.flatMap(_._2).foreach(missing)
      }
    }
  }
}
