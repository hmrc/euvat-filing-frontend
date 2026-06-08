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

import javax.inject.Inject
import models.{Mode, RefundingCurrency}
import navigation.Navigator
import pages.RefundingCurrencyPage
import play.api.Logger
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.ConfigCurrencyMapping
import views.html.RefundingCurrencyView

import scala.concurrent.{ExecutionContext, Future}

class RefundingCurrencyController @Inject() (
                                              override val messagesApi: MessagesApi,
                                              sessionRepository: SessionRepository,
                                              navigator: Navigator,
                                              identify: IdentifierAction,
                                              getData: DataRetrievalAction,
                                              requireData: DataRequiredAction,
                                              formProvider: RefundingCurrencyFormProvider,
                                              configCurrencyMapping: ConfigCurrencyMapping,
                                              val controllerComponents: MessagesControllerComponents,
                                              view: RefundingCurrencyView
                                            )(implicit ec: ExecutionContext)
  extends FrontendBaseController
    with I18nSupport {

  val form: Form[RefundingCurrency] = formProvider()
  private val logger = Logger(getClass)

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
    val maybeCountryCode = request.userAnswers.get(pages.RefundingCountryPage).orElse {
      request.userAnswers.get(pages.RefundingCountryNamePage).map { stored =>
        stored.split(",", 2).headOption.getOrElse(stored)
      }
    }

    maybeCountryCode match {
      case None =>
        logger.warn("RefundingCurrencyController.onPageLoad - no refunding country in session, redirecting to JourneyRecovery")
        Redirect(routes.JourneyRecoveryController.onPageLoad())
      case Some(countryCode) =>
        val preparedForm = request.userAnswers.get(RefundingCurrencyPage) match {
          case None        => form
          case Some(value) => form.fill(value)
        }
        val currencies = configCurrencyMapping.currenciesFor(countryCode)
        logger.warn(s"DEBUG currencies for $countryCode: $currencies")
        val msgs = messagesApi.preferred(request)
        val items = currencies.zipWithIndex.flatMap { case (currency, idx) =>
          RefundingCurrency.values.find(_.toString.equalsIgnoreCase(currency)).map { v =>
            uk.gov.hmrc.govukfrontend.views.viewmodels.radios.RadioItem(
              content = Text(msgs(s"refundingCurrency.${v.toString}")),
              value   = Some(v.toString),
              id      = Some(s"value_$idx")
            )
          }
        }
        Ok(view(preparedForm, items, routes.RefundingLanguageController.onPageLoad(mode), mode))
    }
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    form
      .bindFromRequest()
      .fold(
        formWithErrors =>
          val maybeCountryCode = request.userAnswers.get(pages.RefundingCountryPage).orElse {
            request.userAnswers.get(pages.RefundingCountryNamePage).map { stored =>
              stored.split(",", 2).headOption.getOrElse(stored)
            }
          }

          maybeCountryCode match {
            case None =>
              logger.warn(
                "RefundingCurrencyController.onSubmit - no refunding country in session while binding form errors; redirecting to JourneyRecovery"
              )
              Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
            case Some(countryCode) =>
              val currencies = configCurrencyMapping.currenciesFor(countryCode)
              val msgs = messagesApi.preferred(request)
              val items = currencies.zipWithIndex.flatMap { case (currency, idx) =>
                RefundingCurrency.values.find(_.toString.equalsIgnoreCase(currency)).map { v =>
                  uk.gov.hmrc.govukfrontend.views.viewmodels.radios.RadioItem(
                    content = Text(msgs(s"refundingCurrency.${v.toString}")),
                    value   = Some(v.toString),
                    id      = Some(s"value_$idx")
                  )
                }
              }
              Future.successful(BadRequest(view(formWithErrors, items, routes.RefundingLanguageController.onPageLoad(mode), mode)))
          }
        ,
        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(RefundingCurrencyPage, value))
            _              <- sessionRepository.set(updatedAnswers)
          } yield Redirect(navigator.nextPage(RefundingCurrencyPage, mode, updatedAnswers))
      )
  }
}