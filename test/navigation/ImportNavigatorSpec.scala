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
import models.*
import pages.*
import play.api.Configuration
import utils.{ConfigLanguageMapping, ConfigPurchaseOrImportMapping, CurrencyConfig}
import controllers.purchase.routes as purchaseRoutes
import controllers.imports.routes as importRoutes
import controllers.routes as routes

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

  "Navigator" - {

    "in Normal mode" - {
      "must go from ImportTypePage to JourneyRecovery when ImportType present but no country" in {
        val ua = userAnswers.set(ImportTypePage, Fuel).success.value
        navigator.navigateFromImportTypePage(NormalMode)(ua) mustBe
          controllers.routes.JourneyRecoveryController.onPageLoad()
      }

      "must go from ImportTypePage to the import sub code page for that type when the country has sub codes" in {
        val fakeImportConfig = new ConfigPurchaseOrImportMapping() {
          override def subcodesFor(country: String, parentKey: String): Seq[(String, String)] =
            if (country == "BG" && parentKey == Fuel.toString) {
              Seq(("1.1", "purchase.sub.fuel.1.1"), ("1.1.1", "purchase.sub.fuel.1.1.1"))
            } else {
              Seq.empty
            }
        }
        val nav = new ImportNavigator(
          new CurrencyConfig(Configuration(ConfigFactory.parseString("""currency.mapping = {}"""))),
          fakeImportConfig
        )
        val ua = userAnswers.set(RefundingCountryPage, "BG").success.value.set(ImportTypePage, Fuel).success.value

        nav.navigateFromImportTypePage(NormalMode)(ua) mustBe
          controllers.imports.routes.ImportSubCodeController.onPageLoad(Fuel.toString)
      }

      "must go from ImportTypePage to SadReference when the country has no sub codes for that type" in {
        val fakeImportConfig = new ConfigPurchaseOrImportMapping() {
          override def subcodesFor(country: String, parentKey: String): Seq[(String, String)] = Seq.empty
        }
        val nav = new ImportNavigator(
          new CurrencyConfig(Configuration(ConfigFactory.parseString("""currency.mapping = {}"""))),
          fakeImportConfig
        )
        val ua = userAnswers.set(RefundingCountryPage, "AT").success.value.set(ImportTypePage, Transport).success.value

        nav.navigateFromImportTypePage(NormalMode)(ua) mustBe importRoutes.SadReferenceController.onPageLoad(NormalMode)
      }

      "must go from ImportTypePage to Journey Recovery when no country has been answered" in {
        val nav = new ImportNavigator(
          new CurrencyConfig(Configuration(ConfigFactory.parseString("""currency.mapping = {}"""))),
          new ConfigPurchaseOrImportMapping()
        )
        val ua = userAnswers.set(ImportTypePage, Transport).success.value

        nav.navigateFromImportTypePage(NormalMode)(ua) mustBe controllers.routes.JourneyRecoveryController.onPageLoad()
    }

      "must go from PurchaseOrImportPage to ImportTypeController when Import is selected" in {
        val claimNav = new ClaimNavigator(new ConfigLanguageMapping(Configuration(ConfigFactory.parseString("""language.mapping = {}"""))))
        val purchaseNav = new PurchaseNavigator(new CurrencyConfig(Configuration(ConfigFactory.parseString("""currency.mapping = {}"""))),
          new ConfigPurchaseOrImportMapping()
        )
        val importNav = new ImportNavigator(new CurrencyConfig(Configuration(ConfigFactory.parseString("""currency.mapping = {}"""))),
          new ConfigPurchaseOrImportMapping()
        )

      val nav = new navigation.Navigator(claimNav, purchaseNav, importNav)

      val ua = userAnswers.set(PurchaseOrImportPage, models.PurchaseOrImport.Import).success.value

      nav.nextPage(PurchaseOrImportPage, NormalMode, ua) mustBe
        importRoutes.ImportTypeController.onPageLoad(NormalMode)
    }

      "must go from PurchaseTypePage to DescribeItemsOnInvoiceController" in {
        val claimNav = new ClaimNavigator(new ConfigLanguageMapping(Configuration(ConfigFactory.parseString("""language.mapping = {}"""))))
        val purchaseNav = new PurchaseNavigator(new CurrencyConfig(Configuration(ConfigFactory.parseString("""currency.mapping = {}"""))),
          new ConfigPurchaseOrImportMapping()
        )
        val importNav = new ImportNavigator(new CurrencyConfig(Configuration(ConfigFactory.parseString("""currency.mapping = {}"""))),
          new ConfigPurchaseOrImportMapping()
        )

      val nav = new navigation.Navigator(claimNav, purchaseNav, importNav)

      val ua = userAnswers.set(PurchaseTypePage, models.PurchaseOrImportType.values.head).success.value

      nav.nextPage(PurchaseTypePage, NormalMode, ua) mustBe
        purchaseRoutes.DescribeItemsOnInvoiceController.onPageLoad(NormalMode)
      }
      "must go from ImportTypePage to the free text page when Other has no selectable sub codes" in {
        val fakeConfig = new ConfigPurchaseOrImportMapping() {
          override def subcodesFor(country: String, parentKey: String): Seq[(String, String)] =
            if (parentKey == Other.toString) Seq(("10.99", "sub.other.99")) else Seq.empty
        }
        val nav = new ImportNavigator(
          new CurrencyConfig(Configuration(ConfigFactory.parseString("""currency.mapping = {}"""))),
          fakeConfig
        )
        val ua = userAnswers
          .set(RefundingCountryPage, "AT")
          .success
          .value
          .set(ImportTypePage, Other)
          .success
          .value

        nav.navigateFromImportTypePage(NormalMode)(ua) mustBe
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

      "must go from ImportSubCodePage to SadReference when None of these was selected for a type other than Other" in {
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
          importRoutes.SadReferenceController.onPageLoad(NormalMode)
      }
    }

    "in Check mode" - {
      "must go from ImportTypePage to JourneyRecovery in CheckMode when ImportType present but no country" in {
        val ua = userAnswers.set(ImportTypePage, Fuel).success.value
        navigator.navigateFromImportTypePage(CheckMode)(ua) mustBe
          controllers.routes.JourneyRecoveryController.onPageLoad()
      }

    }

  }
}
