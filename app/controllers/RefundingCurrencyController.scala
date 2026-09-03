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
import forms.RefundingCurrencyFormProvider
import models.requests.DataRequest
import models.{Mode, RefundingCurrency, UserAnswers}
import navigation.Navigator
import pages.{ClaimDetailsAmendedPage, ClaimDetailsCompletedPage, CurrencyChangedPage, RefundingCurrencyPage}
import play.api.Logger
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.*
import repositories.SessionRepository
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.radios.RadioItem
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.*
import views.html.RefundingCurrencyView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Failure}

class RefundingCurrencyController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: RefundingCurrencyFormProvider,
  currencyConfig: CurrencyConfig,
  configLanguageMapping: ConfigLanguageMapping,
  val controllerComponents: MessagesControllerComponents,
  view: RefundingCurrencyView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  val form: Form[RefundingCurrency] = formProvider()
  private val logger = Logger(getClass)

  private def backLink(mode: Mode): Call = routes.SupplierVatRegistrationNumberController.onPageLoad(mode)

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
    CountryCode.findCountryCode(request.userAnswers) match {
      case None =>
        logger.warn("RefundingCurrencyController.onPageLoad - no refunding country in session, redirecting to JourneyRecovery")
        Redirect(routes.JourneyRecoveryController.onPageLoad())
      case Some(countryCode) =>
        val currencies = currencyConfig.currencyConfig(countryCode)
        val msgs = messagesApi.preferred(request)
        val items = buildRadioItems(currencies, msgs)
        val preparedForm = request.userAnswers
          .get(RefundingCurrencyPage)
          .flatMap { storedCode =>
            currencies.find(_._2 == storedCode).map { case Currency(name, _, _) =>
              form.fill(RefundingCurrency.values.find(_.toString == name).getOrElse(RefundingCurrency.Euro))
            }
          }
          .getOrElse(form)

        Ok(view(preparedForm, items, backLink(mode), mode))
    }
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    form.bindFromRequest().fold(handleFormErrors(_, mode), handleValidSubmission(_, mode))
  }

  private def handleFormErrors(formWithErrors: Form[?], mode: Mode)(implicit request: DataRequest[?]): Future[Result] =
    utils.CountryCode.findCountryCode(request.userAnswers) match {
      case None =>
        logger.warn(
          "RefundingCurrencyController.onSubmit - no refunding country in session while binding form errors; redirecting to JourneyRecovery"
        )
        Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
      case Some(countryCode) =>
        val currencies = currencyConfig.currencyConfig(countryCode)
        val msgs = messagesApi.preferred(request)
        val items = buildRadioItems(currencies, msgs)
        Future.successful(BadRequest(view(formWithErrors, items, backLink(mode), mode)))
    }

  private def handleValidSubmission(value: RefundingCurrency, mode: Mode)(implicit request: DataRequest[?]): Future[Result] =
    CountryCode.findCountryCode(request.userAnswers) match {
      case None =>
        logger.warn("RefundingCurrencyController.onSubmit - no refunding country in session; redirecting to JourneyRecovery")
        Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
      case Some(countryCode) =>
        val currencies: Seq[Currency] = currencyConfig.currencyConfig(countryCode)

        currencies.collectFirst { case c if c.name.equalsIgnoreCase(value.toString) => c.code } match {
          case None =>
            logger.warn(s"RefundingCurrencyController.onSubmit - could not find currency code for ${value.toString}")
            Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
          case Some(currencyCode) =>
            val isChanged = request.userAnswers.get(RefundingCurrencyPage).exists(_ != currencyCode)

            val purchaseCYA = controllers.purchase.routes.CheckYourPurchaseDetailsController.onPageLoad()

            utils.ControllerHelpers.shortCircuit(
              RefundingCurrencyPage,
              currencyCode,
              mode,
              request.userAnswers,
              navigator.nextPage(RefundingCurrencyPage, mode, request.userAnswers),
              purchaseCYA,
              None
            ) { (answersAfterSet: UserAnswers) =>
              val maybeAmendedTry =
                if (isChanged && request.userAnswers.get(ClaimDetailsCompletedPage).contains(true))
                  answersAfterSet.set(ClaimDetailsAmendedPage, true)
                else Success(answersAfterSet)

              val maybeCurrencyChangedTry = maybeAmendedTry.flatMap { ua =>
                if (isChanged) ua.set(CurrencyChangedPage, true) else Success(ua)
              }

              Future.fromTry(maybeCurrencyChangedTry)
                .flatMap: finalAnswers =>
                  sessionRepository
                    .set(finalAnswers)
                    .map: _ =>
                      Redirect(navigator.nextPage(RefundingCurrencyPage, mode, finalAnswers))
            }
        }
    }

  private def buildRadioItems(
    currencies: Seq[Currency],
    msgs: Messages
  ): Seq[RadioItem] =
    currencies.zipWithIndex
      .flatMap: (c, idx) =>
        RefundingCurrency.values
          .find(_.toString.equalsIgnoreCase(c.name))
          .map: v =>
            RadioItem(
              content         = Text(msgs(s"refundingCurrency.${v.toString}", c.symbol)),
              value           = Some(v.toString),
              id              = Some(if (idx == 0) "value" else s"value_$idx"),
              label           = None,
              hint            = None,
              divider         = None,
              checked         = false,
              conditionalHtml = None,
              disabled        = false,
              attributes      = Map.empty
            )
}
