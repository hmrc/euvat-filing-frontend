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
import forms.purchase.TotalVatClaimFormProvider
import models.{Mode, UserAnswers}
import navigation.Navigator
import pages.ImportTotalVatClaimPage
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.ControllerHelpers.currencySymbolFromSession
import utils.CurrencyConfig
import views.html.PurchaseOrImportTotalVatClaimView

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
  view: PurchaseOrImportTotalVatClaimView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  val form: Form[BigDecimal] = formProvider()

  private def backLink(mode: Mode): Call = routes.TotalVatPaidImportController.onPageLoad(mode)

  private def formAction(mode: Mode): Call = routes.TotalVatClaimController.onSubmit(mode)

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
    val preparedForm = request.userAnswers.get(ImportTotalVatClaimPage).fold(form)(form.fill)
    val currencySymbol = currencySymbolFromSession(request.userAnswers, currencyConfig.currencyConfig)
    Ok(view(preparedForm, mode, formAction(mode), backLink(mode), currencySymbol))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    val currencySymbol = currencySymbolFromSession(request.userAnswers, currencyConfig.currencyConfig)
    form
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, mode, formAction(mode), backLink(mode), currencySymbol))),
        value =>
          for {
            userAnswers <- Future.fromTry(request.userAnswers.set(ImportTotalVatClaimPage, value))
            _           <- sessionRepository.set(userAnswers)
          } yield Redirect(navigator.nextPage(ImportTotalVatClaimPage, mode, userAnswers))
      )
  }

}
