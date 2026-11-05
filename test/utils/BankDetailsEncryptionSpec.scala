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

  private val provider = new CryptoProvider(Configuration(ConfigFactory.load()))
  private val iban     = "DE89370400440532013000"
  private val bic      = "DEUTDEFF513"

  "CryptoProvider" should {

    "decrypt encrypted IBAN and BIC back to the originals" in {
      val (encIban, encBic) = provider.encrypt(iban, bic)

      encIban should not be iban
      encBic  should not be bic
      provider.decrypt(encIban, encBic) shouldBe Some((iban, bic))
    }

    "produce a different encrypted value each time for the same input" in {
      provider.encrypt(iban, bic) should not be provider.encrypt(iban, bic)
    }

    "produce a known cipher text that decrypts to the known text" in {
      val known = provider.knownCipherText
      provider.decrypt(known, known) shouldBe Some(("knowntext", "knowntext"))
    }

    "return None when a value cannot be decrypted" in {
      val (encIban, _) = provider.encrypt(iban, bic)
      provider.decrypt(encIban, "not-a-valid-encrypted-value") shouldBe None
    }
  }
}