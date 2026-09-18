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
import forms.purchase.SupplierTaxNumberFormProvider
import models.{CheckMode, InvoiceType, Mode, NormalMode, SupplierTaxNumber}
import navigation.Navigator
import pages.{InvoiceTypePage, SupplierTaxIdentifierNumberPage, SupplierTaxNumberPage, SupplierVatRegistrationNumberPage}
import play.api.Logger
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.*
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.ControllerHelpers.*
import views.html.purchase.SupplierTaxNumberView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SupplierTaxNumberController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: SupplierTaxNumberFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: SupplierTaxNumberView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  val form: Form[SupplierTaxNumber] = formProvider()
  private val logger = Logger(getClass)

  private def backLink(mode: Mode): Call = if (mode == CheckMode) {
    routes.CheckYourPurchaseDetailsController.onPageLoad()
  } else {
    routes.SupplierAddressController.onPageLoad(NormalMode)
  }

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
    val preparedForm = request.userAnswers.get(SupplierTaxNumberPage).fold(form)(form.fill)
    val isSimplifiedInvoice: Boolean = request.userAnswers.get(InvoiceTypePage).contains(InvoiceType.SimplifiedInvoice)
    Ok(view(preparedForm, mode, backLink(mode), isSimplifiedInvoice))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    val isSimplifiedInvoice: Boolean = request.userAnswers.get(InvoiceTypePage).contains(InvoiceType.SimplifiedInvoice)
    form
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, mode, backLink(mode), isSimplifiedInvoice))),
        value =>
          for {
            userAnswers <- Future.fromTry(request.userAnswers.set(SupplierTaxNumberPage, value))
            updatedAnswers <- value match {
                                case SupplierTaxNumber.Vatregistrationnumber => Future.fromTry(userAnswers.remove(SupplierTaxIdentifierNumberPage))
                                case SupplierTaxNumber.Taxidentifiernumber   => Future.fromTry(userAnswers.remove(SupplierVatRegistrationNumberPage))
                                case SupplierTaxNumber.Neither =>
                                  for {
                                    vatAnswers <- Future.fromTry(userAnswers.remove(SupplierVatRegistrationNumberPage))
                                    tidAnswers <- Future.fromTry(vatAnswers.remove(SupplierTaxIdentifierNumberPage))
                                  } yield tidAnswers
                              }
            _ <- sessionRepository.set(updatedAnswers)
          } yield Redirect(navigator.nextPage(SupplierTaxNumberPage, mode, updatedAnswers))
      )
  }

}
