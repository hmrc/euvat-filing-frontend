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

package config

import com.typesafe.config.ConfigFactory
import models.Currency
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers

class InternationalCurrencyProviderSpec extends AnyFreeSpec with Matchers {

  private val config =
    ConfigFactory.parseString(
      """
        |international-bank-currencies = [
        |  {
        |    code = "GBP"
        |    description = "United Kingdom, Pounds"
        |  },
        |  {
        |    code = "AFN"
        |    description = "Afghanistan, Afghanis"
        |  },
        |  {
        |    code = "ALL"
        |    description = "Albania, Leke"
        |  }
        |]
        |""".stripMargin
    )

  private val provider =
    new InternationalCurrencyProvider(config)

  "InternationalCurrencyProvider" - {

    ".currencies" - {

      "must load currencies from configuration" in {

        provider.currencies mustBe Seq(
          Currency(
            code        = "GBP",
            description = "United Kingdom, Pounds"
          ),
          Currency(
            code        = "AFN",
            description = "Afghanistan, Afghanis"
          ),
          Currency(
            code        = "ALL",
            description = "Albania, Leke"
          )
        )
      }

      "must load the correct number of currencies" in {

        provider.currencies must have size 3
      }

      "must preserve currency codes from configuration" in {

        provider.currencies.map(_.code) mustBe
          Seq(
            "GBP",
            "AFN",
            "ALL"
          )
      }

      "must preserve currency descriptions from configuration" in {

        provider.currencies.map(_.description) mustBe
          Seq(
            "United Kingdom, Pounds",
            "Afghanistan, Afghanis",
            "Albania, Leke"
          )
      }
    }

    ".isValid" - {

      "must return true when the currency code exists" in {

        provider.isValid("GBP") mustBe true
        provider.isValid("AFN") mustBe true
        provider.isValid("ALL") mustBe true
      }

      "must return false when the currency code does not exist" in {

        provider.isValid("XYZ") mustBe false
      }

      "must return false for an empty currency code" in {

        provider.isValid("") mustBe false
      }

      "must return false for a lowercase currency code" in {

        provider.isValid("gbp") mustBe false
      }

      "must return false when the currency code contains leading or trailing spaces" in {

        provider.isValid(" GBP ") mustBe false
      }
    }
  }
}
