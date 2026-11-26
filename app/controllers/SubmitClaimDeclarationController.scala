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

import controllers.actions.{DataRequiredAction, DataRetrievalAction, IdentifierAction}
import models.NormalMode
import models.requests.DataRequest
import navigation.Navigator
import pages.SubmitClaimDeclarationPage
import pages.claim.{RefundPeriodPage, RefundingCountryNamePage}
import play.api.Logging
import play.api.i18n.{I18nSupport, Lang, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.ClaimApplicationResponseQuery
import services.EuVatRefundsService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.DateTimeFormats.shortMonthYearFormat
import views.html.SubmitClaimDeclarationView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SubmitClaimDeclarationController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  val controllerComponents: MessagesControllerComponents,
  navigator: Navigator,
  service: EuVatRefundsService,
  view: SubmitClaimDeclarationView
)(using ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad: Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    val details = for {
      claim   <- request.userAnswers.get(ClaimApplicationResponseQuery)
      country <- request.userAnswers.get(RefundingCountryNamePage)
      period  <- request.userAnswers.get(RefundPeriodPage)
    } yield (claim.applicationId, country, period)

    details match {
      case Some((applicationId, country, period)) =>
        service
          .getApplicationCounts(applicationId)
          .map { counts =>
            implicit val lang: Lang = messagesApi.preferred(request).lang
            Ok(
              view(
                country,
                period.startDate.format(shortMonthYearFormat()),
                period.endDate.format(shortMonthYearFormat()),
                counts
              )
            )
          }
          .recover { case error =>
            logger.error(s"Unable to retrieve application counts for applicationId $applicationId", error)
            Redirect(routes.JourneyRecoveryController.onPageLoad())
          }
      case None =>
        logger.warn("Missing claim application, refunding country or refund period for declaration")
        Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
    }
  }

  def onSubmit: Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
    Redirect(navigator.nextPage(SubmitClaimDeclarationPage, NormalMode, request.userAnswers))
  }
}
