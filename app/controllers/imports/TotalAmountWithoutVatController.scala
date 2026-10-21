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
import forms.imports.TotalAmountWithoutVatFormProvider
import models.Mode
import navigation.{ImportNavigator, Navigator}
import pages.imports.{SadReferenceCheckPage, SadReferenceNumberPage, TotalAmountWithoutVatPage}
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
                                                  importNavigator: ImportNavigator,
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
    importNavigator.backLinkFromTotalAmountWithoutVatPage(mode)(userAnswers)

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
        value => {
          val changed = !request.userAnswers.isAnswerUnchanged(TotalAmountWithoutVatPage, value)
          val cleared: scala.util.Try[models.UserAnswers] =
            if (changed) request.userAnswers.remove(TotalVatPaidImportPage).flatMap(_.remove(ImportTotalVatClaimPage))
            else Success(request.userAnswers)
          for {
            base    <- Future.fromTry(cleared)
            updated <- Future.fromTry(base.set(TotalAmountWithoutVatPage, value).flatMap(_.set(ImportAmountWithoutVatChangedPage, changed)))
            _       <- sessionRepository.set(updated)
          } yield Redirect(navigator.nextPage(TotalAmountWithoutVatPage, mode, updated))
        }
      )
  }

}
