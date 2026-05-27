package models

import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.OptionValues
import play.api.libs.json.{JsError, JsString, Json}

class InvoiceTypeSpec extends AnyFreeSpec with Matchers with ScalaCheckPropertyChecks with OptionValues {

  "InvoiceType" - {

    "must deserialise valid values" in {

      val gen = Gen.oneOf(InvoiceType.values.toSeq)

      forAll(gen) {
        invoiceType =>

          JsString(invoiceType.toString).validate[InvoiceType].asOpt.value mustEqual invoiceType
      }
    }

    "must fail to deserialise invalid values" in {

      val gen = arbitrary[String] suchThat (!InvoiceType.values.map(_.toString).contains(_))

      forAll(gen) {
        invalidValue =>

          JsString(invalidValue).validate[InvoiceType] mustEqual JsError("error.invalid")
      }
    }

    "must serialise" in {

      val gen = Gen.oneOf(InvoiceType.values.toSeq)

      forAll(gen) {
        invoiceType =>

          Json.toJson(invoiceType) mustEqual JsString(invoiceType.toString)
      }
    }
  }
}
