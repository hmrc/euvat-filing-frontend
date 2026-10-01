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
import models.requests.{DataRequest, PurchaseImportListRequest}
import models.{PurchaseImport, UserAnswers}
import navigation.Navigator
import pages.PurchaseImportSummaryPage
import play.api.Logging
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
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
    val userAnswers = request.userAnswers
    val preparedForm = userAnswers.get(PurchaseImportSummaryPage).fold(form)(form.fill)
    retrieveSummaryList(userAnswers).map {
      case (responseList: List[PurchaseImport], totalClaim: BigDecimal) =>
        val summaryListRows = PurchaseImportListSummary.rows(userAnswers, responseList, currencyConfig.currencyConfig)
        Ok(view(preparedForm, summaryListRows, responseList.size, totalClaim))
      case _ =>
        logger.warn("No records found in database")
        Redirect(routes.JourneyRecoveryController.onPageLoad())
    }
  }

  private def retrieveSummaryList(userAnswers: UserAnswers)(implicit request: DataRequest[?]) = {
    userAnswers.get(ClaimApplicationResponseQuery).map(_.applicationId) match {
      case Some(appId) =>
        service
          .getPurchaseImportList(PurchaseImportListRequest(appId))
          .map(response => {
            val totalClaim = response.purchaseImportList.map(_.deductibleVatAmount).sum
            (response.purchaseImportList, totalClaim)
          })
      case _ =>
        logger.warn("Missing or invalid applicationId")
        Future.successful(Seq.empty, BigDecimal(0))
    }
  }

  def onSubmit: Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    form
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, Seq.empty, 0, BigDecimal(0)))),
        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(PurchaseImportSummaryPage, value))
            _              <- sessionRepository.set(updatedAnswers)
          } yield
            if (value) {
              Redirect(routes.PurchaseOrImportController.onPageLoad)
            } else {
              Redirect(routes.JourneyRecoveryController.onPageLoad()) // TODO - redirect to CYA page
            }
      )
  }

}
