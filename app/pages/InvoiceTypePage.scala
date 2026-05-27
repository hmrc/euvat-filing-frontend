package pages

import models.InvoiceType
import play.api.libs.json.JsPath

case object InvoiceTypePage extends QuestionPage[InvoiceType] {

  override def path: JsPath = JsPath \ toString

  override def toString: String = "invoiceType"
}
