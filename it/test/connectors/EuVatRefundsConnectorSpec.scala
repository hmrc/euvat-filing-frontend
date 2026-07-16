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

package connectors

import models.responses.TraderKnownFactsResponse
import org.mockito.ArgumentMatchers.*
import org.mockito.Mockito.*
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.http.*
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.{ExecutionContext, Future}

class EuVatRefundsConnectorSpec extends AnyWordSpec with Matchers with MockitoSugar with ScalaFutures {

  implicit val ec: ExecutionContext = ExecutionContext.global
  implicit val hc: HeaderCarrier = HeaderCarrier()

  val mockHttp: HttpClientV2 = mock[HttpClientV2]
  val mockRequestBuilder: RequestBuilder = mock[RequestBuilder]
  val mockConfig: ServicesConfig = mock[ServicesConfig]

  val baseUrl = "http://localhost:9000/euvat-refunds"

  when(mockConfig.baseUrl("euvat-refunds")).thenReturn("http://localhost:9000")

  val connector = new EuVatRefundsConnector(mockConfig, mockHttp)

  "EuVatRefundsConnector.retrieveTradersKnownFacts" should {

    "call the correct URL and return the expected response" in {
      val expected = TraderKnownFactsResponse(
        vatRegNumber = 123,
        traderName   = Some("ABC GmbH"),
        tradeClass   = Some("49200")
      )

      // Mock GET call
      when(mockHttp.get(any())(any())).thenReturn(mockRequestBuilder)

      // Mock execute returning expected response
      when(mockRequestBuilder.execute[TraderKnownFactsResponse](any(), any()))
        .thenReturn(Future.successful(expected))

      val result = connector.retrieveTradersKnownFacts().futureValue

      result shouldBe expected

      verify(mockHttp).get(url"$baseUrl/traders/get-known-facts")
      verify(mockRequestBuilder).execute[TraderKnownFactsResponse](any(), any())
    }

    "propagate failures from the HTTP client" in {
      val failure = new RuntimeException("boom")

      when(mockHttp.get(any())(any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[TraderKnownFactsResponse](any(), any()))
        .thenReturn(Future.failed(failure))

      val result = connector.retrieveTradersKnownFacts()

      whenReady(result.failed) { ex =>
        ex shouldBe failure
      }
    }
  }
}
