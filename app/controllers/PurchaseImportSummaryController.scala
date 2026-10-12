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
import forms.PurchaseImportSummaryFormProvider
import models.UserAnswers
import models.requests.{DataRequest, PurchaseImportListRequest}
import navigation.Navigator
import pages.{PurchaseImportSummaryPage, PurchaseOrImportPage}
import play.api.Logging
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import queries.ClaimApplicationResponseQuery
import repositories.SessionRepository
import services.EuVatRefundsService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.CurrencyConfig
import viewmodels.checkAnswers.PurchaseImportListSummary
import views.html.PurchaseImportSummaryView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PurchaseImportSummaryController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  val controllerComponents: MessagesControllerComponents,
  formProvider: PurchaseImportSummaryFormProvider,
  view: PurchaseImportSummaryView,
  currencyConfig: CurrencyConfig,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  service: EuVatRefundsService
)(using ExecutionContext)
    extends FrontendBaseController
    with Logging
    with I18nSupport {

  val form: Form[Boolean] = formProvider()

  def onPageLoad: Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    val preparedForm = request.userAnswers.get(PurchaseImportSummaryPage).fold(form)(form.fill)
    renderSummary(userAnswers = request.userAnswers, formToRender = preparedForm, badRequest = false)
  }

  def onSubmit: Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    val boundForm = form.bindFromRequest()
    boundForm.fold(
      formWithErrors => renderSummary(userAnswers = request.userAnswers, formToRender = formWithErrors, badRequest = true),
      value => saveAndRedirect(value)
    )
  }

  private def renderSummary(
    userAnswers: UserAnswers,
    formToRender: Form[Boolean],
    badRequest: Boolean
  )(implicit request: DataRequest[AnyContent]): Future[Result] = {
    getApplicationId(userAnswers).fold(recoveryPage("Missing or invalid applicationId")) { applicationId =>
      service
        .getPurchaseImportList(PurchaseImportListRequest(applicationId))
        .map { summaryResponse =>
          val summaryListRows = PurchaseImportListSummary.rows(userAnswers, summaryResponse.purchaseImportList, currencyConfig.currencyConfig)
          val page = view(formToRender, summaryListRows, summaryResponse.totalItems, summaryResponse.totalVatClaims)
          if (badRequest) BadRequest(page) else Ok(page)
        }
    }
  }

  private def saveAndRedirect(value: Boolean)(implicit request: DataRequest[AnyContent]): Future[Result] = {
    for {
      answersWithSummary <- Future.fromTry(request.userAnswers.set(PurchaseImportSummaryPage, value))
      updatedAnswers     <- Future.fromTry(answersWithSummary.remove(PurchaseOrImportPage))
      _                  <- sessionRepository.set(updatedAnswers)
      result <-
        if (value) {
          Future.successful(Redirect(routes.PurchaseOrImportController.onPageLoad))
        } else {
          validateDeductibleVatAmounts(updatedAnswers, value)
        }
    } yield result
  }

  private def validateDeductibleVatAmounts(updatedAnswers: UserAnswers, submittedValue: Boolean)(implicit
    request: DataRequest[AnyContent]
  ): Future[Result] = {
    getApplicationId(updatedAnswers).fold(recoveryPage("Missing or invalid applicationId")) { applicationId =>
      service
        .getPurchaseImportList(PurchaseImportListRequest(applicationId))
        .map { summaryResponse =>
          val hasAnyZeroDeductibleVatAmount = summaryResponse.purchaseImportList.exists(_.deductibleVatAmount <= 0)
          if (hasAnyZeroDeductibleVatAmount) {
            val formWithError = form.fill(submittedValue).withError("", "purchaseImportSummary.error.incomplete")
            val summaryListRows = PurchaseImportListSummary.rows(updatedAnswers, summaryResponse.purchaseImportList, currencyConfig.currencyConfig)
            BadRequest(view(formWithError, summaryListRows, summaryResponse.totalItems, summaryResponse.totalVatClaims))
          } else {
            Redirect(routes.TaskListDashboardController.onPageLoad())
          }
        }
    }
  }

  private def recoveryPage(msg: String): Future[Result] = {
    logger.warn(msg)
    Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
  }

  private def getApplicationId(userAnswers: UserAnswers): Option[Long] =
    userAnswers.get(ClaimApplicationResponseQuery).map(_.applicationId)

}
