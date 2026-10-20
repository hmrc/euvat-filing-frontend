package pages.imports

import pages.QuestionPage
import play.api.libs.json.JsPath

case object ImportMissingAnswersJourneyPage extends QuestionPage[Boolean] {
  override def path: JsPath = JsPath \ toString
  override def toString: String = "importMissingAnswersJourney"
}
