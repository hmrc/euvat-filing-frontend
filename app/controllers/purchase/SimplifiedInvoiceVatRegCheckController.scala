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
import forms.purchase.SimplifiedInvoiceVatRegCheckFormProvider
import models.requests.DataRequest
import models.{CheckMode, Mode, NormalMode}
import navigation.Navigator
import pages.{PurchaseTypePage, SimplifiedInvoiceVatRegCheckPage, SupplierAddressPage, SupplierVatRegistrationNumberPage}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.{CheckModeShortCircuit, ControllerHelpers}
import views.html.purchase.SimplifiedInvoiceVatRegCheckView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SimplifiedInvoiceVatRegCheckController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: SimplifiedInvoiceVatRegCheckFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: SimplifiedInvoiceVatRegCheckView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  val form: Form[Boolean] = formProvider()
  private def backLink: Call = routes.SupplierAddressController.onPageLoad(NormalMode)

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
    request.userAnswers.get(SupplierAddressPage) match {
      case None => Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
      case Some(_) =>
        val preparedForm = ControllerHelpers.preparedFormFromAnswers(_.get(SimplifiedInvoiceVatRegCheckPage), form)
        okView(preparedForm, mode)
    }
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    form
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(badRequestView(formWithErrors, mode)),
        value =>
          CheckModeShortCircuit.shortCircuitIfUnchanged(
            SimplifiedInvoiceVatRegCheckPage,
            value,
            mode,
            request.userAnswers,
            routes.CheckYourPurchaseDetailsController.onPageLoad()
          ) match {
            case Some(res) => Future.successful(res)
            case None =>
              val userAnswersTry = request.userAnswers.set(SimplifiedInvoiceVatRegCheckPage, value)

              (mode, request.userAnswers.get(PurchaseTypePage)) match {
                case (CheckMode, Some(_)) if !value =>
                  for {
                    afterSet     <- Future.fromTry(userAnswersTry)
                    afterCleared <- Future.fromTry(afterSet.remove(SupplierVatRegistrationNumberPage))
                    _            <- sessionRepository.set(afterCleared)
                  } yield Redirect(routes.CheckYourPurchaseDetailsController.onPageLoad())

                case (CheckMode, Some(_)) if value =>
                  for {
                    afterSet <- Future.fromTry(userAnswersTry)
                    _        <- sessionRepository.set(afterSet)
                  } yield Redirect(routes.SupplierVatRegistrationNumberController.onPageLoad(mode))

                case _ =>
                  for {
                    persistedAnswers <- Future.fromTry(userAnswersTry)
                    _                <- sessionRepository.set(persistedAnswers)
                  } yield {
                    if (value) Redirect(routes.SupplierVatRegistrationNumberController.onPageLoad(mode))
                    else Redirect(routes.TotalPurchaseAmountBeforeVatController.onPageLoad(mode))
                  }
              }
          }
      )
  }

  private def okView(formToRender: Form[?], mode: Mode)(implicit request: DataRequest[?]) =
    Ok(view(formToRender, mode, backLink))

  private def badRequestView(formWithErrors: Form[?], mode: Mode)(implicit request: DataRequest[?]) =
    BadRequest(view(formWithErrors, mode, backLink))
}
