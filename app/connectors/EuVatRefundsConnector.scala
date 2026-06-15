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

import models.requests.LatestApplicationRequest
import models.responses.{LatestApplicationResponse, TraderKnownFactsResponse}
import play.api.Logging
import play.api.http.Status.OK
import play.api.libs.json.Json
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpReadsInstances, HttpResponse, StringContextOps, UpstreamErrorResponse}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

@Singleton
class EuVatRefundsConnector @Inject() (config: ServicesConfig, http: HttpClientV2)(implicit ec: ExecutionContext)
    extends HttpReadsInstances
    with Logging {

  private val euVatRefundsBaseUrl: String = config.baseUrl("euvat-refunds") + "/euvat-refunds"

  def retrieveBusinessActivityCode()(implicit hc: HeaderCarrier): Future[TraderKnownFactsResponse] = {
    http
      .get(url"$euVatRefundsBaseUrl/traders/getKnownFacts")(hc)
      .execute[TraderKnownFactsResponse]
  }

  def getLatestApplications(request: LatestApplicationRequest)(implicit hc: HeaderCarrier): Future[LatestApplicationResponse] =
    http
      .post(url"$euVatRefundsBaseUrl/get-latest-application")
      .withBody(Json.toJson(request))
      .execute[LatestApplicationResponse]

}
