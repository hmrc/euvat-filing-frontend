/*
 * Copyright 2026 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers

import com.google.inject.Inject
import controllers.actions.{DataRequiredAction, DataRetrievalAction, IdentifierAction}
import models.UserAnswers
import pages.ClaimDetailsCompletedPage
import play.api.Logging
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.checkAnswers.CheckYourClaimDetailsSummary
import utils.{ConfigCurrencyMapping, ConfigLanguageMapping}
import views.html.CheckYourClaimDetailsView

import scala.concurrent.ExecutionContext

class CheckYourClaimDetailsController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  val controllerComponents: MessagesControllerComponents,
  view: CheckYourClaimDetailsView,
  configLanguageMapping: ConfigLanguageMapping,
  configCurrencyMapping: ConfigCurrencyMapping,
  sessionRepository: SessionRepository
)(using ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad(): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
    val summaryList = buildSummaryList(request.userAnswers)

    Ok(view(summaryList))
  }

  def onSubmit(): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    val updatedAnswers = request.userAnswers.set(ClaimDetailsCompletedPage, true).getOrElse(request.userAnswers)
    sessionRepository.set(updatedAnswers).map(_ => Redirect(controllers.routes.TaskListDashboardController.onPageLoad()))
  }

  private def buildSummaryList(
    answers: UserAnswers
  )(implicit messages: Messages): Seq[(String, Seq[(String, Option[String], Seq[(String, String, String)])])] = {

    val maybeCountryCode = answers.get(pages.RefundingCountryPage).orElse {
      answers.get(pages.RefundingCountryNamePage).map { stored =>
        stored.split(",", 2).headOption.getOrElse(stored)
      }
    }

    val maybeCurrencyDisplayName: Option[String] =
      answers.get(pages.RefundingCurrencyPage).map { code =>
        maybeCountryCode.toSeq
          .flatMap(configCurrencyMapping.currenciesFor)
          .find(_._2 == code)
          .map(c => messages(s"refundingCurrency.${c._1}", c._3))
          .getOrElse(code)
      }

    val languageSection: Seq[(String, Seq[(String, Option[String], Seq[(String, String, String)])])] =
      maybeCountryCode match {
        case Some(code) if configLanguageMapping.languagesFor(code).size > 1 =>
          Seq(("checkYourClaimDetails.refundingLanguage.label", Seq(CheckYourClaimDetailsSummary.rowLanguage(answers)).flatten))
        case _ => Seq.empty
      }

    val currencySection: Seq[(String, Seq[(String, Option[String], Seq[(String, String, String)])])] =
      maybeCountryCode match {
        case Some(code) if configCurrencyMapping.requiresCurrencySelection(code) =>
          Seq(("checkYourClaimDetails.refundingCurrency.label", Seq(CheckYourClaimDetailsSummary.rowCurrency(maybeCurrencyDisplayName)).flatten))
        case _ => Seq.empty
      }

    Seq(("checkYourClaimDetails.refundingCountry.label", Seq(CheckYourClaimDetailsSummary.rowCountry(answers)).flatten)) ++
      languageSection ++
      currencySection ++
      Seq(
        ("checkYourClaimDetails.refundingPeriod.label",
         Seq(CheckYourClaimDetailsSummary.rowRefundStart(answers), CheckYourClaimDetailsSummary.rowRefundEnd(answers)).flatten
        ),
        ("checkYourClaimDetails.contactDetails.label",
         Seq(CheckYourClaimDetailsSummary.rowContactEmail(answers), CheckYourClaimDetailsSummary.rowContactPhone(answers)).flatten
        ),
        ("checkYourClaimDetails.businessActivity.label",
         Seq(
           CheckYourClaimDetailsSummary.rowBusinessActivity(answers),
           CheckYourClaimDetailsSummary.rowBusinessActivity2(answers),
           CheckYourClaimDetailsSummary.rowBusinessActivity3(answers)
         ).flatten
        )
      )
  }
}
