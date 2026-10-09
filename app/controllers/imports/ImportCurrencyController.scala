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
import forms.RefundingCurrencyFormProvider
import models.requests.DataRequest
import models.{Mode, RefundingCurrency}
import navigation.Navigator
import pages.imports.ImportCurrencyPage
import play.api.Logger
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.*
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.radios.RadioItem
import utils.*
import views.html.purchasesOrImports.RefundingCurrencyView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ImportCurrencyController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: RefundingCurrencyFormProvider,
  currencyConfig: CurrencyConfig,
  val controllerComponents: MessagesControllerComponents,
  view: RefundingCurrencyView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  private val messagePrefix = "import.refundingCurrency"
  val form: Form[RefundingCurrency] = formProvider(messagePrefix)
  private val logger = Logger(getClass)

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
    CountryCode.findCountryCode(request.userAnswers) match {
      case None =>
        logger.warn("ImportCurrencyController.onPageLoad - no refunding country in session, redirecting to JourneyRecovery")
        Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
      case Some(countryCode) =>
        val currencies = currencyConfig.currencyConfig(countryCode)
        val preparedForm = request.userAnswers
          .get(ImportCurrencyPage)
          .flatMap(storedCode => currencies.find(_.code == storedCode))
          .flatMap(c => RefundingCurrency.values.find(_.toString.equalsIgnoreCase(c.name)))
          .fold(form)(form.fill)

        Ok(renderView(preparedForm, currencies, mode))
    }
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    CountryCode.findCountryCode(request.userAnswers) match {
      case None =>
        logger.warn("ImportCurrencyController.onSubmit - no refunding country in session, redirecting to JourneyRecovery")
        Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
      case Some(countryCode) =>
        val currencies = currencyConfig.currencyConfig(countryCode)
        form
          .bindFromRequest()
          .fold(
            formWithErrors => Future.successful(BadRequest(renderView(formWithErrors, currencies, mode))),
            value =>
              currencies.collectFirst { case c if c.name.equalsIgnoreCase(value.toString) => c.code } match {
                case None =>
                  logger.warn(s"ImportCurrencyController.onSubmit - could not find currency code for ${value.toString}")
                  Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
                case Some(currencyCode) =>
                  for {
                    updatedAnswers <- Future.fromTry(request.userAnswers.set(ImportCurrencyPage, currencyCode))
                    _              <- sessionRepository.set(updatedAnswers)
                  } yield Redirect(navigator.nextPage(ImportCurrencyPage, mode, updatedAnswers))
              }
          )
    }
  }

  private def renderView(form: Form[?], currencies: Seq[Currency], mode: Mode)(implicit request: DataRequest[?]) =
    view(
      form,
      PurchaseOrImportHelpers.currencyRadioItems(currencies, messagesApi.preferred(request)),
      routes.ImportCurrencyController.onSubmit(mode),
      messagePrefix,
      "import.caption",
      Some(importRoutes.ImportSuppliersNameController.onPageLoad(mode))
    )
}
