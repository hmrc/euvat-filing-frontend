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
import controllers.imports.routes as importRoutes
import models.Mode
import models.PurchaseOrImport.{Import, Purchase}
import pages.PurchaseOrImportPage
import pages.imports.{ImportTotalVatClaimPage, TotalVatPaidImportPage}
import models.requests.DataRequest
import pages.purchase.{TotalVatClaimPage, TotalVatPaidPage}
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.ControllerHelpers.*
import utils.CurrencyConfig
import controllers.routes.JourneyRecoveryController
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
    val answers = request.userAnswers
    val journeyAnswers = answers.get(PurchaseOrImportPage) match {
      case Some(Purchase) =>
        (answers.get(TotalVatPaidPage), answers.get(TotalVatClaimPage), routes.TotalVatClaimController.onPageLoad(mode))
      case Some(Import) =>
        (answers.get(TotalVatPaidImportPage), answers.get(ImportTotalVatClaimPage), importRoutes.TotalVatClaimController.onPageLoad(mode))
      case None =>
        (None, None, JourneyRecoveryController.onPageLoad())
    }

    journeyAnswers match {
      case (Some(_), Some(totalVatClaiming), changeLink) =>
        val currencySymbol = currencySymbolFromSession(answers, currencyConfig.currencyConfig)
        okView(changeLink, mode, currencySymbol, totalVatClaiming)
      case _ =>
        logger.warn("Missing session data")
        Redirect(JourneyRecoveryController.onPageLoad())
    }
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
    request.userAnswers.get(PurchaseOrImportPage) match {
      case Some(Purchase) => Redirect(navigator.nextPage(TotalVatClaimPage, mode, request.userAnswers))
      case Some(Import)   => Redirect(navigator.nextPage(ImportTotalVatClaimPage, mode, request.userAnswers))
      case None           => Redirect(JourneyRecoveryController.onPageLoad())
    }
  }

  private def okView(returnCall: Call, mode: Mode, currencySymbol: String, totalVatClaiming: BigDecimal)(implicit request: DataRequest[?]) =
    Ok(view(returnCall, mode, currencySymbol, totalVatClaiming))
}
