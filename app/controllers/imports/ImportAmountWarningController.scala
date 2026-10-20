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

package controllers.imports

import controllers.actions.{DataRequiredAction, DataRetrievalAction, IdentifierAction}
import models.Mode
import navigation.ImportNavigator
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.ControllerHelpers.currencySymbolFromSession
import utils.CurrencyConfig
import views.html.warning.{VatClaimWarningView, VatPaidWarningView}

import javax.inject.Inject

class ImportAmountWarningController @Inject() (
                                                override val messagesApi: MessagesApi,
                                                identify: IdentifierAction,
                                                getData: DataRetrievalAction,
                                                requireData: DataRequiredAction,
                                                val controllerComponents: MessagesControllerComponents,
                                                importNavigator: ImportNavigator,
                                                currencyConfig: CurrencyConfig,
                                                vatPaidView: VatPaidWarningView,
                                                vatClaimView: VatClaimWarningView
                                              ) extends FrontendBaseController
  with I18nSupport {

  def onVatPaidPageLoad(mode: Mode): Action[AnyContent] =
    (identify andThen getData andThen requireData) { implicit request =>
      importNavigator.vatPaidWarningPage(mode)(request.userAnswers).fold(
        destination => Redirect(destination),
        _ =>
          Ok(
            vatPaidView(
              routes.TotalVatPaidImportController.onPageLoad(mode),
              mode,
              Some(
                routes.ImportAmountWarningController.onVatPaidSubmit(mode)
              )
            )
          )
      )
    }

  def onVatPaidSubmit(mode: Mode): Action[AnyContent] =
    (identify andThen getData andThen requireData) { implicit request =>
      Redirect(
        importNavigator.navigateFromVatPaidWarning(mode)(request.userAnswers)
      )
    }

  def onVatClaimPageLoad(mode: Mode): Action[AnyContent] =
    (identify andThen getData andThen requireData) { implicit request =>
      importNavigator.vatClaimWarningPage(mode)(request.userAnswers).fold(
        destination => Redirect(destination),
        claim => {
          val currencySymbol = currencySymbolFromSession(
            request.userAnswers,
            currencyConfig.currencyConfig
          )

          Ok(
            vatClaimView(
              routes.TotalVatClaimController.onPageLoad(mode),
              mode,
              currencySymbol,
              claim,
              Some(
                routes.ImportAmountWarningController.onVatClaimSubmit(mode)
              )
            )
          )
        }
      )
    }

  def onVatClaimSubmit(mode: Mode): Action[AnyContent] =
    (identify andThen getData andThen requireData) { implicit request =>
      Redirect(
        importNavigator.navigateFromVatClaimWarning(mode)(request.userAnswers)
      )
    }
}
