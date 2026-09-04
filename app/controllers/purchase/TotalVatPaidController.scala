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

package controllers.purchase

import controllers.actions.*
import controllers.purchase.routes
import forms.purchase.TotalVatPaidFormProvider
import models.Mode
import models.requests.DataRequest
import navigation.Navigator
import pages.{TotalPurchaseAmountBeforeVatPage, TotalVatPaidPage}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.ControllerHelpers.*
import utils.CurrencyConfig
import views.html.purchase.TotalVatPaidView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class TotalVatPaidController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  currencyConfig: CurrencyConfig,
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
    val preparedForm = request.userAnswers.get(TotalVatPaidPage).fold(form)(form.fill)
    val (currencyName, prefix) = currencyNameAndPrefix(request.userAnswers, currencyConfig.currencyConfig)
    Ok(view(preparedForm, mode, backLink(mode), prefix, currencyName))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    form
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(badRequestView(formWithErrors, mode)),
        value => handleSubmit(value, mode)
      )
  }

  private def handleSubmit(value: BigDecimal, mode: Mode)(implicit request: DataRequest[?]) = {
    shortCircuit(
      TotalVatPaidPage,
      value,
      mode,
      request.userAnswers,
      navigator.nextPage(TotalVatPaidPage, mode, request.userAnswers),
      routes.CheckYourPurchaseDetailsController.onPageLoad(),
      Some(sessionRepository)
    ) { updated =>
      if (compareWithPage(value, TotalPurchaseAmountBeforeVatPage, updated)(_ >= _)) {
        Future.successful(Redirect(controllers.warning.routes.VatPaidWarningController.onPageLoad(mode)))
      } else {
        Future.successful(Redirect(navigator.nextPage(TotalVatPaidPage, mode, updated)))
      }
    }
  }

  private def badRequestView(formWithErrors: Form[?], mode: Mode)(implicit request: DataRequest[?]) = {
    val (currencyName, prefix) = currencyNameAndPrefix(request.userAnswers, currencyConfig.currencyConfig)
    BadRequest(view(formWithErrors, mode, backLink(mode), prefix, currencyName))
  }
}
