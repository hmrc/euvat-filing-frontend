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

import controllers.actions.{DataRequiredAction, DataRetrievalAction, IdentifierAction}
import forms.ContactDetailsFormProvider
import models.{Mode, UserAnswers}
import navigation.Navigator
import pages.ContactDetailsPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.ContactDetailsView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ContactDetailsController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: ContactDetailsFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: ContactDetailsView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  private val form = formProvider()

  // LOCAL-DEV: `andThen requireData` temporarily removed until RA2.1 (DTR-4263)
  // lands and seeds UserAnswers. Restore by adding `andThen requireData` back and
  // replacing the `getOrElse(...)` with `request.userAnswers` once the upstream
  // page creates the session row. See docs/decisions.md.
  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData) { implicit request =>
    val answers = request.userAnswers.getOrElse(UserAnswers(request.userId))
    val preparedForm = answers.get(ContactDetailsPage) match {
      case None        => form
      case Some(value) => form.fill(value)
    }
    Ok(view(preparedForm, mode))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData).async { implicit request =>
    val answers = request.userAnswers.getOrElse(UserAnswers(request.userId))
    form.bindFromRequest().fold(
      formWithErrors => Future.successful(BadRequest(view(formWithErrors, mode))),
      value =>
        for {
          updatedAnswers <- Future.fromTry(answers.set(ContactDetailsPage, value))
          _              <- sessionRepository.set(updatedAnswers)
        } yield Redirect(navigator.nextPage(ContactDetailsPage, mode, updatedAnswers))
    )
  }
}
