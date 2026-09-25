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

        nav.navigateFromImportTypePage(NormalMode)(ua) mustBe controllers.imports.routes.SadReferenceController.onPageLoad
      }

      "must go from ImportTypePage to SadReference when the only sub code is 10.99" in {
        val fakePurchaseConfig = new ConfigPurchaseOrImportMapping() {
          override def subcodesFor(country: String, parentKey: String): Seq[(String, String)] =
            if (parentKey == Other.toString) Seq(("10.99", "purchase.sub.other.10.99")) else Seq.empty
        }
        val nav = new ImportNavigator(
          new CurrencyConfig(Configuration(ConfigFactory.parseString("""currency.mapping = {}"""))),
          fakePurchaseConfig
        )
        val ua = userAnswers.set(RefundingCountryPage, "AT").success.value.set(ImportTypePage, Other).success.value

        nav.navigateFromImportTypePage(NormalMode)(ua) mustBe controllers.imports.routes.SadReferenceController.onPageLoad
      }
    }

    "must go from ImportTypePage to Journey Recovery when no country has been answered" in {
      val nav = new ImportNavigator(
        new CurrencyConfig(Configuration(ConfigFactory.parseString("""currency.mapping = {}"""))),
        new ConfigPurchaseOrImportMapping()
      )
      val ua = userAnswers.set(ImportTypePage, Transport).success.value

      nav.navigateFromImportTypePage(NormalMode)(ua) mustBe controllers.routes.JourneyRecoveryController.onPageLoad()
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
