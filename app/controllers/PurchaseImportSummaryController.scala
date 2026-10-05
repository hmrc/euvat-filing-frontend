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
import models.requests.PurchaseImportListRequest
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
    userAnswers.get(ClaimApplicationResponseQuery).map(_.applicationId) match {
      case Some(appId) =>
        service.getPurchaseImportList(PurchaseImportListRequest(appId)).map { summaryResponse =>
          val summaryListRows = PurchaseImportListSummary.rows(userAnswers, summaryResponse.purchaseImportList, currencyConfig.currencyConfig)
          Ok(view(preparedForm, summaryListRows, summaryResponse.totalItems, summaryResponse.totalVatClaims))
        }
      case _ =>
        logger.warn("Missing or invalid applicationId")
        Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
    }
  }

  def onSubmit: Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    form
      .bindFromRequest()
      .fold(
        formWithErrors => {
          request.userAnswers
            .get(ClaimApplicationResponseQuery)
            .map(_.applicationId)
            .fold {
              logger.warn("Missing applicationId")
              Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
            } { applicationId =>
              service
                .getPurchaseImportList(PurchaseImportListRequest(applicationId))
                .map { summaryResponse =>
                  val summaryListRows =
                    PurchaseImportListSummary.rows(request.userAnswers, summaryResponse.purchaseImportList, currencyConfig.currencyConfig)
                  BadRequest(view(formWithErrors, summaryListRows, summaryResponse.totalItems, summaryResponse.totalVatClaims))
                }
            }
        },
        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(PurchaseImportSummaryPage, value))
            _              <- sessionRepository.set(updatedAnswers)
          } yield
            if (value) {
              Redirect(routes.PurchaseOrImportController.onPageLoad)
            } else {
              Redirect(routes.TaskListDashboardController.onPageLoad())
            }
      )
  }

}
