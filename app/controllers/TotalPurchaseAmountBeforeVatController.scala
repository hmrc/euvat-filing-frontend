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
import forms.TotalPurchaseAmountBeforeVatFormProvider
import models.{InvoiceType, Mode, SupplierTaxNumber, UserAnswers}
import navigation.Navigator
import pages.*
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.{ConfigCurrencyMapping, RefundingAndPurchaseUtils}
import views.html.TotalPurchaseAmountBeforeVatView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class TotalPurchaseAmountBeforeVatController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  refundingAndPurchaseUtils: RefundingAndPurchaseUtils,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: TotalPurchaseAmountBeforeVatFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: TotalPurchaseAmountBeforeVatView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  val form: Form[BigDecimal] = formProvider()
  private def backLink(mode: Mode)(userAnswers: UserAnswers): Call = {
    // If this country requires currency selection, the currency page was shown right before this one.
    userAnswers.get(RefundingCountryPage) match {
      case Some(countryCode) if refundingAndPurchaseUtils.requiresCurrencySelection(countryCode) =>
        routes.RefundingCurrencyController.onPageLoad(mode)

      case Some("DE") =>
        // Prefer explicit answers about which supplier tax path was chosen (the
        // SupplierTaxNumberPage), falling back to presence of the specific pages
        // if required.
        userAnswers.get(SupplierVatRegistrationNumberPage) match {
          case Some(_) => routes.SupplierVatRegistrationNumberController.onPageLoad(mode)
          case None =>
            userAnswers.get(SupplierTaxNumberPage) match {
              case Some(SupplierTaxNumber.Vatregistrationnumber) => routes.SupplierVatRegistrationNumberController.onPageLoad(mode)
              case Some(SupplierTaxNumber.Taxidentifiernumber)   => routes.SupplierTaxIdentifierNumberController.onPageLoad(mode)
              case _ =>
                if (userAnswers.get(SupplierTaxIdentifierNumberPage).isDefined) {
                  routes.SupplierTaxIdentifierNumberController.onPageLoad(mode)
                } else {
                  routes.SupplierTaxNumberController.onPageLoad(mode)
                }
            }
        }

      case _ =>
        userAnswers.get(SupplierVatRegistrationNumberPage) match {
          case Some(_) => routes.SupplierVatRegistrationNumberController.onPageLoad(mode)
          case None    => routes.SimplifiedInvoiceVatRegCheckController.onPageLoad(mode)
        }
    }
  }

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
    val preparedForm = request.userAnswers.get(TotalPurchaseAmountBeforeVatPage) match {
      case None        => form
      case Some(value) => form.fill(value)
    }
    val (currencyName, prefix) = refundingAndPurchaseUtils.resolveCurrency(request.userAnswers)
    Ok(view(preparedForm, mode, backLink(mode)(request.userAnswers), prefix, currencyName))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    form
      .bindFromRequest()
      .fold(
        formWithErrors => {
          val (currencyName, prefix) = refundingAndPurchaseUtils.resolveCurrency(request.userAnswers)
          Future.successful(BadRequest(view(formWithErrors, mode, backLink(mode)(request.userAnswers), prefix, currencyName)))
        },
        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(TotalPurchaseAmountBeforeVatPage, value))
            _              <- sessionRepository.set(updatedAnswers)
          } yield Redirect(navigator.nextPage(TotalPurchaseAmountBeforeVatPage, mode, updatedAnswers))
      )
  }

}
