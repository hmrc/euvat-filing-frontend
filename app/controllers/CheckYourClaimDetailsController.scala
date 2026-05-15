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
import pages.RefundingCountryPage
import play.api.Logging
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.govukfrontend.views.Aliases.SummaryList
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.checkAnswers.CheckYourClaimsSummary
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
    val ua = request.userAnswers
    val euCountry = ua.get(RefundingCountryPage)
    val summaryList = buildSummaryList(ua)

    Ok(view(summaryList))
  }

  def onSubmit(): Action[AnyContent] = Action { implicit request =>
    Redirect(controllers.routes.TaskListDashboardController.onPageLoad())
  }

  private def buildSummaryList(answers: models.UserAnswers)(implicit messages: Messages): SummaryList =
    SummaryListViewModel(
      rows = Seq(
        CheckYourClaimsSummary.rowCountryLabel(answers),
        CheckYourClaimsSummary.rowCountry(answers),
        CheckYourClaimsSummary.rowLanguageLabel(answers),
        CheckYourClaimsSummary.rowLanguage(answers),
        CheckYourClaimsSummary.rowRefundPeriodLabel(answers),
        CheckYourClaimsSummary.rowRefundStart(answers),
        CheckYourClaimsSummary.rowRefundEnd(answers),
        CheckYourClaimsSummary.rowContactLabel(answers),
        CheckYourClaimsSummary.rowContactEmail(answers),
        CheckYourClaimsSummary.rowContactTelephone(answers),
        CheckYourClaimsSummary.rowBusinessActivityLabel(answers),
        CheckYourClaimsSummary.rowBusinessActivity1(answers),
        CheckYourClaimsSummary.rowBusinessActivity2(answers),
        CheckYourClaimsSummary.rowBusinessActivity3(answers)
      ).flatten
    )

}
