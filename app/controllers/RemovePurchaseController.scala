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

import controllers.actions._
import forms.RemovePurchaseFormProvider
import javax.inject.Inject
import models.Mode
import navigation.Navigator
import pages.{PurchaseTypePage, RemovePurchasePage, TotalVatClaimPage}
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import utils.ConfigCurrencyMapping
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.RemovePurchaseView

import scala.concurrent.{ExecutionContext, Future}

class RemovePurchaseController @Inject()(
                                          sessionRepository: SessionRepository,
                                          navigator: Navigator,
                                          identify: IdentifierAction,
                                          getData: DataRetrievalAction,
                                          requireData: DataRequiredAction,
                                          formProvider: RemovePurchaseFormProvider,
                                          configCurrencyMapping: ConfigCurrencyMapping,
                                          val controllerComponents: MessagesControllerComponents,
                                          view: RemovePurchaseView
                                        )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  val form = formProvider()

  // Duplicated from TotalVatClaimController.resolveCurrencyPrefix (private there) —
  // worth pulling into a shared util if a third page ends up needing this.
  private def resolveCurrencyPrefix(userAnswers: models.UserAnswers): String = {
    val maybeCountry = userAnswers.get(pages.RefundingCountryPage).orElse {
      userAnswers.get(pages.RefundingCountryNamePage).map { stored =>
        stored.split(",", 2).headOption.getOrElse(stored)
      }
    }

    val defaultSymbol = "€"

    maybeCountry match {
      case None => defaultSymbol
      case Some(countryCode) =>
        userAnswers.get(pages.RefundingCurrencyPage) match {
          case Some(currencyCode) =>
            configCurrencyMapping
              .currenciesFor(countryCode)
              .find(_._2 == currencyCode)
              .map(_._3)
              .getOrElse(configCurrencyMapping.currenciesFor(countryCode).headOption.map(_._3).getOrElse(defaultSymbol))
          case None =>
            configCurrencyMapping.currenciesFor(countryCode).headOption.map(_._3).getOrElse(defaultSymbol)
        }
    }
  }

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) {
    implicit request =>

      implicit val messages: Messages = messagesApi.preferred(request)

      val preparedForm = request.userAnswers.get(RemovePurchasePage) match {
        case None => form
        case Some(value) => form.fill(value)
      }

      val purchaseType = request.userAnswers.get(PurchaseTypePage)
        .map(pt => messages(s"purchaseType.$pt"))
        .getOrElse("")

      val vatClaiming = request.userAnswers.get(TotalVatClaimPage)
        .map(amount => s"${resolveCurrencyPrefix(request.userAnswers)}$amount")
        .getOrElse("")

      Ok(view(preparedForm, mode, purchaseType, vatClaiming))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>

      implicit val messages: Messages = messagesApi.preferred(request)

      form.bindFromRequest().fold(
        formWithErrors => {
          val purchaseType = request.userAnswers.get(PurchaseTypePage)
            .map(pt => messages(s"purchaseType.$pt"))
            .getOrElse("")

          val vatClaiming = request.userAnswers.get(TotalVatClaimPage)
            .map(amount => s"${resolveCurrencyPrefix(request.userAnswers)}$amount")
            .getOrElse("")

          Future.successful(BadRequest(view(formWithErrors, mode, purchaseType, vatClaiming)))
        },

        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(RemovePurchasePage, value))
            _ <- sessionRepository.set(updatedAnswers)
          } yield Redirect(navigator.nextPage(RemovePurchasePage, mode, updatedAnswers))
      )
  }
}