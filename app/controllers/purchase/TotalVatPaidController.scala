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
import forms.purchase.TotalVatPaidFormProvider
import models.{CheckMode, Mode, NormalMode}
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

  private def backLink(mode: Mode) = if (mode == CheckMode) {
    routes.CheckYourPurchaseDetailsController.onPageLoad()
  } else {
    routes.TotalPurchaseAmountBeforeVatController.onPageLoad(NormalMode)
  }

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
    val preparedForm = request.userAnswers.get(TotalVatPaidPage).fold(form)(form.fill)
    val (currencyName, prefix) = currencyNameAndPrefix(request.userAnswers, currencyConfig.currencyConfig)
    Ok(view(preparedForm, mode, backLink(mode), prefix, currencyName))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    val (currencyName, prefix) = currencyNameAndPrefix(request.userAnswers, currencyConfig.currencyConfig)
    form
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, mode, backLink(mode), prefix, currencyName))),
        value =>
          for {
            userAnswers <- Future.fromTry(request.userAnswers.set(TotalVatPaidPage, value))
            _           <- sessionRepository.set(userAnswers)
          } yield {
            val amountBeforeVat: BigDecimal = userAnswers.get(TotalPurchaseAmountBeforeVatPage).getOrElse(BigDecimal(0))
            if (value > amountBeforeVat) {
              Redirect(controllers.warning.routes.VatPaidWarningController.onPageLoad(mode))
            } else {
              Redirect(navigator.nextPage(TotalVatPaidPage, mode, userAnswers))
            }
          }
      )
  }

}
