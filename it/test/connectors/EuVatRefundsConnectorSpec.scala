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

  "EuVatRefundsConnector.retrieveBusinessActivityCode" should {

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

      val result = connector.retrieveBusinessActivityCode().futureValue

      result shouldBe expected

      verify(mockHttp).get(url"$baseUrl/traders/getKnownFacts")
      verify(mockRequestBuilder).execute[TraderKnownFactsResponse](any(), any())
    }

    "propagate failures from the HTTP client" in {
      val failure = new RuntimeException("boom")

      when(mockHttp.get(any())(any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[TraderKnownFactsResponse](any(), any()))
        .thenReturn(Future.failed(failure))

      val result = connector.retrieveBusinessActivityCode()

      whenReady(result.failed) { ex =>
        ex shouldBe failure
      }
    }
  }
}
