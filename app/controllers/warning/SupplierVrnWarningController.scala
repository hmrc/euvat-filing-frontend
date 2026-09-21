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
import models.{CheckMode, Mode, NormalMode}
import navigation.Navigator
import pages.{SupplierVatRegistrationWarningPage, TotalPurchaseAmountBeforeVatPage}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.{CountryCode, CurrencyConfig}
import views.html.warning.SupplierVrnWarningView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SupplierVrnWarningController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  navigator: Navigator,
  val controllerComponents: MessagesControllerComponents,
  view: SupplierVrnWarningView,
  currencyConfig: CurrencyConfig
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    for {
      updatedAnswers <- Future.fromTry(request.userAnswers.set(SupplierVatRegistrationWarningPage, true))
      _              <- sessionRepository.set(updatedAnswers)
    } yield Ok(view(mode, routes.SupplierVatRegistrationNumberController.onPageLoad(CheckMode), routes.InvoiceNumberController.onPageLoad(CheckMode)))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    for {
      userAnswers <- Future.fromTry(request.userAnswers.set(SupplierVatRegistrationWarningPage, true))
      _           <- sessionRepository.set(userAnswers)
    } yield {
      if (userAnswers.get(TotalPurchaseAmountBeforeVatPage).isDefined) {
        Redirect(routes.CheckYourPurchaseDetailsController.onPageLoad())
      } else {
        CountryCode.findCountryCode(userAnswers) match {
          case Some(countryCode) if currencyConfig.requiresCurrencySelection(countryCode) =>
            Redirect(routes.RefundingCurrencyController.onPageLoad(NormalMode))
          case _ => Redirect(routes.TotalPurchaseAmountBeforeVatController.onPageLoad(NormalMode))
        }
      }
    }
  }
}
