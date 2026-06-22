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

import controllers.actions.*
import forms.BusinessActivityCodeThreeFormProvider
import models.{NormalMode, UserAnswers}
import models.Mode
import models.CheckMode
import navigation.Navigator
import pages.BusinessActivityCodeThreePage
 
import play.api.data.FormError
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
 
import views.html.BusinessActivityCodeThreeView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import play.api.Logger
import scala.util.control.NonFatal

class BusinessActivityCodeThreeController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: BusinessActivityCodeThreeFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: BusinessActivityCodeThreeView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
    val form = formProvider()
    val preparedForm = request.userAnswers.get(BusinessActivityCodeThreePage).fold(form)(form.fill)
    Ok(view(preparedForm, Some(routes.BusinessActivityTwoController.onPageLoad(mode).url), mode))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    val form = formProvider()
    val baseAnswers: UserAnswers = request.userAnswers

    val boundResult = form
      .bindFromRequest()
      .fold(
        formWithErrors => {
          val typed = request.body.asFormUrlEncoded.flatMap(_.get("valueTyped").flatMap(_.headOption)).getOrElse("")
          val adjustedForm = if (typed.trim.nonEmpty) {
            val filtered = formWithErrors.errors.filterNot(e => e.key == "value" && e.message == "businessActivityCodeThree.error.required")
            formWithErrors.copy(errors = filtered :+ FormError("value", "businessActivityCodeThree.error.invalid"))
          } else {
            formWithErrors
          }
          Future.successful(BadRequest(view(adjustedForm, Some(routes.BusinessActivityTwoController.onPageLoad(mode).url), mode)))
        },
        value => {
          for {
            updatedAnswers <- Future.fromTry(baseAnswers.set(BusinessActivityCodeThreePage, value))
            _              <- sessionRepository.set(updatedAnswers)
          } yield Redirect(navigator.nextPage(BusinessActivityCodeThreePage, mode, updatedAnswers))
        }
      )

    boundResult.recover { case NonFatal(e) =>
      Logger(getClass).error("Error in BusinessActivityCodeThreeController.onSubmit", e)
      BadRequest(view(form.bindFromRequest(), Some(routes.BusinessActivityTwoController.onPageLoad(mode).url), mode))
    }
  }
}
