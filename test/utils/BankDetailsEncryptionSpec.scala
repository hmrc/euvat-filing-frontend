package utils

import com.typesafe.config.ConfigFactory
import config.FrontendAppConfig
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.Configuration

class BankDetailsEncryptionSpec extends AnyWordSpec with Matchers {

  private val appConfig = new FrontendAppConfig(Configuration(ConfigFactory.load()))
  private val encryption = new BankDetailsEncryption(appConfig)

  "BankDetailsEncryption" should {

    "decrypt an encrypted value back to the original" in {
      val iban = "DE89370400440532013000"
      val encrypted = encryption.encrypt(iban)

      encrypted                       should not be iban
      encryption.decrypt(encrypted) shouldBe iban
    }

    "produce a known cipher text that decrypts to the known text" in {
      encryption.decrypt(encryption.knownCipherText) shouldBe "knowntext"
    }
  }
}
