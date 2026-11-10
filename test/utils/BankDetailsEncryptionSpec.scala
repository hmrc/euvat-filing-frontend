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

import com.typesafe.config.ConfigFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.Configuration

class BankDetailsEncryptionSpec extends AnyWordSpec with Matchers {

  private val provider = new BankDetailsEncryption(Configuration(ConfigFactory.load()))
  private val iban = "DE89370400440532013000"
  private val bic = "DEUTDEFF513"

  "BankDetailsEncryption" should {

    "decrypt an encrypted value back to the original" in {
      val encrypted = provider.encrypt(iban)
      encrypted                     should not be iban
      provider.decrypt(encrypted) shouldBe iban
    }

    "decrypt an encrypted BIC back to the original" in {
      val encrypted = provider.encrypt(bic)

      encrypted                     should not be bic
      provider.decrypt(encrypted) shouldBe bic
    }

    "decrypt an encrypted 8-character BIC back to the original" in {
      val shortBic = "DEUTDEFF"
      val encrypted = provider.encrypt(shortBic)

      provider.decrypt(encrypted) shouldBe shortBic
    }

    "decrypt an encrypted long IBAN back to the original" in {
      val longIban = "MT84MALT011000012345MTLCAST001S"
      val encrypted = provider.encrypt(longIban)

      provider.decrypt(encrypted) shouldBe longIban
    }

    "produce a different encrypted value each time for the same input" in {
      provider.encrypt(iban) should not be provider.encrypt(iban)
    }

    "produce a known cipher text that decrypts to the known text" in {
      provider.decrypt(provider.knownCipherText) shouldBe "knowntext"
    }

  }
}
