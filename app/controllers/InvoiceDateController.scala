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
import forms.InvoiceDateFormProvider
import models.Mode
import navigation.Navigator
import pages.InvoiceDatePage
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.InvoiceDateView

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class InvoiceDateController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: InvoiceDateFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: InvoiceDateView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  private def form(implicit messages: Messages) = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
    request.userAnswers.get(pages.RefundPeriodPage) match {
      case None => Redirect(routes.JourneyRecoveryController.onPageLoad())
      case Some(_) =>
        val preparedForm = request.userAnswers.get(InvoiceDatePage) match {
          case None        => form
          case Some(value) => form.fill(value)
        }
        Ok(view(preparedForm, mode))
    }
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    form
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, mode))),
        value =>
          request.userAnswers.get(pages.RefundPeriodPage) match {
            case None => Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
            case Some(refundPeriod) =>
              val today = java.time.LocalDate.now()
              if (value.isAfter(today)) {
                val errorForm = form.bindFromRequest().withError("value", "invoiceDate.error.past")
                Future.successful(BadRequest(view(errorForm, mode)))
              }
              /*
              TODO: business rule to prevent users entering a date outside the refund period
              this is currently commented out as
              it is not yet clear whether this validation will be possible at this point in the journey.
              Once the refund period can be calculated, this validation should be added back in
              and the relevant error message added to the messages file.

              else if (value.isBefore(refundPeriod.startDate) || value.isAfter(refundPeriod.endDate)) {
                val errorForm = form.withError("value", "invoiceDate.error.outsideRefundPeriod")
                Future.successful(BadRequest(view(errorForm, mode)))
              } */
              else {
                for {
                  updatedAnswers <- Future.fromTry(request.userAnswers.set(InvoiceDatePage, value))
                  _              <- sessionRepository.set(updatedAnswers)
                } yield Redirect(navigator.nextPage(InvoiceDatePage, mode, updatedAnswers))
              }
          }
      )
  }
}
