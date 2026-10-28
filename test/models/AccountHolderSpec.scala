package models

import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.OptionValues
import play.api.libs.json.{JsError, JsString, Json}

class AccountHolderSpec extends AnyFreeSpec with Matchers with ScalaCheckPropertyChecks with OptionValues {

  "AccountHolder" - {

    "must deserialise valid values" in {

      val gen = Gen.oneOf(AccountHolder.values.toSeq)

      forAll(gen) {
        accountHolder =>

          JsString(accountHolder.toString).validate[AccountHolder].asOpt.value mustEqual accountHolder
      }
    }

    "must fail to deserialise invalid values" in {

      val gen = arbitrary[String] suchThat (!AccountHolder.values.map(_.toString).contains(_))

      forAll(gen) {
        invalidValue =>

          JsString(invalidValue).validate[AccountHolder] mustEqual JsError("error.invalid")
      }
    }

    "must serialise" in {

      val gen = Gen.oneOf(AccountHolder.values.toSeq)

      forAll(gen) {
        accountHolder =>

          Json.toJson(accountHolder) mustEqual JsString(accountHolder.toString)
      }
    }
  }
}
