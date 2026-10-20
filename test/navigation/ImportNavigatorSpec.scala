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
import controllers.imports.routes as importRoutes
import models.*
import pages.*
import pages.imports.*
import controllers.imports.routes as importsRoutes
import play.api.Configuration
import utils.{ConfigPurchaseOrImportMapping, CurrencyConfig}

class ImportNavigatorSpec extends SpecBase {

  val navigator = new ImportNavigator(
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

  private def navigatorWith(config: ConfigPurchaseOrImportMapping): ImportNavigator =
    new ImportNavigator(
      new CurrencyConfig(Configuration(ConfigFactory.parseString("""currency.mapping = {}"""))),
      config
    )

  private def answersFor(country: String, importType: PurchaseOrImportType, subCode: String): UserAnswers =
    userAnswers
      .set(RefundingCountryPage, country)
      .success
      .value
      .set(ImportTypePage, importType)
      .success
      .value
      .set(ImportSubCodePage, subCode)
      .success
      .value

  "ImportNavigator" - {

    "in Normal mode" - {

      "must go from ImportTypePage to JourneyRecovery when ImportType present but no country" in {
        val ua = userAnswers.set(ImportTypePage, Fuel).success.value

        navigator.navigateFromImportTypePage(NormalMode)(ua) mustBe
          controllers.routes.JourneyRecoveryController.onPageLoad()
      }

      "must go from ImportTypePage to the import sub code page for that type when the country has sub codes" in {
        val fakeConfig = new ConfigPurchaseOrImportMapping() {
          override def subcodesFor(country: String, parentKey: String): Seq[(String, String)] =
            if (country == "BG" && parentKey == Fuel.toString) {
              Seq(("1.1", "purchase.sub.fuel.1.1"), ("1.1.1", "purchase.sub.fuel.1.1.1"))
            } else {
              Seq.empty
            }
        }
        val ua = userAnswers.set(RefundingCountryPage, "BG").success.value.set(ImportTypePage, Fuel).success.value

        navigatorWith(fakeConfig)
          .navigateFromImportTypePage(NormalMode)(ua) mustBe
          importRoutes.ImportSubCodeController.onPageLoad(Fuel.toString, NormalMode)
      }

      "must go from ImportTypePage to SadReferenceCheck when the country has no sub codes for that type" in {
        val fakeConfig = new ConfigPurchaseOrImportMapping() {
          override def subcodesFor(country: String, parentKey: String): Seq[(String, String)] = Seq.empty
        }
        val ua = userAnswers.set(RefundingCountryPage, "AT").success.value.set(ImportTypePage, Transport).success.value

        navigatorWith(fakeConfig).navigateFromImportTypePage(NormalMode)(ua) mustBe
          importRoutes.SadReferenceCheckController.onPageLoad(NormalMode)
      }

      "must go from ImportTypePage to the free text page when Other has no selectable sub codes" in {
        val fakeConfig = new ConfigPurchaseOrImportMapping() {
          override def subcodesFor(country: String, parentKey: String): Seq[(String, String)] =
            if (parentKey == Other.toString) Seq(("10.99", "sub.other.99")) else Seq.empty
        }
        val ua = userAnswers.set(RefundingCountryPage, "AT").success.value.set(ImportTypePage, Other).success.value

        navigatorWith(fakeConfig).navigateFromImportTypePage(NormalMode)(ua) mustBe
          importRoutes.DescribeItemsOnImportDocController.onPageLoad(NormalMode)
      }

      "must go from ImportSubCodePage to the free text page when Other and None of these was selected" in {
        val ua = answersFor("BE", Other, "__none__")

        navigator.navigateFromImportSubCodePage(NormalMode)(ua) mustBe
          importRoutes.DescribeItemsOnImportDocController.onPageLoad(NormalMode)
      }

      "must go from ImportSubCodePage to the free text page when Other and a 99 sub code was selected" in {
        val ua = answersFor("BE", Other, "10.99")

        navigator.navigateFromImportSubCodePage(NormalMode)(ua) mustBe
          importRoutes.DescribeItemsOnImportDocController.onPageLoad(NormalMode)
      }

      "must go from ImportSubCodePage to the sub category page when the sub code has sub categories" in {
        val fakeConfig = new ConfigPurchaseOrImportMapping() {
          override def subcategoriesFor(country: String, parentKey: String, subcode: String): Seq[(String, String)] =
            Seq(("1.2.6", "sub.fuel.2.6"))
        }
        val ua = answersFor("AT", Fuel, "1.2")

        navigatorWith(fakeConfig).navigateFromImportSubCodePage(NormalMode)(ua) mustBe
          importRoutes.ImportSubCategoryController.onPageLoad(NormalMode)
      }

      "must go from ImportSubCodePage to SadReferenceCheck when the sub code has no sub categories" in {
        val ua = answersFor("AT", Fuel, "1.3")

        navigator.navigateFromImportSubCodePage(NormalMode)(ua) mustBe
          importRoutes.SadReferenceCheckController.onPageLoad(NormalMode)
      }

      "must go from ImportSubCodePage to SadReferenceCheck when None of these was selected for a type other than Other" in {
        val ua = answersFor("AT", Fuel, "__none__")

        navigator.navigateFromImportSubCodePage(NormalMode)(ua) mustBe
          importRoutes.SadReferenceCheckController.onPageLoad(NormalMode)
      }

      "must go from ImportSubCodePage to Journey Recovery when no sub code has been answered" in {
        val ua = userAnswers.set(ImportTypePage, Fuel).success.value

        navigator.navigateFromImportSubCodePage(NormalMode)(ua) mustBe
          controllers.routes.JourneyRecoveryController.onPageLoad()
      }

      "must go from ImportSubCategoryPage to SadReferenceCheck when a sub category has been answered" in {
        val ua = userAnswers.set(ImportSubCategoryPage, "1.2.6").success.value

        navigator.navigateFromImportSubCategoryPage(NormalMode)(ua) mustBe
          importRoutes.SadReferenceCheckController.onPageLoad(NormalMode)
      }

      "must go from ImportSubCategoryPage to Journey Recovery when no sub category has been answered" in {
        navigator.navigateFromImportSubCategoryPage(NormalMode)(userAnswers) mustBe
          controllers.routes.JourneyRecoveryController.onPageLoad()
      }

      "must go from SadReferenceCheckPage to the SAD reference number page when the user has a SAD reference" in {
        val ua = userAnswers.set(SadReferenceCheckPage, true).success.value

        navigator.navigateFromSadReferenceCheckPage(NormalMode)(ua) mustBe
          importRoutes.SadReferenceNumberController.onPageLoad(NormalMode)
      }

      "must go from SadReferenceCheckPage to the import details info page when the user has no SAD reference" in {
        val ua = userAnswers.set(SadReferenceCheckPage, false).success.value

        navigator.navigateFromSadReferenceCheckPage(NormalMode)(ua) mustBe
          importRoutes.ImportDetailsInfoController.onPageLoad(NormalMode)
      }

      "must go from SadReferenceCheckPage to Journey Recovery when the question is unanswered" in {
        navigator.navigateFromSadReferenceCheckPage(NormalMode)(userAnswers) mustBe
          importRoutes.ImportDetailsInfoController.onPageLoad(NormalMode)
      }

      "must go from navigateToCurrencyOrNextPage to ImportCurrency when the country requires currency selection" in {
        val ua = userAnswers.set(RefundingCountryPage, "EE").success.value

        navigator.navigateToCurrencyOrNextPage(NormalMode)(ua) mustBe
          importRoutes.ImportCurrencyController.onPageLoad(NormalMode)
      }

      "must skip ImportCurrency in navigateToCurrencyOrNextPage when the country has one currency" in {
        val ua = userAnswers.set(RefundingCountryPage, "AT").success.value

        navigator.navigateToCurrencyOrNextPage(NormalMode)(ua) mustBe
          importRoutes.TotalAmountWithoutVatController.onPageLoad(NormalMode)
      }

      "must go from navigateToCurrencyOrNextPage to Journey Recovery when no country is in session" in {
        navigator.navigateToCurrencyOrNextPage(NormalMode)(userAnswers) mustBe
          controllers.routes.JourneyRecoveryController.onPageLoad()
      }

      "must go from ImportDatePage to SadReferenceNumberController in Normal Mode when SadReferencePage is true" in {
        val ua = userAnswers.set(SadReferenceCheckPage, true).success.value
        navigator.backLinkFromImportDatePage(NormalMode)(ua) mustBe
          controllers.imports.routes.SadReferenceNumberController.onPageLoad(NormalMode)
      }

      "must go from ImportDatePage to ImportDetailsInfoController in Normal Mode when SadReferencePage is false" in {
        val ua = userAnswers.set(SadReferenceCheckPage, false).success.value
        navigator.backLinkFromImportDatePage(NormalMode)(ua) mustBe
          controllers.imports.routes.ImportDetailsInfoController.onPageLoad(NormalMode)
      }

      "must go from ImportDatePage to Journey Recovery in Normal Mode when SadReferencePage is not answered" in {
        navigator.backLinkFromImportDatePage(NormalMode)(userAnswers) mustBe
          controllers.routes.JourneyRecoveryController.onPageLoad()
      }
    }

    "in Check mode" - {

      "must go from ImportTypePage to JourneyRecovery when ImportType present but no country" in {
        val ua = userAnswers.set(ImportTypePage, Fuel).success.value

        navigator.navigateFromImportTypePage(CheckMode)(ua) mustBe
          controllers.routes.JourneyRecoveryController.onPageLoad()
      }

      "must go from ImportSubCodePage to SadReferenceCheck when the sub code has no sub categories" in {
        val ua = answersFor("AT", Fuel, "1.3")

        navigator.navigateFromImportSubCodePage(CheckMode)(ua) mustBe
          importRoutes.SadReferenceCheckController.onPageLoad(CheckMode)
      }

      "must go from ImportSubCategoryPage to SadReferenceCheck when a sub category has been answered" in {
        val ua = userAnswers.set(ImportSubCategoryPage, "1.2.6").success.value
        navigator.navigateFromImportSubCategoryPage(CheckMode)(ua) mustBe
          importRoutes.SadReferenceCheckController.onPageLoad(CheckMode)
      }

      "must go from SadReferenceCheckPage to the SAD reference number page when the user has a SAD reference" in {
        val ua = userAnswers.set(SadReferenceCheckPage, true).success.value

        navigator.navigateFromSadReferenceCheckPage(CheckMode)(ua) mustBe
          importRoutes.SadReferenceNumberController.onPageLoad(CheckMode)
      }

      "must go from SadReferenceCheckPage to the import details info page when the user has no SAD reference" in {
        val ua = userAnswers.set(SadReferenceCheckPage, false).success.value

        navigator.navigateFromSadReferenceCheckPage(CheckMode)(ua) mustBe
          importRoutes.ImportDetailsInfoController.onPageLoad(CheckMode)
      }

      "must go from navigateToCurrencyOrNextPage to ImportCurrency when the country requires currency selection" in {
        val ua = userAnswers.set(RefundingCountryPage, "EE").success.value

        navigator.navigateToCurrencyOrNextPage(CheckMode)(ua) mustBe
          importRoutes.ImportCurrencyController.onPageLoad(CheckMode)
      }
    }

    "navigateFromSadReferenceCheckPage" - {

      val navigator = navigatorWith(new ConfigPurchaseOrImportMapping())

      "must go to SAD reference in NormalMode when the answer is Yes" in {
        val answers = emptyUserAnswers
          .set(SadReferenceCheckPage, true)
          .success.value

        navigator.navigateFromSadReferenceCheckPage(NormalMode)(answers) mustBe
          controllers.imports.routes.SadReferenceNumberController
            .onPageLoad(NormalMode)
      }

      "must go to document information in NormalMode when the answer is No" in {
        val answers = emptyUserAnswers
          .set(SadReferenceCheckPage, false)
          .success.value

        navigator.navigateFromSadReferenceCheckPage(NormalMode)(answers) mustBe
          controllers.imports.routes.ImportDetailsInfoController
            .onPageLoad(NormalMode)
      }

      Seq(NormalMode, CheckMode).foreach { mode =>
        s"must go to Journey Recovery when unanswered in $mode" in {
          navigator.navigateFromSadReferenceCheckPage(mode)(
            emptyUserAnswers
          ) mustBe controllers.routes.JourneyRecoveryController.onPageLoad()
        }
      }

      "must go to SAD reference in CheckMode when changed to Yes" in {
        val answers = emptyUserAnswers
          .set(SadReferenceCheckPage, true)
          .flatMap(_.set(ImportSadChangedPage, true))
          .success.value

        navigator.navigateFromSadReferenceCheckPage(CheckMode)(answers) mustBe
          controllers.imports.routes.SadReferenceNumberController
            .onPageLoad(CheckMode)
      }

      "must go to document information in CheckMode when changed to No" in {
        val answers = emptyUserAnswers
          .set(SadReferenceCheckPage, false)
          .flatMap(_.set(ImportSadChangedPage, true))
          .success.value

        navigator.navigateFromSadReferenceCheckPage(CheckMode)(answers) mustBe
          controllers.imports.routes.ImportDetailsInfoController
            .onPageLoad(CheckMode)
      }

      Seq(true, false).foreach { answer =>
        s"must return unchanged SAD answer $answer to CYA in CheckMode" in {
          val answers = emptyUserAnswers
            .set(SadReferenceCheckPage, answer)
            .flatMap(_.set(ImportSadChangedPage, false))
            .success.value

          navigator.navigateFromSadReferenceCheckPage(CheckMode)(answers) mustBe
            controllers.imports.routes.CheckYourImportDetailsController
              .onPageLoad()
        }
      }
    }

    "navigateFromSadReferenceNumberPage" - {

      val navigator = navigatorWith(new ConfigPurchaseOrImportMapping())

      "must go to Import date in NormalMode when SAD is Yes" in {
        val answers = emptyUserAnswers
          .set(SadReferenceCheckPage, true)
          .flatMap(_.set(SadReferenceNumberPage, "SAD123"))
          .success.value

        navigator.navigateFromSadReferenceNumberPage(NormalMode)(answers) mustBe
          controllers.imports.routes.ImportDateController.onPageLoad(NormalMode)
      }

      "must preserve the document information fallback in NormalMode when SAD is No" in {
        val answers = emptyUserAnswers
          .set(SadReferenceCheckPage, false)
          .success.value

        navigator.navigateFromSadReferenceNumberPage(NormalMode)(answers) mustBe
          controllers.imports.routes.ImportDetailsInfoController
            .onPageLoad(NormalMode)
      }

      "must preserve the document information fallback in NormalMode when SAD is missing" in {
        navigator.navigateFromSadReferenceNumberPage(NormalMode)(
          emptyUserAnswers
        ) mustBe controllers.imports.routes.ImportDetailsInfoController
          .onPageLoad(NormalMode)
      }

      "must return to CYA after a valid SAD reference submission in CheckMode" in {
        val answers = emptyUserAnswers
          .set(SadReferenceCheckPage, true)
          .flatMap(_.set(SadReferenceNumberPage, "SAD123"))
          .success.value

        navigator.navigateFromSadReferenceNumberPage(CheckMode)(answers) mustBe
          controllers.imports.routes.CheckYourImportDetailsController.onPageLoad()
      }
    }

    "navigateFromTotalAmountWithoutVatPage" - {

      val navigator = navigatorWith(new ConfigPurchaseOrImportMapping())

      "must go to VAT paid in NormalMode when an amount exists" in {
        val answers = emptyUserAnswers
          .set(TotalAmountWithoutVatPage, BigDecimal("100"))
          .success.value

        navigator.navigateFromTotalAmountWithoutVatPage(NormalMode)(answers) mustBe
          controllers.imports.routes.TotalVatPaidImportController
            .onPageLoad(NormalMode)
      }

      "must go to VAT paid in CheckMode when the amount changed" in {
        val answers = emptyUserAnswers
          .set(TotalAmountWithoutVatPage, BigDecimal("100"))
          .flatMap(_.set(ImportAmountWithoutVatChangedPage, true))
          .success.value

        navigator.navigateFromTotalAmountWithoutVatPage(CheckMode)(answers) mustBe
          controllers.imports.routes.TotalVatPaidImportController
            .onPageLoad(CheckMode)
      }

      "must return to CYA in CheckMode when the amount is unchanged" in {
        val answers = emptyUserAnswers
          .set(TotalAmountWithoutVatPage, BigDecimal("100"))
          .flatMap(_.set(ImportAmountWithoutVatChangedPage, false))
          .success.value

        navigator.navigateFromTotalAmountWithoutVatPage(CheckMode)(answers) mustBe
          controllers.imports.routes.CheckYourImportDetailsController.onPageLoad()
      }

      Seq(NormalMode, CheckMode).foreach { mode =>
        s"must go to Journey Recovery when the amount is missing in $mode" in {
          navigator.navigateFromTotalAmountWithoutVatPage(mode)(
            emptyUserAnswers
          ) mustBe controllers.routes.JourneyRecoveryController.onPageLoad()
        }
      }
    }

    "backLinkFromImportDatePage" - {

      "must return to Import CYA in CheckMode regardless of the SAD answer" in {
        val answers = Seq(
          emptyUserAnswers,
          emptyUserAnswers
            .set(SadReferenceCheckPage, true)
            .success.value,
          emptyUserAnswers
            .set(SadReferenceCheckPage, false)
            .success.value
        )

        answers.foreach { userAnswers =>
          navigator.backLinkFromImportDatePage(CheckMode)(userAnswers) mustBe
            controllers.imports.routes.CheckYourImportDetailsController.onPageLoad()
        }
      }

      "must return to SAD reference number in NormalMode when SAD is Yes" in {
        val answers = emptyUserAnswers
          .set(SadReferenceCheckPage, true)
          .success.value

        navigator.backLinkFromImportDatePage(NormalMode)(answers) mustBe
          controllers.imports.routes.SadReferenceNumberController
            .onPageLoad(NormalMode)
      }

      "must return to import document information in NormalMode when SAD is No" in {
        val answers = emptyUserAnswers
          .set(SadReferenceCheckPage, false)
          .success.value

        navigator.backLinkFromImportDatePage(NormalMode)(answers) mustBe
          controllers.imports.routes.ImportDetailsInfoController
            .onPageLoad(NormalMode)
      }

      "must return to Journey Recovery in NormalMode when the SAD answer is missing" in {
        navigator.backLinkFromImportDatePage(NormalMode)(emptyUserAnswers) mustBe
          controllers.routes.JourneyRecoveryController.onPageLoad()
      }
    }
  }
}
