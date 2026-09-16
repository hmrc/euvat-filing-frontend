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
import forms.purchase.SupplierTaxIdentifierNumberFormProvider
import models.requests.{DataRequest, SupplierTaxIdentifierCountRequest}
import models.responses.{AddPurchaseResponse, SupplierTaxIdentifierCountResponse}
import models.{CheckMode, Mode, NormalMode}
import navigation.Navigator
import pages.*
import play.api.Logging
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.ClaimApplicationResponseQuery
import repositories.SessionRepository
import services.EuVatRefundsService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.purchase.SupplierTaxIdentifierNumberView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SupplierTaxIdentifierNumberController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  euVatRefundsService: EuVatRefundsService,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: SupplierTaxIdentifierNumberFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: SupplierTaxIdentifierNumberView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with Logging
    with I18nSupport {

  val form: Form[String] = formProvider()

  private def backLink(request: DataRequest[?], mode: Mode) = {
    val hasInvoiceNumber = request.userAnswers.get(InvoiceNumberPage).isDefined

    mode match {
      case CheckMode if hasInvoiceNumber => routes.InvoiceNumberController.onPageLoad(CheckMode)
      case CheckMode                     => routes.CheckYourPurchaseDetailsController.onPageLoad()
      case _                             => routes.SupplierTaxNumberController.onPageLoad(NormalMode)
    }
  }

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
    for {
      updatedAnswers <- Future.fromTry(request.userAnswers.remove(SupplierVatRegistrationNumberPage))
      _              <- sessionRepository.set(updatedAnswers)
    } yield None
    val preparedForm = request.userAnswers.get(SupplierTaxIdentifierNumberPage).fold(form)(form.fill)
    Ok(view(preparedForm, mode, backLink(request, mode)))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    form
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, mode, backLink(request, mode)))),
        value =>
          val invoiceNumber = request.userAnswers.get(InvoiceNumberPage).getOrElse("")
          val maybeAppId = request.userAnswers.get(ClaimApplicationResponseQuery).map(_.applicationId)
          val mayBeItemNumber = request.userAnswers.get(AddPurchaseResponsePage).map(_.itemNumber)

          (maybeAppId, mayBeItemNumber) match {
            case (Some(maybeAppId), Some(mayBeItemNumber)) =>
              euVatRefundsService
                .getSupplierTaxIdentifierCount(SupplierTaxIdentifierCountRequest(maybeAppId, mayBeItemNumber, value, invoiceNumber))
                .flatMap { case SupplierTaxIdentifierCountResponse(dupCount) =>
                  for {
                    userAnswers    <- Future.fromTry(request.userAnswers.set(SupplierTaxIdentifierNumberPage, value))
                    updatedAnswers <- Future.fromTry(userAnswers.remove(SupplierTaxIdentifierWarningPage))
                    _              <- sessionRepository.set(updatedAnswers)
                  } yield {
                    if (dupCount > 0) {
                      Redirect(controllers.warning.routes.SupplierTaxIdentifierWarningController.onPageLoad())
                    } else {
                      Redirect(navigator.nextPage(SupplierTaxIdentifierNumberPage, mode, updatedAnswers))
                    }
                  }
                }
                .recover { case ex =>
                  logger.error("Error while retrieving supplier tax identifier count", ex)
                  Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
                }
            case _ =>
              logger.warn("Missing session data")
              Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
          }
      )
  }

}
