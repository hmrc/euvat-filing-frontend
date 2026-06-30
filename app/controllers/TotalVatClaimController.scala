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
import forms.TotalVatClaimFormProvider

import javax.inject.Inject
import models.{Mode, NormalMode, UserAnswers}
import navigation.Navigator
import pages.{RefundingCountryNamePage, RefundingCountryPage, RefundingCurrencyPage, TotalVatClaimPage}
import utils.ConfigCurrencyMapping
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.TotalVatClaimView

import scala.concurrent.{ExecutionContext, Future}

class TotalVatClaimController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: TotalVatClaimFormProvider,
  configCurrencyMapping: ConfigCurrencyMapping,
  val controllerComponents: MessagesControllerComponents,
  view: TotalVatClaimView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  val form = formProvider()

  private def backLink(mode: Mode): Call = routes.TotalVatPaidController.onPageLoad(mode)

  // TODO - duplicated from RefundingCurrencyController; consider extracting to a shared helper (see DTR-4294)
  private def resolveCountryCode(userAnswers: UserAnswers): Option[String] =
    userAnswers.get(RefundingCountryPage).orElse {
      userAnswers.get(RefundingCountryNamePage).map { stored =>
        stored.split(",", 2).headOption.getOrElse(stored)
      }
    }

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>

    val preparedForm = request.userAnswers.get(TotalVatClaimPage) match {
      case None        => form
      case Some(value) => form.fill(value)
    }

    val currencySymbol = resolveCountryCode(request.userAnswers)
      .map(configCurrencyMapping.currenciesFor)
      .flatMap { currencies =>
        request.userAnswers
          .get(RefundingCurrencyPage)
          .flatMap(code => currencies.find(_._2 == code).map(_._3))
      }
      .getOrElse("€")

    Ok(view(preparedForm, mode, backLink(mode), currencySymbol))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>

    val currencySymbol = resolveCountryCode(request.userAnswers)
      .map(configCurrencyMapping.currenciesFor)
      .flatMap { currencies =>
        request.userAnswers
          .get(RefundingCurrencyPage)
          .flatMap(code => currencies.find(_._2 == code).map(_._3))
      }
      .getOrElse("€")

    form
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, mode, backLink(mode), currencySymbol))),
        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(TotalVatClaimPage, value))
            _              <- sessionRepository.set(updatedAnswers)
          } yield Redirect(navigator.nextPage(TotalVatClaimPage, mode, updatedAnswers))
      )
  }
}
