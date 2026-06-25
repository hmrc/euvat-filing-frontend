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

import config.FrontendAppConfig
import controllers.actions.*
import models.UserAnswers

import javax.inject.Inject
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.TaskListDashboardView
import pages.RefundingCountryPage
import models.NormalMode
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.tag.Tag
import uk.gov.hmrc.govukfrontend.views.viewmodels.tasklist.{TaskList, TaskListItem, TaskListItemStatus, TaskListItemTitle}
import scala.concurrent.ExecutionContext

class TaskListDashboardController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  sessionRepository: SessionRepository,
  appConfig: FrontendAppConfig,
  val controllerComponents: MessagesControllerComponents,
  view: TaskListDashboardView
)(using ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  def onPageLoad: Action[AnyContent] = (identify andThen getData).async { implicit request =>
    implicit val messages: Messages = messagesApi.preferred(request)
    val originalAnswers = request.userAnswers.getOrElse(UserAnswers(request.userId))

    val detailsDone = originalAnswers.get(RefundingCountryPage).isDefined

    val claimDetailsItem = TaskListItem(
      title = TaskListItemTitle(content =
        Text(
          if (detailsDone) messages("taskListDashboard.listItem1.completed")
          else messages("taskListDashboard.listItem1")
        )
      ),
      status =
        if (detailsDone) TaskListItemStatus(content = Text(messages("taskListDashboard.status3")))
        else
          TaskListItemStatus(
            tag = Some(Tag(content = Text(messages("taskListDashboard.status1"))))
          )
        val taskList = TaskList(items = Seq(claimDetailsItem), idPrefix = "make-eu-vat-claim")
    )
    sessionRepository.set(originalAnswers).map(_ => Ok(view(taskList)))
  }

  // Clear session before calling the manage frontend
  def redirectToManageClaim: Action[AnyContent] = (identify andThen getData).async { implicit request =>
    val clearSessionAnswer = request.userAnswers.getOrElse(UserAnswers(request.userId)).clear()
    sessionRepository.set(clearSessionAnswer).map(_ => Redirect(appConfig.claimDashboardUrl))
  }

}
