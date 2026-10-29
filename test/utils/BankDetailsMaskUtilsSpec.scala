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
  }
}