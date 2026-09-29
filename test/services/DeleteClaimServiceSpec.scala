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

package services

import base.SpecBase
import config.FrontendAppConfig
import models.requests.DeleteApplicationRequest
import models.responses.ApplicationResponse
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.{verify, when}
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.Json
import play.api.test.Helpers.*
import queries.{ClaimApplicationResponseQuery, UpdateSequenceNumberQuery}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class DeleteClaimServiceSpec extends SpecBase with MockitoSugar with ScalaFutures {

  implicit val ec: ExecutionContext = ExecutionContext.global
  implicit val hc: HeaderCarrier = HeaderCarrier()

  val mockConfig: FrontendAppConfig = mock[FrontendAppConfig]
  when(mockConfig.claimDashboardUrl).thenReturn("/manage")

  val service = new DeleteClaimService(mockEuVatRefundsService, mockSessionRepository, mockConfig)

  "DeleteClaimService.deleteAndRedirect" - {

    "should call connector with provided update sequence number, clear session and redirect to management" in {
      val appResp = ApplicationResponse(123L, "GB123", 5)
      val ua1 = emptyUserAnswers.set(ClaimApplicationResponseQuery, appResp).success.value
      val ua = ua1.set(UpdateSequenceNumberQuery, 9).success.value

      when(mockEuVatRefundsService.deleteApplication(any())(any())).thenReturn(Future.successful(()))

      val resultF = service.deleteAndRedirect(ua)

      redirectLocation(resultF).value mustEqual "/manage"

      val captor = ArgumentCaptor.forClass(classOf[models.UserAnswers])
      verify(mockSessionRepository).set(captor.capture())
      captor.getValue.data mustEqual Json.obj()
      verify(mockEuVatRefundsService).deleteApplication(DeleteApplicationRequest(123L, 9))(hc)
    }

    "should fallback to application updateSeqNumber when UpdateSequenceNumberQuery absent" in {
      val appResp = ApplicationResponse(222L, "GB222", 7)
      val ua = emptyUserAnswers.set(ClaimApplicationResponseQuery, appResp).success.value

      when(mockEuVatRefundsService.deleteApplication(any())(any())).thenReturn(Future.successful(()))

      val resultF = service.deleteAndRedirect(ua)

      redirectLocation(resultF).value mustEqual "/manage"
      verify(mockEuVatRefundsService).deleteApplication(DeleteApplicationRequest(222L, 7))(hc)
    }

    "should redirect to Journey Recovery when ClaimApplicationResponse is missing" in {
      val ua = emptyUserAnswers

      val resultF = service.deleteAndRedirect(ua)

      redirectLocation(resultF).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
    }

    "should recover and redirect to Journey Recovery when connector fails" in {
      val appResp = ApplicationResponse(321L, "GB321", 2)
      val ua = emptyUserAnswers.set(ClaimApplicationResponseQuery, appResp).success.value

      when(mockEuVatRefundsService.deleteApplication(any())(any())).thenReturn(Future.failed(new RuntimeException("boom")))

      val resultF = service.deleteAndRedirect(ua)

      redirectLocation(resultF).value must include(controllers.routes.JourneyRecoveryController.onPageLoad().url)
    }

  }

}
