package models

import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.OptionValues
import play.api.libs.json.{JsError, JsString, Json}

class RefundingCurrencySpec extends AnyFreeSpec with Matchers with ScalaCheckPropertyChecks with OptionValues {

  "RefundingCurrency" - {

    "must deserialise valid values" in {

      val gen = Gen.oneOf(RefundingCurrency.values.toSeq)

      forAll(gen) {
        refundingCurrency =>

          JsString(refundingCurrency.toString).validate[RefundingCurrency].asOpt.value mustEqual refundingCurrency
      }
    }

    "must fail to deserialise invalid values" in {

      val gen = arbitrary[String] suchThat (!RefundingCurrency.values.map(_.toString).contains(_))

      forAll(gen) {
        invalidValue =>

          JsString(invalidValue).validate[RefundingCurrency] mustEqual JsError("error.invalid")
      }
    }

    "must serialise" in {

      val gen = Gen.oneOf(RefundingCurrency.values.toSeq)

      forAll(gen) {
        refundingCurrency =>

          Json.toJson(refundingCurrency) mustEqual JsString(refundingCurrency.toString)
      }
    }
  }
}
