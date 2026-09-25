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

import controllers.actions.*
import controllers.imports.routes as importRoutes
import forms.imports.TotalAmountWithoutVatFormProvider
import models.Mode
import navigation.Navigator
import pages.{SadReferenceCheckPage, SadReferenceNumberPage, TotalAmountWithoutVatPage}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.ControllerHelpers.*
import utils.{CountryCode, CurrencyConfig}
import views.html.imports.TotalAmountWithoutVatView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class TotalAmountWithoutVatController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  currencyConfig: CurrencyConfig,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: TotalAmountWithoutVatFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: TotalAmountWithoutVatView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  val form: Form[BigDecimal] = formProvider()

  private def backLink(mode: Mode)(userAnswers: models.UserAnswers): Call =
    CountryCode.findCountryCode(userAnswers) match {
      case Some(country) if currencyConfig.requiresCurrencySelection(country) => importRoutes.ImportCurrencyController.onPageLoad(mode)
      case Some(_)                                                            => importRoutes.ImportSuppliersNameController.onPageLoad(mode)
      case None                                                               => controllers.routes.JourneyRecoveryController.onPageLoad()
    }

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
    val preparedForm = request.userAnswers.get(TotalAmountWithoutVatPage).fold(form)(form.fill)
    val (currencyName, prefix) = currencyNameAndPrefix(request.userAnswers, currencyConfig.currencyConfig)
    Ok(view(preparedForm, mode, backLink(mode)(request.userAnswers), prefix, currencyName))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    val (currencyName, prefix) = currencyNameAndPrefix(request.userAnswers, currencyConfig.currencyConfig)
    form
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, mode, backLink(mode)(request.userAnswers), prefix, currencyName))),
        value =>
          for {
            updated <- Future.fromTry(request.userAnswers.set(TotalAmountWithoutVatPage, value))
            _       <- sessionRepository.set(updated)
          } yield Redirect(navigator.nextPage(TotalAmountWithoutVatPage, mode, updated))
      )
  }

}
