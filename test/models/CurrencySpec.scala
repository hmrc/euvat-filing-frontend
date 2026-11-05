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

package models

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers

class CurrencySpec extends AnyFreeSpec with Matchers {

  "Currency" - {

    ".displayValue" - {

      "must return the currency code and description separated by a hyphen" in {

        val currency =
          Currency(
            code        = "GBP",
            description = "United Kingdom, Pounds"
          )

        currency.displayValue mustBe
          "GBP - United Kingdom, Pounds"
      }

      "must return the correct display value for another currency" in {

        val currency =
          Currency(
            code        = "AFN",
            description = "Afghanistan, Afghanis"
          )

        currency.displayValue mustBe
          "AFN - Afghanistan, Afghanis"
      }

      "must preserve punctuation in the currency description" in {

        val currency =
          Currency(
            code        = "USD",
            description = "United States, Dollars"
          )

        currency.displayValue mustBe
          "USD - United States, Dollars"
      }
    }
  }
}
