package models

import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.OptionValues
import play.api.libs.json.{JsError, JsString, Json}

class SupplierTaxNumberSpec extends AnyFreeSpec with Matchers with ScalaCheckPropertyChecks with OptionValues {

  "SupplierTaxNumber" - {

    "must deserialise valid values" in {

      val gen = Gen.oneOf(SupplierTaxNumber.values.toSeq)

      forAll(gen) {
        supplierTaxNumber =>

          JsString(supplierTaxNumber.toString).validate[SupplierTaxNumber].asOpt.value mustEqual supplierTaxNumber
      }
    }

    "must fail to deserialise invalid values" in {

      val gen = arbitrary[String] suchThat (!SupplierTaxNumber.values.map(_.toString).contains(_))

      forAll(gen) {
        invalidValue =>

          JsString(invalidValue).validate[SupplierTaxNumber] mustEqual JsError("error.invalid")
      }
    }

    "must serialise" in {

      val gen = Gen.oneOf(SupplierTaxNumber.values.toSeq)

      forAll(gen) {
        supplierTaxNumber =>

          Json.toJson(supplierTaxNumber) mustEqual JsString(supplierTaxNumber.toString)
      }
    }
  }
}
