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

package controllers.bankdetails

import config.InternationalCurrencyProvider
import controllers.actions.{DataRequiredAction, DataRetrievalAction, IdentifierAction}
import models.requests.UpdateApplicationDetailsRequest
import models.{ApplicationDetails, BankDetails, CheckMode, UserAnswers}
import pages.bankdetails.BankDetailsPage
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.*
import queries.{BankDetailsCompletedQuery, ClaimApplicationResponseQuery, UpdateSequenceNumberQuery}
import repositories.SessionRepository
import services.EuVatRefundsService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.checkAnswers.CheckYourBankDetailsSummary
import views.html.bankdetails.CheckYourBankDetailsView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CheckYourBankDetailsController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  val controllerComponents: MessagesControllerComponents,
  view: CheckYourBankDetailsView,
  currencyProvider: InternationalCurrencyProvider,
  sessionRepository: SessionRepository,
  service: EuVatRefundsService
)(using ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad(): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    val userAnswers = request.userAnswers
    val isChanged = bankDetailsHasChanged(userAnswers)
    val changeUrl = routes.AccountHolderController.onPageLoad(CheckMode).url
    val rows = CheckYourBankDetailsSummary.rows(request.userAnswers, currencyProvider.currencies, isChanged)

    userAnswers.get(ClaimApplicationResponseQuery) match {
      case Some(claimApplicationResponse) =>
        val applicationId = claimApplicationResponse.applicationId
        service
          .getApplicationDetails(applicationId)
          .flatMap { applicationDetailsResponse =>
            applicationDetailsResponse.applicationDetails.headOption match {
              case Some(application) if application.ibanCode.nonEmpty =>
                for {
                  userAnswersUpdated <- Future.fromTry(userAnswers.set(BankDetailsCompletedQuery, true))
                  _                  <- sessionRepository.set(userAnswersUpdated)
                } yield Ok(view(rows, changeUrl, bankDetailsCompleted = true, isChanged))
              case _ =>
                for {
                  userAnswersUpdated <- Future.fromTry(userAnswers.set(BankDetailsCompletedQuery, false))
                  _                  <- sessionRepository.set(userAnswersUpdated)
                } yield Ok(view(rows, changeUrl, bankDetailsCompleted = false, isChanged))
            }
          }
          .recover { case ex =>
            logger.error("Error while retrieving application details", ex)
            Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
          }
      case _ =>
        logger.error("ClaimApplicationResponseQuery is missing in session")
        Future.successful(Ok(view(rows, changeUrl, bankDetailsCompleted = false, isChanged)))
    }

  }

  def onSubmit(): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    val userAnswers = request.userAnswers

    userAnswers.get(ClaimApplicationResponseQuery) match {
      case Some(claimApplicationResponse) =>
        val applicationId = claimApplicationResponse.applicationId
        service
          .getApplicationDetails(applicationId)
          .flatMap { applicationDetailsResponse =>
            applicationDetailsResponse.applicationDetails.headOption match {
              case Some(application) =>
                if (bankDetailsHasChanged(userAnswers)) {
                  logger.error(s"Updating application for applicationId: $applicationId")
                  updateApplicationAndRedirect(userAnswers, application)
                } else {
                  logger.info("No changes to save. Redirecting to task list")
                  Future.successful(Redirect(controllers.routes.TaskListDashboardController.onPageLoad()))
                }
              case _ =>
                logger.error(s"No application details found for applicationId: $applicationId")
                Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
            }
          }
          .recover { case ex =>
            logger.error("Error while retrieving or saving application details", ex)
            Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
          }
      case _ =>
        logger.error("ClaimApplicationResponseQuery is missing in session")
        Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
    }
  }

  private def bankDetailsHasChanged(userAnswers: UserAnswers) = {
    userAnswers
      .get(BankDetailsPage)
      .exists(bankDetails => bankDetails.BIC.nonEmpty && bankDetails.accountNumber.nonEmpty)
  }

  private def updateApplicationAndRedirect(userAnswers: UserAnswers, applicationDetails: ApplicationDetails)(using RequestHeader) = {
    val request = UpdateApplicationDetailsRequest.from(userAnswers, applicationDetails)
    service
      .updateApplicationDetails(request)
      .flatMap { updateApplicationResponse =>
        val updateSequenceNumber = updateApplicationResponse.updateSequenceNumber
        val maybeEmptyBankDetails = userAnswers
          .get(BankDetailsPage)
          .map(bankDetails => bankDetails.copy(BIC = "", accountNumber = ""))

        maybeEmptyBankDetails match {
          case Some(emptyBankDetails) =>
            for {
              seqNumUpdated        <- Future.fromTry(userAnswers.set(UpdateSequenceNumberQuery, updateSequenceNumber))
              bankDetailsCompleted <- Future.fromTry(seqNumUpdated.set(BankDetailsCompletedQuery, true))
              bankDetailsEmptied   <- Future.fromTry(bankDetailsCompleted.set(BankDetailsPage, emptyBankDetails))
              _                    <- sessionRepository.set(bankDetailsEmptied)
            } yield Redirect(controllers.routes.TaskListDashboardController.onPageLoad())
          case _ =>
            for {
              seqNumUpdated        <- Future.fromTry(userAnswers.set(UpdateSequenceNumberQuery, updateSequenceNumber))
              bankDetailsCompleted <- Future.fromTry(seqNumUpdated.set(BankDetailsCompletedQuery, true))
              _                    <- sessionRepository.set(bankDetailsCompleted)
            } yield Redirect(controllers.routes.TaskListDashboardController.onPageLoad())
        }
      }
  }
}
