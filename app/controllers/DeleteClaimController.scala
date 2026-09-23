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

import config.FrontendAppConfig
import controllers.actions.*
import forms.DeleteClaimFormProvider
import models.requests.DeleteApplicationRequest
import navigation.Navigator
import pages.{RefundPeriodPage, RefundingCountryNamePage}
import play.api.Logging
import play.api.data.Form
import play.api.i18n.{I18nSupport, Lang, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import services.EuVatRefundsService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import utils.DateTimeFormats.shortMonthYearFormat
import views.html.DeleteClaimView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DeleteClaimController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: DeleteClaimFormProvider,
  appConfig: FrontendAppConfig,
  euVatRefundsService: EuVatRefundsService,
  val controllerComponents: MessagesControllerComponents,
  view: DeleteClaimView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  val form: Form[Boolean] = formProvider()

  private def extractSummaryData(userAnswers: models.UserAnswers)(implicit messages: Messages, lang: Lang): (String, String, String) = {
    val memberState = userAnswers.get(RefundingCountryNamePage).getOrElse("")
    val startDate = userAnswers.get(RefundPeriodPage).map(_.startDate.format(shortMonthYearFormat())).getOrElse("")
    val endDate = userAnswers.get(RefundPeriodPage).map(_.endDate.format(shortMonthYearFormat())).getOrElse("")
    (memberState, startDate, endDate)
  }

  def onPageLoad: Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
    implicit val messages: Messages = messagesApi.preferred(request)
    implicit val lang: Lang = messages.lang

    val (memberState, startDate, endDate) = extractSummaryData(request.userAnswers)

    Ok(view(form, memberState, startDate, endDate))
  }

  def onSubmit: Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    implicit val messages: Messages = messagesApi.preferred(request)
    implicit val lang: Lang = messages.lang

    form
      .bindFromRequest()
      .fold(
        formWithErrors => {
          val (memberState, startDate, endDate) = extractSummaryData(request.userAnswers)
          Future.successful(BadRequest(view(formWithErrors, memberState, startDate, endDate)))
        },
        value =>
          if (value) {
            implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

            val maybeApp = request.userAnswers.get(queries.ClaimApplicationResponseQuery)

            maybeApp match {
              case Some(appResp) =>
                val deleteReq = DeleteApplicationRequest(appResp.applicationId, appResp.updateSeqNumber)
                euVatRefundsService
                  .deleteApplication(deleteReq)
                  .flatMap { _ =>
                    val cleared = request.userAnswers.clear()
                    sessionRepository.set(cleared).map(_ => Redirect(appConfig.claimDashboardUrl))
                  }
                  .recover { case ex =>
                    logger.error("Error deleting claim", ex)
                    Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
                  }
              case None =>
                logger.warn("Missing applicationId for delete-claim")
                Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
            }
          } else {
            Future.successful(Redirect(controllers.routes.TaskListDashboardController.onPageLoad()))
          }
      )
  }
}
