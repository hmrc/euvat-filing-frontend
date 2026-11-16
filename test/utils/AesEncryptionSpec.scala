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
  private val plainText = "AB123456C"
  private val iBan = "d67BQJZWtL3pqprT23Wp9gYyJtBOIc4/9miKcdXwLh0="
  private val bicCode = "RJAsIwOAF0TBXkk1KUIJALpZF26O84DbqppSd/sSHh4="
  private val cipherText = "ofqMcMn1FmiqJePk+xik5365lyhiZNlF7jdDg9RN5O8="

  "AesEncryption" should {
    "Encrypt and decrypt the data" in {
      val encrypted = aesService.encrypt(plainText)
      val decrypted = aesService.decrypt(encrypted)

      decrypted shouldBe "AB123456C"
    }

    "Decrypt the IBAN number" in {
      val decrypted = aesService.decrypt(iBan)
      decrypted shouldBe "GB00TESTIBANXX"
    }

    "Decrypt the Bic code number" in {
      val decrypted = aesService.decrypt(bicCode)
      decrypted shouldBe "TESTXX00"
    }

    "Decrypt the cipher text" in {
      val decrypted = aesService.decrypt(cipherText)
      decrypted shouldBe "knowntext"
    }

    "produce a different encrypted value each time for the same input" in {
      aesService.encrypt(iBan) should not be aesService.encrypt(iBan)
    }

    "produce a known cipher text that decrypts to the known text" in {
      aesService.decrypt(aesService.knownCipherText) shouldBe "knowntext"
    }

  }
}
