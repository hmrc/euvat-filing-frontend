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
import forms.purchase.TotalVatClaimFormProvider
import models.{CheckMode, Mode, NormalMode}
import navigation.Navigator
import pages.{TotalVatClaimPage, TotalVatPaidPage}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.ControllerHelpers.currencySymbolFromSession
import utils.CurrencyConfig
import views.html.purchase.TotalVatClaimView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class TotalVatClaimController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: TotalVatClaimFormProvider,
  currencyConfig: CurrencyConfig,
  val controllerComponents: MessagesControllerComponents,
  view: TotalVatClaimView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  val form: Form[BigDecimal] = formProvider()

  private def backLink(mode: Mode): Call = if (mode == CheckMode) {
    routes.CheckYourPurchaseDetailsController.onPageLoad()
  } else {
    routes.TotalVatPaidController.onPageLoad(NormalMode)
  }

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
    val preparedForm = request.userAnswers.get(TotalVatClaimPage).fold(form)(form.fill)
    val currencySymbol = currencySymbolFromSession(request.userAnswers, currencyConfig.currencyConfig)
    Ok(view(preparedForm, mode, backLink(mode), currencySymbol))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    form
      .bindFromRequest()
      .fold(
        formWithErrors =>
          Future.successful(
            BadRequest(view(formWithErrors, mode, backLink(mode), currencySymbolFromSession(request.userAnswers, currencyConfig.currencyConfig)))
          ),
        value =>
          for {
            userAnswers <- Future.fromTry(request.userAnswers.set(TotalVatClaimPage, value))
            _           <- sessionRepository.set(userAnswers)
          } yield {
            val totalVatPaid: BigDecimal = userAnswers.get(TotalVatPaidPage).getOrElse(BigDecimal(0))
            if (value > totalVatPaid) {
              Redirect(controllers.warning.routes.VatClaimWarningController.onPageLoad(mode))
            } else {
              Redirect(navigator.nextPage(TotalVatClaimPage, mode, userAnswers))
            }
          }
      )
  }

}
