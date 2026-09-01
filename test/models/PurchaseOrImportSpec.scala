package models

import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.OptionValues
import play.api.libs.json.{JsError, JsString, Json}

class PurchaseOrImportSpec extends AnyFreeSpec with Matchers with ScalaCheckPropertyChecks with OptionValues {

  "PurchaseOrImport" - {

    "must deserialise valid values" in {

      val gen = Gen.oneOf(PurchaseOrImport.values.toSeq)

      forAll(gen) {
        purchaseOrImport =>

          JsString(purchaseOrImport.toString).validate[PurchaseOrImport].asOpt.value mustEqual purchaseOrImport
      }
    }

    "must fail to deserialise invalid values" in {

      val gen = arbitrary[String] suchThat (!PurchaseOrImport.values.map(_.toString).contains(_))

      forAll(gen) {
        invalidValue =>

          JsString(invalidValue).validate[PurchaseOrImport] mustEqual JsError("error.invalid")
      }
    }

    "must serialise" in {

      val gen = Gen.oneOf(PurchaseOrImport.values.toSeq)

      forAll(gen) {
        purchaseOrImport =>

          Json.toJson(purchaseOrImport) mustEqual JsString(purchaseOrImport.toString)
      }
    }
  }
}
