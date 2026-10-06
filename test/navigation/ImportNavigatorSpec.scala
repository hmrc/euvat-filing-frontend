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

        navigatorWith(fakeConfig).navigateFromImportTypePage(NormalMode)(ua) mustBe
          importRoutes.ImportSubCodeController.onPageLoad(Fuel.toString)
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
        val ua = userAnswers
          .set(RefundingCountryPage, "BE")
          .success
          .value
          .set(ImportTypePage, Other)
          .success
          .value
          .set(ImportSubCodePage, "__none__")
          .success
          .value

        navigator.navigateFromImportSubCodePage(NormalMode)(ua) mustBe
          importRoutes.DescribeItemsOnImportDocController.onPageLoad(NormalMode)
      }

      "must go from ImportSubCodePage to the free text page when Other and a 99 sub code was selected" in {
        val ua = userAnswers
          .set(RefundingCountryPage, "BE")
          .success
          .value
          .set(ImportTypePage, Other)
          .success
          .value
          .set(ImportSubCodePage, "10.99")
          .success
          .value

        navigator.navigateFromImportSubCodePage(NormalMode)(ua) mustBe
          importRoutes.DescribeItemsOnImportDocController.onPageLoad(NormalMode)
      }

      "must go from ImportSubCodePage to SadReferenceCheck when None of these was selected for a type other than Other" in {
        val ua = userAnswers
          .set(RefundingCountryPage, "AT")
          .success
          .value
          .set(ImportTypePage, Fuel)
          .success
          .value
          .set(ImportSubCodePage, "__none__")
          .success
          .value

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

      "must go from navigateToCurrencyOrNextPage to ImportCurrency when the country requires currency selection" in {
        val ua = userAnswers.set(RefundingCountryPage, "EE").success.value

        navigator.navigateToCurrencyOrNextPage(NormalMode)(ua) mustBe
          importRoutes.ImportCurrencyController.onPageLoad(NormalMode)
      }

      "must skip ImportCurrency in navigateToCurrencyOrNextPage when the country has one currency" in {
        val ua = userAnswers.set(RefundingCountryPage, "AT").success.value

        navigator.navigateToCurrencyOrNextPage(NormalMode)(ua) mustBe
          controllers.routes.JourneyRecoveryController.onPageLoad()
      }

      "must go from navigateToCurrencyOrNextPage to Journey Recovery when no country is in session" in {
        navigator.navigateToCurrencyOrNextPage(NormalMode)(userAnswers) mustBe
          controllers.routes.JourneyRecoveryController.onPageLoad()
      }
    }

    "in Check mode" - {

      "must go from ImportTypePage to JourneyRecovery when ImportType present but no country" in {
        val ua = userAnswers.set(ImportTypePage, Fuel).success.value

        navigator.navigateFromImportTypePage(CheckMode)(ua) mustBe
          controllers.routes.JourneyRecoveryController.onPageLoad()
      }

      "must go from ImportSubCategoryPage to SadReferenceCheck when a sub category has been answered" in {
        val ua = userAnswers.set(ImportSubCategoryPage, "1.2.6").success.value

        navigator.navigateFromImportSubCategoryPage(CheckMode)(ua) mustBe
          importRoutes.SadReferenceCheckController.onPageLoad(CheckMode)
      }

      "must go from navigateToCurrencyOrNextPage to ImportCurrency when the country requires currency selection" in {
        val ua = userAnswers.set(RefundingCountryPage, "EE").success.value

        navigator.navigateToCurrencyOrNextPage(CheckMode)(ua) mustBe
          importRoutes.ImportCurrencyController.onPageLoad(CheckMode)
      }
    }
  }
}
