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

class AesEncryptionSpec extends AnyWordSpec with Matchers {
  private val keyStoreService = new EncryptionKeyStore(Configuration(ConfigFactory.load()))
  private val aesService = new AesEncryption(keyStoreService)
  private val plainText = "FAKETEST123456"
  private val iBan = "OKceBlhSS7CsB0g9UwlxyA=="
  private val bicCode = "eQTlwOQVzexwqJr86eA10A=="
  private val cipherText = "cC/I7STN++joU/EVvNVt6A=="

  "AesEncryption" should {
    "Encrypt and decrypt the data" in {
      val encrypted = aesService.encrypt(plainText)
      val decrypted = aesService.decrypt(encrypted)

      decrypted shouldBe plainText
    }

    "Decrypt the IBAN number" in {
      val decrypted = aesService.decrypt(iBan)
      decrypted shouldBe "GB00TESTIBANXX"
    }

    "Decrypt the Bic code number" in {
      val decrypted = aesService.decrypt(bicCode)
      decrypted shouldBe "FAKETEST"
    }

    "Decrypt the cipher text" in {
      val decrypted = aesService.decrypt(cipherText)
      decrypted shouldBe "knowntext"
    }

    "produce a same encrypted value each time for the same input (to decrypt legacy data)" in {
      aesService.encrypt(iBan) shouldBe aesService.encrypt(iBan)
    }

    "produce a known cipher text that decrypts to the known text" in {
      aesService.decrypt(aesService.knownCipherText) shouldBe "knowntext"
    }

  }
}
