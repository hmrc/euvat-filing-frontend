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
import forms.SupplierVatRegistrationNumberFormProvider
import models.requests.{DataRequest, SupplierVrnCountRequest}

import javax.inject.Inject
import models.{Mode, UserAnswers}
import navigation.Navigator
import pages.{AddPurchaseResponsePage, ClaimApplicationResponsePage, InvoiceNumberPage, SupplierVatRegistrationNumberPage, VrnWarningFlowPage}
import pages.{RefundingCountryPage, SupplierTaxIdentifierNumberPage, SupplierVatRegistrationNumberPage}
import play.api.data.Form
import play.api.i18n.Lang.logger
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import repositories.SessionRepository
import services.EuVatRefundsService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.SupplierVatRegistrationNumberView

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
  euVatRefundsService : EuVatRefundsService,
  view: SupplierVatRegistrationNumberView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  val form: Form[String] = formProvider()

  private def backLink(mode: Mode) = routes.SupplierTaxNumberController.onPageLoad(mode)

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
        value => {
          val changed = !request.userAnswers.get(SupplierVatRegistrationNumberPage).contains(value)
          for {
            updated <- Future.fromTry(request.userAnswers.set(SupplierVatRegistrationNumberPage, value))
            finalAnswers <- Future.fromTry(
              if (request.userAnswers.get(VrnWarningFlowPage).isDefined && changed)
                updated.set(VrnWarningFlowPage, false)
              else
                scala.util.Success(updated)
            )
            _      <- sessionRepository.set(finalAnswers)
            result <- checkDuplicate(value, finalAnswers, mode)
          } yield result
        }
      )
  }

  private def checkDuplicate(vatNumber: String, answers: UserAnswers, mode: Mode)(implicit request: DataRequest[_]): Future[Result] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    val maybeRequest = for {
      applicationId <- answers.get(ClaimApplicationResponsePage).map(_.applicationId)
      itemNumber    <- answers.get(AddPurchaseResponsePage).map(_.itemNumber)
      invoiceNumber <- answers.get(InvoiceNumberPage)
    } yield SupplierVrnCountRequest(applicationId.toLong, itemNumber, vatNumber, invoiceNumber)

    maybeRequest match {
      case Some(req) =>
        euVatRefundsService.getSupplierVrnCount(req).map { response =>
          if (response.duplicateCount > 0)
            Redirect(routes.SupplierVrnWarningController.onPageLoad(mode))
          else
            Redirect(navigator.nextPage(SupplierVatRegistrationNumberPage, mode, answers))
        }.recover { case ex: Exception =>
          logger.error("Error retrieving supplier VRN count", ex)
          Redirect(routes.JourneyRecoveryController.onPageLoad())
        }
      case None =>
        logger.warn("Missing data for duplicate VRN check")
        Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
    }
  }

}
