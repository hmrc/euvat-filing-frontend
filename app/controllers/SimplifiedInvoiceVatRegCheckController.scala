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
import forms.SimplifiedInvoiceVatRegCheckFormProvider

import javax.inject.Inject
import models.{Mode, NormalMode}
import navigation.Navigator
import pages.{SimplifiedInvoiceVatRegCheckPage, SupplierAddressPage}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.SimplifiedInvoiceVatRegCheckView

import scala.concurrent.{ExecutionContext, Future}

class SimplifiedInvoiceVatRegCheckController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: SimplifiedInvoiceVatRegCheckFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: SimplifiedInvoiceVatRegCheckView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  val form = formProvider()
  private def backLink: play.api.mvc.Call = routes.SupplierAddressController.onPageLoad(NormalMode)

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) {
    implicit request =>

      request.userAnswers.get(SupplierAddressPage) match {
        case None => Redirect(routes.JourneyRecoveryController.onPageLoad())
        case Some(_) =>
          val preparedForm = request.userAnswers.get(SimplifiedInvoiceVatRegCheckPage) match {
            case None        => form
            case Some(value) => form.fill(value)
          }
          Ok(view(preparedForm, mode, backLink))
      }
  }


  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>

    form
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, mode, backLink))),
        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(SimplifiedInvoiceVatRegCheckPage, value))
            _              <- sessionRepository.set(updatedAnswers)
          } yield {
            if (value) {
              Redirect(routes.JourneyRecoveryController.onPageLoad()) // TODO: yes should route to "what is the suppliers VRN"
            } else {
              Redirect(routes.JourneyRecoveryController.onPageLoad()) // TODO: no should bypass "what is the suppliers VRN"
            }
          }
      )
  }
}
