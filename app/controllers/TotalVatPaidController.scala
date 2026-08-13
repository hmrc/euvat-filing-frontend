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
import forms.TotalVatPaidFormProvider

import javax.inject.Inject
import models.Mode
import navigation.Navigator
import utils.{ConfigCurrencyMapping, RefundingAndPurchaseUtils}
import pages.{RefundingCurrencyPage, TotalPurchaseAmountBeforeVatPage, TotalVatPaidPage}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.TotalVatPaidView

import scala.concurrent.{ExecutionContext, Future}

class TotalVatPaidController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  refundingAndPurchaseUtils: RefundingAndPurchaseUtils,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: TotalVatPaidFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: TotalVatPaidView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  val form: Form[BigDecimal] = formProvider()

  private def backLink(mode: Mode) = routes.TotalPurchaseAmountBeforeVatController.onPageLoad(mode)

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
    val preparedForm = request.userAnswers.get(TotalVatPaidPage) match {
      case None        => form
      case Some(value) => form.fill(value)
    }

    val (currencyName, prefix) = refundingAndPurchaseUtils.resolveCurrency(request.userAnswers)
    Ok(view(preparedForm, mode, backLink(mode), prefix, currencyName))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    form
      .bindFromRequest()
      .fold(
        formWithErrors => {
          val (currencyName, prefix) = refundingAndPurchaseUtils.resolveCurrency(request.userAnswers)
          Future.successful(BadRequest(view(formWithErrors, mode, backLink(mode), prefix, currencyName)))
        },
        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(TotalVatPaidPage, value))
            _              <- sessionRepository.set(updatedAnswers)
          } yield {
            val totalPurchaseAmt: BigDecimal = request.userAnswers.get(TotalPurchaseAmountBeforeVatPage).getOrElse(BigDecimal(0))
            if (value >= totalPurchaseAmt) {
              Redirect(routes.VatPaidWarningController.onPageLoad(mode))
            } else {
              Redirect(navigator.nextPage(TotalVatPaidPage, mode, updatedAnswers))
            }
          }
      )
  }

}
