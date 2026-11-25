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

package models.requests

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.Json

class GetAllAttachmentsRequestSpec extends AnyFreeSpec with Matchers {

  private val fullRequest = GetAllAttachmentsRequest(
    applicationId = 104,
    orderBy       = Some(0),
    sortOrder     = Some("ASC"),
    startAt       = Some(0),
    maxNumber     = Some(10)
  )

  "GetAllAttachmentsRequest" - {

    "must round-trip through JSON" in {
      Json.toJson(fullRequest).as[GetAllAttachmentsRequest] mustEqual fullRequest
    }

    "must read a request with only the applicationId" in {
      Json.obj("applicationId" -> 104).as[GetAllAttachmentsRequest] mustEqual GetAllAttachmentsRequest(applicationId = 104)
    }

    "must fail to read a request without an applicationId" in {
      Json.obj("orderBy" -> 0).validate[GetAllAttachmentsRequest].isError mustBe true
    }
  }
}
