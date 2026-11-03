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

package controllers.bankdetails

import controllers.actions.{DataRequiredAction, DataRetrievalAction, IdentifierAction}
import models.CheckMode
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.*
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.checkAnswers.CheckYourBankDetailsSummary
import views.html.bankdetails.CheckYourBankDetailsView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CheckYourBankDetailsController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  val controllerComponents: MessagesControllerComponents,
  view: CheckYourBankDetailsView
)(using ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad(): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
    val rows = Seq(
      CheckYourBankDetailsSummary.rowAccountHolder(request.userAnswers)
      // add the other four rows once available
    ).flatten

    val changeUrl = controllers.bankdetails.routes.AccountHolderController.onPageLoad(CheckMode).url
    Ok(view(rows, changeUrl))
  }

  def onSubmit(): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    // TODO F5.2: build request (BIC/IBAN encrypted via your F5.3 utils), call service,
    // update UpdateSequenceNumberQuery, save session — same shape as updateClaimDetailsAndRedirect
    Future.successful(Redirect(controllers.routes.TaskListDashboardController.onPageLoad()))
  }
}
