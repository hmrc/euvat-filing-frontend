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
import forms.purchase.TotalPurchaseAmountBeforeVatFormProvider
import models.Mode
import models.requests.DataRequest
import navigation.Navigator
import pages.purchase.*
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.ControllerHelpers.*
import utils.CurrencyConfig
import views.html.purchasesOrImports.PurchaseOrImportTotalAmountWithoutVatView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class TotalPurchaseAmountBeforeVatController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  currencyConfig: CurrencyConfig,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: TotalPurchaseAmountBeforeVatFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: PurchaseOrImportTotalAmountWithoutVatView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  val form: Form[BigDecimal] = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
    val preparedForm = request.userAnswers.get(TotalPurchaseAmountBeforeVatPage).fold(form)(form.fill)
    Ok(renderView(preparedForm, mode))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    form
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(renderView(formWithErrors, mode))),
        value => handleSubmit(value, mode)
      )
  }

  private def handleSubmit(value: BigDecimal, mode: Mode)(implicit request: DataRequest[?]) =
    shortCircuit(
      TotalPurchaseAmountBeforeVatPage,
      value,
      mode,
      request.userAnswers,
      navigator.nextPage(TotalPurchaseAmountBeforeVatPage, mode, request.userAnswers),
      routes.CheckYourPurchaseDetailsController.onPageLoad(),
      Some(sessionRepository)
    )(updated => Future.successful(Redirect(navigator.nextPage(TotalPurchaseAmountBeforeVatPage, mode, updated))))

  private def renderView(form: Form[?], mode: Mode)(implicit request: DataRequest[?]) = {
    val (currencyName, prefix) = currencyNameAndPrefix(request.userAnswers, currencyConfig.currencyConfig)
    view(
      form,
      routes.TotalPurchaseAmountBeforeVatController.onSubmit(mode),
      "totalAmountWithoutVat",
      "purchase.caption",
      prefix,
      currencyName
    )
  }
}
