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
import forms.purchase.SupplierVatRegistrationNumberFormProvider
import models.requests.{DataRequest, SupplierVrnCountRequest}
import models.{CheckMode, InvoiceType, Mode, NormalMode, UserAnswers}
import navigation.Navigator
import pages.*
import play.api.Logging
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.*
import queries.ClaimApplicationResponseQuery
import repositories.SessionRepository
import services.EuVatRefundsService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.purchase.SupplierVatRegistrationNumberView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SupplierVatRegistrationNumberController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: SupplierVatRegistrationNumberFormProvider,
  val controllerComponents: MessagesControllerComponents,
  euVatRefundsService: EuVatRefundsService,
  view: SupplierVatRegistrationNumberView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with Logging
    with I18nSupport {

  val form: Form[String] = formProvider()

  private def backLink(mode: Mode)(implicit request: DataRequest[?]): Call = {
    val isGermany = request.userAnswers.get(RefundingCountryPage).exists(_.equalsIgnoreCase("DE"))
    val isSimplified = request.userAnswers.get(InvoiceTypePage).contains(InvoiceType.SimplifiedInvoice)
    val hasInvoiceNumber = request.userAnswers.get(InvoiceNumberPage).isDefined

    mode match {
      case CheckMode if hasInvoiceNumber => routes.InvoiceNumberController.onPageLoad(CheckMode)
      case CheckMode                     => routes.CheckYourPurchaseDetailsController.onPageLoad()
      case _ if isGermany                => routes.SupplierTaxNumberController.onPageLoad(NormalMode)
      case _ if isSimplified             => routes.SimplifiedInvoiceVatRegCheckController.onPageLoad(NormalMode)
      case _                             => routes.SupplierAddressController.onPageLoad(NormalMode)
    }
  }

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
    for {
      updatedAnswers <- Future.fromTry(request.userAnswers.remove(SupplierTaxIdentifierNumberPage))
      _              <- sessionRepository.set(updatedAnswers)
    } yield None

    val preparedForm = request.userAnswers.get(SupplierVatRegistrationNumberPage).fold(form)(form.fill)
    val isGermany = request.userAnswers.get(RefundingCountryPage).exists(_.equalsIgnoreCase("DE"))
    Ok(view(preparedForm, mode, backLink(mode), isGermany))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    val isGermany = request.userAnswers.get(RefundingCountryPage).exists(_.equalsIgnoreCase("DE"))
    form
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, mode, backLink(mode), isGermany))),
        value =>
          buildSupplierVrnCountRequest(request.userAnswers, value) match {
            case Some(vrnCountRequest) =>
              euVatRefundsService
                .getSupplierVrnCount(vrnCountRequest)
                .flatMap { response =>
                  for {
                    userAnswers    <- Future.fromTry(request.userAnswers.set(SupplierVatRegistrationNumberPage, value))
                    updatedAnswers <- Future.fromTry(userAnswers.remove(SupplierVatRegistrationWarningPage))
                    _              <- sessionRepository.set(updatedAnswers)
                  } yield {
                    if (response.duplicateCount > 0) {
                      Redirect(controllers.warning.routes.SupplierVrnWarningController.onPageLoad(mode))
                    } else {
                      Redirect(navigator.nextPage(SupplierVatRegistrationNumberPage, mode, updatedAnswers))
                    }
                  }
                }
                .recover { case ex =>
                  logger.error("Error while retrieving supplier VRN count", ex)
                  Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
                }
            case _ =>
              logger.warn("Missing session data")
              Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
          }
      )
  }

  private def buildSupplierVrnCountRequest(answers: UserAnswers, vatNumber: String): Option[SupplierVrnCountRequest] =
    for {
      applicationId <- answers.get(ClaimApplicationResponseQuery).map(_.applicationId)
      itemNumber    <- answers.get(AddPurchaseResponsePage).map(_.itemNumber)
      invoiceNumber <- answers.get(InvoiceNumberPage)
    } yield SupplierVrnCountRequest(applicationId, itemNumber, vatNumber, invoiceNumber)

}
