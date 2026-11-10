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
import play.api.libs.json.{JsSuccess, Json}

class BankDetailsSpec extends AnyFreeSpec with Matchers {

  private val bankDetails =
    BankDetails(
      NameOnTheAccount  = "John Smith",
      BIC               = "NWBKGB2L",
      accountNumber     = "GB29NWBK60161331926819",
      CurrencyOfAccount = "GBP"
    )

  private val json =
    Json.obj(
      "NameOnTheAccount"  -> "John Smith",
      "BIC"               -> "NWBKGB2L",
      "accountNumber"     -> "GB29NWBK60161331926819",
      "CurrencyOfAccount" -> "GBP"
    )

  "BankDetails" - {

    "must serialise to JSON" in {

      Json.toJson(bankDetails) mustBe json
    }

    "must deserialise from JSON" in {

      json.validate[BankDetails] mustBe
        JsSuccess(bankDetails)
    }

    "must serialise and deserialise successfully" in {

      val result =
        Json
          .toJson(bankDetails)
          .validate[BankDetails]

      result mustBe JsSuccess(bankDetails)
    }

    "must fail to deserialise when NameOnTheAccount is missing" in {

      val invalidJson =
        json - "NameOnTheAccount"

      invalidJson
        .validate[BankDetails]
        .isError mustBe true
    }

    "must fail to deserialise when BIC is missing" in {

      val invalidJson =
        json - "BIC"

      invalidJson
        .validate[BankDetails]
        .isError mustBe true
    }

    "must fail to deserialise when accountNumber is missing" in {

      val invalidJson =
        json - "accountNumber"

      invalidJson
        .validate[BankDetails]
        .isError mustBe true
    }

    "must fail to deserialise when CurrencyOfAccount is missing" in {

      val invalidJson =
        json - "CurrencyOfAccount"

      invalidJson
        .validate[BankDetails]
        .isError mustBe true
    }

    "must fail to deserialise when a field has the wrong type" in {

      val invalidJson =
        json ++ Json.obj(
          "BIC" -> 12345
        )

      invalidJson
        .validate[BankDetails]
        .isError mustBe true
    }
  }
}
