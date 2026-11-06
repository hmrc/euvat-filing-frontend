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
import models.{AccountHolder, ApplicationDetails, BankDetails, CheckMode, UserAnswers}
import pages.bankdetails.{AccountHolderPage, BankDetailsPage}
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

  def onPageLoad(): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
    val rows = CheckYourBankDetailsSummary.rows(request.userAnswers, currencyProvider.currencies)

    val changeUrl = routes.AccountHolderController.onPageLoad(CheckMode).url
    val bankDetailsCompleted = request.userAnswers.get(BankDetailsCompletedQuery).getOrElse(false)

    Ok(view(rows, changeUrl, bankDetailsCompleted))
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
                if (bankDetailsUnchanged(userAnswers, application)) {
                  logger.info("No changes to save. Redirecting to task list")
                  Future.successful(Redirect(controllers.routes.TaskListDashboardController.onPageLoad()))
                } else {
                  updateApplicationAndRedirect(userAnswers, application)
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

  private def bankDetailsUnchanged(userAnswers: UserAnswers, applicationDetails: ApplicationDetails): Boolean = {

    val bankDetailsUnchanged = userAnswers.get(BankDetailsPage) match {
      case Some(bankDetails) =>
        applicationDetails.bankAccountOwnerName.contains(bankDetails.NameOnTheAccount) &&
        applicationDetails.bicCode.contains(bankDetails.BIC) &&
        applicationDetails.ibanCode.contains(bankDetails.accountNumber) &&
        applicationDetails.bankAccountCurrencyCode.contains(bankDetails.CurrencyOfAccount)
      case _ =>
        logger.error("Bank details not found in session")
        throw new RuntimeException("Bank details missing from session")
    }

    val accountHolderUnchanged = userAnswers.get(AccountHolderPage) match {
      case Some(accountHolder) =>
        applicationDetails.bankAccountOwnerType.contains(accountHolder.toString)
      case _ =>
        logger.error("Account holder not found in session")
        throw new RuntimeException("Account holder missing from session")
    }

    bankDetailsUnchanged && accountHolderUnchanged
  }

  private def updateApplicationAndRedirect(userAnswers: UserAnswers, applicationDetails: ApplicationDetails)(using RequestHeader) = {
    val request = UpdateApplicationDetailsRequest.from(userAnswers, applicationDetails)
    service
      .updateApplicationDetails(request)
      .flatMap { updateApplicationResponse =>
        val updateSequenceNumber = updateApplicationResponse.updateSequenceNumber
        for {
          seqNumUpdated        <- Future.fromTry(userAnswers.set(UpdateSequenceNumberQuery, updateSequenceNumber))
          bankDetailsCompleted <- Future.fromTry(seqNumUpdated.set(BankDetailsCompletedQuery, true))
          _                    <- sessionRepository.set(bankDetailsCompleted)
        } yield Redirect(controllers.routes.TaskListDashboardController.onPageLoad())
      }
  }
}
