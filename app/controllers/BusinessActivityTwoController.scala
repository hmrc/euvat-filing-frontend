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
import forms.BusinessActivityTwoFormProvider
import models.{NormalMode, UserAnswers}
import navigation.Navigator
import pages.BusinessActivityTwoPage
import play.api.Configuration
import play.api.data.FormError
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.BusinessActivityList
import views.html.BusinessActivityTwoView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import play.api.Logger
import scala.util.control.NonFatal

class BusinessActivityTwoController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: BusinessActivityTwoFormProvider,
  config: Configuration,
  val controllerComponents: MessagesControllerComponents,
  view: BusinessActivityTwoView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  private def buildListAndForm() = {
    val activities = BusinessActivityList.fromConfig(config)
    val allowed: Set[String] = activities.flatMap { case (name, code) => Seq(code, s"$code ($name)") }.toSet
    val form = formProvider(allowed)
    (activities, form)
  }

  // Allow access even when there is no existing UserAnswers (for dev/debug).
  def onPageLoad(): Action[AnyContent] = (identify andThen getData) { implicit request =>

    val (activities, form) = buildListAndForm()

    val preparedForm = request.userAnswers.flatMap(_.get(BusinessActivityTwoPage)).fold(form)(form.fill)

    Ok(view(preparedForm, activities, None))
  }

  def onSubmit(): Action[AnyContent] = (identify andThen getData).async { implicit request =>

    val (activities, form) = buildListAndForm()

    val baseAnswers: UserAnswers = request.userAnswers.getOrElse(UserAnswers(request.userId))

    val boundResult = form
      .bindFromRequest()
      .fold(
        formWithErrors => {
          val typed = request.body.asFormUrlEncoded.flatMap(_.get("valueTyped").flatMap(_.headOption)).getOrElse("")
          val adjustedForm = if (typed.trim.nonEmpty) {
            val filtered = formWithErrors.errors.filterNot(e => e.key == "value" && e.message == "businessActivityTwo.error.required")
            formWithErrors.copy(errors = filtered :+ FormError("value", "businessActivityTwo.error.invalid"))
          } else {
            formWithErrors
          }
          Future.successful(BadRequest(view(adjustedForm, activities, None)))
        },
        value => {
          for {
            updatedAnswers <- Future.fromTry(baseAnswers.set(BusinessActivityTwoPage, value))
            _              <- sessionRepository.set(updatedAnswers)
          } yield Redirect(navigator.nextPage(BusinessActivityTwoPage, NormalMode, updatedAnswers))
        }
      )

    boundResult.recover { case NonFatal(e) =>
      Logger(getClass).error("Error in BusinessActivityTwoController.onSubmit", e)
      BadRequest(view(form.bindFromRequest(), activities, None))
    }
  }
}
