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

import pages.imports.*
import scala.util.Success

import controllers.actions.*
import controllers.imports.routes as importRoutes
import forms.imports.TotalVatPaidImportFormProvider

import javax.inject.Inject
import models.{Mode, UserAnswers}
import navigation.Navigator
import pages.imports.{TotalAmountWithoutVatPage, TotalVatPaidImportPage}
import queries.ImportVatPaidChangedQuery
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.ControllerHelpers.*
import utils.CurrencyConfig
import views.html.purchasesOrImports.PurchaseOrImportTotalVatPaidView

import scala.concurrent.{ExecutionContext, Future}

class TotalVatPaidImportController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  currencyConfig: CurrencyConfig,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: TotalVatPaidImportFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: PurchaseOrImportTotalVatPaidView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  val form: Form[BigDecimal] = formProvider()

  private def backLink(mode: Mode): Call =
    importRoutes.TotalAmountWithoutVatController.onPageLoad(mode)

  private def formAction(mode: Mode): Call = routes.TotalVatPaidImportController.onSubmit(mode)

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
    val preparedForm = request.userAnswers.get(TotalVatPaidImportPage).fold(form)(form.fill)
    val (currencyName, prefix) = currencyNameAndPrefix(request.userAnswers, currencyConfig.currencyConfig)
    Ok(view(preparedForm, formAction(mode), backLink(mode), "import.caption", "totalVatPaidImport.p1", prefix, currencyName))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    val (currencyName, prefix) = currencyNameAndPrefix(request.userAnswers, currencyConfig.currencyConfig)
    form
      .bindFromRequest()
      .fold(
        formWithErrors =>
          Future.successful(
            BadRequest(view(formWithErrors, formAction(mode), backLink(mode), "import.caption", "totalVatPaidImport.p1", prefix, currencyName))
          ),
        value => {
          val changed = !request.userAnswers.isAnswerUnchanged(TotalVatPaidImportPage, value)
          val cleared: scala.util.Try[models.UserAnswers] =
            if (changed) request.userAnswers.remove(ImportTotalVatClaimPage) else Success(request.userAnswers)
          for {
            base    <- Future.fromTry(cleared)
            updated <- Future.fromTry(base.set(TotalVatPaidImportPage, value).flatMap(_.set(ImportVatPaidChangedQuery, changed)))
            _       <- sessionRepository.set(updated)
          } yield {
            val totalAmountWithoutVat = updated.get(TotalAmountWithoutVatPage).getOrElse(BigDecimal(0))
            if (value < totalAmountWithoutVat) {
              Redirect(navigator.nextPage(TotalVatPaidImportPage, mode, updated))
            } else {
              Redirect(controllers.warning.routes.VatPaidWarningController.onPageLoad(mode))
            }
          }
        }
      )
  }
}
