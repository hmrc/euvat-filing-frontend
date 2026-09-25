package pages

case object ImportCurrencyPage extends QuestionPage[String] {
  override def path: JsPath = JsPath \ toString
  override def toString: String = "importCurrency"
}
