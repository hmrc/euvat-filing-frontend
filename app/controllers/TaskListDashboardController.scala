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
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.taskList.ClaimDetailsStatus
import views.html.TaskListDashboardView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class TaskListDashboardController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  sessionRepository: SessionRepository,
  appConfig: FrontendAppConfig,
  val controllerComponents: MessagesControllerComponents,
  view: TaskListDashboardView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad: Action[AnyContent] = (identify andThen getData).async { implicit request =>
    val userAnswersF: Future[UserAnswers] = request.userAnswers match {
      case Some(existing) =>
        Future.successful(existing)
      case None           =>
        val seeded = UserAnswers(request.userId)
        logger.info("[TaskListDashboardController] seeding UserAnswers for new user")
        sessionRepository.set(seeded).map(_ => seeded)
    }

    userAnswersF.map { userAnswers =>
      val claimDetailsStatus = ClaimDetailsStatus.from(userAnswers)
      Ok(view(appConfig.claimDashboardUrl, claimDetailsStatus))
    }
  }
}
