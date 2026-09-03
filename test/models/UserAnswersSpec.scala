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

import base.SpecBase
import play.api.libs.json.Json
import pages.SuppliersNamePage
import pages.TotalVatPaidPage

class UserAnswersSpec extends SpecBase {

  "UserAnswers.isAnswerUnchanged" - {
    "must return true when the stored value is unchanged" in {
      val ua = emptyUserAnswers.set(TotalVatPaidPage, BigDecimal(12.34)).success.value
      ua.isAnswerUnchanged(TotalVatPaidPage, BigDecimal(12.34)) mustBe true
    }

    "must return false when the stored value changed" in {
      val ua = emptyUserAnswers.set(TotalVatPaidPage, BigDecimal(1)).success.value
      ua.isAnswerUnchanged(TotalVatPaidPage, BigDecimal(2)) mustBe false
    }

    "must return false when there is no stored value" in {
      val ua = emptyUserAnswers
      ua.isAnswerUnchanged(TotalVatPaidPage, BigDecimal(2)) mustBe false
    }
  }

  "set and get" - {
    "must set and retrieve a value" in {
      val ua = emptyUserAnswers.set(SuppliersNamePage, "Acme Ltd").success.value
      ua.get(SuppliersNamePage).value mustBe "Acme Ltd"
    }
  }

  "remove" - {
    "must remove a stored value" in {
      val ua = emptyUserAnswers.set(SuppliersNamePage, "Acme Ltd").success.value
      val removed = ua.remove(SuppliersNamePage).success.value
      removed.get(SuppliersNamePage) mustBe None
    }
  }

  "clear" - {
    "must clear all data" in {
      val ua = emptyUserAnswers.set(SuppliersNamePage, "Acme Ltd").success.value
      val cleared = ua.clear()
      cleared.data mustBe Json.obj()
    }
  }

  "json format" - {
    "must serialize and deserialize" in {
      val ua = emptyUserAnswers.set(SuppliersNamePage, "Acme Ltd").success.value
      val json = Json.toJson(ua)
      val parsed = json.validate[UserAnswers]
      parsed.isSuccess mustBe true
      parsed.get.get(SuppliersNamePage).value mustBe "Acme Ltd"
    }
  }

}
