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
import connectors.EuVatRefundsConnector
import models.requests.LatestApplicationRequest
import models.responses.{LatestApplicationResponse, TraderKnownFactsResponse}
import org.mockito.ArgumentMatchers.*
import org.mockito.Mockito.*
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDateTime
import scala.concurrent.{ExecutionContext, Future}

class EuVatRefundsServiceSpec extends SpecBase with MockitoSugar with ScalaFutures {

  implicit val ec: ExecutionContext = ExecutionContext.global
  implicit val hc: HeaderCarrier = HeaderCarrier()

  val mockConnector: EuVatRefundsConnector = mock[EuVatRefundsConnector]
  val mockConfig: FrontendAppConfig = mock[FrontendAppConfig]
  val service = new EuVatRefundsService(mockConnector, mockConfig)

  "EuVatRefundsService.retrieveTraderKnownFacts" - {
    val expected = TraderKnownFactsResponse(
      vatRegNumber = 123,
      traderName   = Some("ABC GmbH"),
      tradeClass   = Some("49200")
    )

    "should retrieve existing details from Cache first" in {
      when(mockConnector.retrieveBusinessActivityCode()(any()))
        .thenReturn(Future.successful(expected))

      val result = service.retrieveTraderKnownFacts()(any()).futureValue
      result mustEqual expected
    }
  }
  "EuVatRefundsService.getLatestApplications" - {

    val request = LatestApplicationRequest(
      applicantVatRegNumber = "123456789",
      refundingCountry = "LV",
      startDate = LocalDateTime.of(2025, 2, 1, 0, 0),
      endDate = LocalDateTime.of(2025, 5, 31, 0, 0),
      representativeId = "rep123",
      maxNumber = 10,
      orderBy = None,
      sortOrder = None,
      startAt = None
    )

    val expectedResponse = LatestApplicationResponse(
      applications = List.empty,
      totalApplication = 0
    )

    "should return latest applications from the connector" in {
      when(mockConnector.getLatestApplications(any())(any()))
        .thenReturn(Future.successful(expectedResponse))

      val result = service.getLatestApplications(request)(hc).futureValue
      result mustEqual expectedResponse
    }

    "should propagate an exception from the connector" in {
      val failure = new RuntimeException("Connector failed")

      when(mockConnector.getLatestApplications(any())(any()))
        .thenReturn(Future.failed(failure))

      val result = service.getLatestApplications(request)

      whenReady(result.failed) { ex =>
        ex mustEqual failure
      }
    }
  }
}
