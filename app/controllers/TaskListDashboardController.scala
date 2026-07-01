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
import viewmodels.TaskListViewModel

import scala.concurrent.{ExecutionContext, Future}

class TaskListDashboardController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  sessionRepository: SessionRepository,
  appConfig: FrontendAppConfig,
  taskListViewModel: TaskListViewModel,
  val controllerComponents: MessagesControllerComponents,
  view: TaskListDashboardView
)(using ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  def onPageLoad: Action[AnyContent] = (identify andThen getData).async { implicit request =>
    implicit val messages: Messages = messagesApi.preferred(request)
    val originalAnswers = request.userAnswers.getOrElse(UserAnswers(request.userId))
    val taskList = taskListViewModel.buildTaskList(originalAnswers)
    val deleteLink = taskListViewModel.showDeleteLink(originalAnswers)
    request.userAnswers match {
      case Some(answers) => sessionRepository.set(answers).map(_ => Ok(view(taskList, deleteLink)))
      case None          => Future.successful(Ok(view(taskList, deleteLink)))
    }
  }

  // Clear session before calling the manage frontend
  def redirectToManageClaim: Action[AnyContent] = (identify andThen getData).async { implicit request =>
    val clearSessionAnswer = request.userAnswers.getOrElse(UserAnswers(request.userId)).clear()
    sessionRepository.set(clearSessionAnswer).map(_ => Redirect(appConfig.claimDashboardUrl))
  }

}
