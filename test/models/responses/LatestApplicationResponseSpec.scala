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

package models.responses

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.Json

import java.time.LocalDateTime

class LatestApplicationResponseSpec extends AnyFreeSpec with Matchers {

  private val sampleApplication = LatestApplication(
    applicationId        = 133,
    refundingCountryCode = "LV",
    periodStartDate      = LocalDateTime.of(2025, 2, 1, 0, 0),
    periodEndDate        = LocalDateTime.of(2025, 5, 31, 23, 59),
    applicationNumber    = "GB0000000000000133",
    applicationStatus    = "D",
    submissionStatus     = "S",
    applicationVersion   = LocalDateTime.of(2025, 2, 11, 10, 38)
  )

  private val sampleResponse = LatestApplicationResponse(
    applications     = List(sampleApplication),
    totalApplication = 1
  )

  "LatestApplicationResponse" - {

    "must serialise to JSON correctly" in {
      val json = Json.toJson(sampleResponse)
      (json \ "totalApplication").as[Int] mustEqual 1
      (json \ "applications" \ 0 \ "applicationId").as[Long] mustEqual 133
      (json \ "applications" \ 0 \ "refundingCountryCode").as[String] mustEqual "LV"
    }

    "must deserialise from JSON correctly" in {
      val json = Json.obj(
        "applications" -> Json.arr(
          Json.obj(
            "applicationId"        -> 133,
            "refundingCountryCode" -> "LV",
            "periodStartDate"      -> "2025-02-01T00:00:00",
            "periodEndDate"        -> "2025-05-31T23:59:00",
            "applicationNumber"    -> "GB0000000000000133",
            "applicationStatus"    -> "D",
            "submissionStatus"     -> "S",
            "applicationVersion"   -> "2025-02-11T10:38:00"
          )
        ),
        "totalApplication" -> 1
      )
      json.as[LatestApplicationResponse] mustEqual sampleResponse
    }

    "must round-trip through JSON" in {
      Json.toJson(sampleResponse).as[LatestApplicationResponse] mustEqual sampleResponse
    }
  }
}
