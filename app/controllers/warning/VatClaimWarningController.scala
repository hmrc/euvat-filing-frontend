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

package controllers.warning

import controllers.actions.*
import controllers.purchase.routes
import models.Mode
import models.requests.DataRequest
import pages.*
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.ControllerHelpers.*
import utils.CurrencyConfig
import views.html.warning.VatClaimWarningView

import javax.inject.Inject

class VatClaimWarningController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  currencyConfig: CurrencyConfig,
  val controllerComponents: MessagesControllerComponents,
  navigator: navigation.Navigator,
  view: VatClaimWarningView
) extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
    (request.userAnswers.get(TotalVatPaidPage), request.userAnswers.get(TotalVatClaimPage)) match {
      case (Some(_), Some(totalVatClaiming)) =>
        val currencySymbol = currencySymbolFromSession(request.userAnswers, currencyConfig.currencyConfig)
        okView(routes.TotalVatClaimController.onPageLoad(mode), mode, currencySymbol, totalVatClaiming)

      case _ =>
        logger.warn("Missing session data")
        Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
    }
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
    Redirect(navigator.nextPage(TotalVatClaimPage, mode, request.userAnswers))
  }

  private def okView(returnCall: Call, mode: Mode, currencySymbol: String, totalVatClaiming: BigDecimal)(implicit request: DataRequest[?]) =
    Ok(view(returnCall, mode, currencySymbol, totalVatClaiming))
}
