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
import forms.InvoiceTypeFormProvider
import javax.inject.Inject
import models.Mode
import navigation.Navigator
import pages.InvoiceTypePage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.InvoiceTypeView
import play.api.mvc.Call

import scala.concurrent.{ExecutionContext, Future}

class InvoiceTypeController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: InvoiceTypeFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: InvoiceTypeView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  val form = formProvider()

  private def backLink: Call = routes.PurchaseTypeController.onPageLoad(NormalMode)

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>

    val preparedForm = request.userAnswers.get(InvoiceTypePage) match {
      case None        => form
      case Some(value) => form.fill(value)
    }

    Ok(view(preparedForm, mode, backLink))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>

    form
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, mode, backLink))),
        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(InvoiceTypePage, value))
            _              <- sessionRepository.set(updatedAnswers)
          } yield Redirect(navigator.nextPage(InvoiceTypePage, mode, updatedAnswers))
      )
  }
}
