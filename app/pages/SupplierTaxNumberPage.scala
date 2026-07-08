package pages

import models.SupplierTaxNumber
import play.api.libs.json.JsPath

case object SupplierTaxNumberPage extends QuestionPage[SupplierTaxNumber] {

  override def path: JsPath = JsPath \ toString

  override def toString: String = "supplierTaxNumber"
}
