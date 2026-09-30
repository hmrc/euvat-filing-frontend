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

package utils

import models.UserAnswers
import models.requests.DeleteApplicationRequest
import play.api.Logging
import play.api.mvc.Result
import repositories.SessionRepository
import config.FrontendAppConfig
import play.api.mvc.Results.Redirect
import queries.{ClaimApplicationResponseQuery, UpdateSequenceNumberQuery}
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DeleteClaimHelper @Inject() (
  euVatRefundsService: services.EuVatRefundsService,
  sessionRepository: SessionRepository,
  appConfig: FrontendAppConfig
)(implicit ec: ExecutionContext)
    extends Logging {

  def deleteAndRedirect(userAnswers: UserAnswers)(implicit hc: HeaderCarrier): Future[Result] =
    userAnswers.get(ClaimApplicationResponseQuery) match {
      case Some(appResp) =>
        val seqNumber = userAnswers.get(UpdateSequenceNumberQuery).getOrElse(appResp.updateSeqNumber)
        val deleteReq = DeleteApplicationRequest(appResp.applicationId, seqNumber)

        euVatRefundsService
          .deleteApplication(deleteReq)
          .flatMap { _ =>
            val cleared = userAnswers.clear()
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
}
