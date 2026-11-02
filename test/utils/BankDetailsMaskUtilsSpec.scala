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

package utils

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class BankDetailsMaskUtilsSpec extends AnyWordSpec with Matchers {

  "BankDetailsMaskUtils.maskLastFour" should {
    "show only the last 4 characters of an IBAN" in {
      BankDetailsMaskUtils.maskLastFour("DE89370400440532015679") shouldBe "******************5679"
    }
    "show only the last 4 characters of a BIC" in {
      BankDetailsMaskUtils.maskLastFour("DEUTDEFF513") shouldBe "*******F513"
    }
    "show a value of 4 characters or fewer unchanged" in {
      BankDetailsMaskUtils.maskLastFour("1234") shouldBe "1234"
    }
  }
}
