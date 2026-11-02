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

  private val encryption = new BankDetailsEncryption(Configuration(ConfigFactory.load()))
  private val iban = "DE89370400440532013000"

  "BankDetailsEncryption" should {

    "decrypt an encrypted value back to the original" in {
      val encrypted = encryption.encrypt(iban)

      encrypted                       should not be iban
      encryption.decrypt(encrypted) shouldBe Some(iban)
    }

    "produce a known cipher text that decrypts to the known text" in {
      encryption.decrypt(encryption.knownCipherText) shouldBe Some("knowntext")
    }

    "return None when the value cannot be decrypted" in {
      encryption.decrypt("not-a-valid-encrypted-value") shouldBe None
    }
  }
}
