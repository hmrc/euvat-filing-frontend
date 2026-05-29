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
import pages.RefundingCountryPage
import play.api.Logging
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.Configuration
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.govukfrontend.views.Aliases.SummaryList
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.checkAnswers.CheckYourClaimDetailsSummary
import views.html.CheckYourClaimDetailsView
import viewmodels.govuk.summarylist.*

class CheckYourClaimDetailsController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  val controllerComponents: MessagesControllerComponents,
  view: CheckYourClaimDetailsView
) extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad(): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
    val summaryList = buildSummaryList(request.userAnswers)

    Ok(view(summaryList))
  }

  def onSubmit(): Action[AnyContent] = Action { implicit request =>
    Redirect(controllers.routes.TaskListDashboardController.onPageLoad())
  }

  private def getChangeUrl(rowOpt: Option[SummaryListRow]): Option[String] =
    rowOpt.flatMap(_.actions.flatMap(_.items.headOption.map(_.href)))

  private def buildSummaryList(answers: UserAnswers)(implicit messages: Messages): Seq[(String, Option[String], SummaryList)] =
    Seq(
      (
        "checkYourClaimDetails.refundingCountry.label",
        getChangeUrl(CheckYourClaimDetailsSummary.rowCountryLabel(answers)),
        SummaryListViewModel(Seq(CheckYourClaimDetailsSummary.rowCountry(answers)).flatten)
      ),
      (
        "checkYourClaimDetails.refundingLanguage.label",
        getChangeUrl(CheckYourClaimDetailsSummary.rowLanguageLabel(answers)),
        SummaryListViewModel(Seq(CheckYourClaimDetailsSummary.rowLanguage(answers)).flatten)
      ),
      (
        "checkYourClaimDetails.refundingPeriod.label",
        getChangeUrl(CheckYourClaimDetailsSummary.rowRefundPeriodLabel(answers)),
        SummaryListViewModel(Seq(CheckYourClaimDetailsSummary.rowRefundStart(answers), CheckYourClaimDetailsSummary.rowRefundEnd(answers)).flatten)
      ),
      (
        "checkYourClaimDetails.contactDetails.label",
        getChangeUrl(CheckYourClaimDetailsSummary.rowContactLabel(answers)),
        SummaryListViewModel(Seq(CheckYourClaimDetailsSummary.rowContactEmail(answers), CheckYourClaimDetailsSummary.rowContactTelephone(answers)).flatten)
      ),
      (
        "checkYourClaimDetails.businessActivity.label",
        getChangeUrl(CheckYourClaimDetailsSummary.rowBusinessActivityLabel(answers)),
        SummaryListViewModel(
          Seq(
            CheckYourClaimDetailsSummary.rowBusinessActivity(answers),
            CheckYourClaimDetailsSummary.rowBusinessActivity2(answers),
            CheckYourClaimDetailsSummary.rowBusinessActivity3(answers)
          ).flatten
        )
      )
    )

}
