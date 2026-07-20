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

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.*

import java.time.LocalDateTime

class TraderKnownFactsResponseSpec extends AnyWordSpec with Matchers {

  "TraderKnownFactsResponse JSON format" should {

    "serialize to JSON correctly" in {
      val model = TraderKnownFactsResponse(
        vatRegNumber           = 123456789,
        traderName             = Some("ABC GmbH"),
        addressLine1           = Some("Line 1"),
        postCode               = Some("AB12 3CD"),
        tradeClass             = Some("8765"),
        dateOfRegistration     = Some(LocalDateTime.of(2020, 5, 10, 12, 30)),
        missingTraderIndicator = Some("N"),
        singleMarketIndicator  = Some(1)
      )

      val json = Json.toJson(model)

      (json \ "vatRegNumber").as[Int]              shouldBe 123456789
      (json \ "traderName").as[String]             shouldBe "ABC GmbH"
      (json \ "addressLine1").as[String]           shouldBe "Line 1"
      (json \ "postCode").as[String]               shouldBe "AB12 3CD"
      (json \ "tradeClass").as[String]             shouldBe "8765"
      (json \ "dateOfRegistration").as[String]     shouldBe "2020-05-10T12:30:00"
      (json \ "missingTraderIndicator").as[String] shouldBe "N"
      (json \ "singleMarketIndicator").as[Int]     shouldBe 1
    }

    "deserialize from JSON correctly" in {
      val json = Json.parse(
        """
          |{
          |  "vatRegNumber": 987654321,
          |  "traderName": "XYZ Ltd",
          |  "tradeClass": "1234",
          |  "dateOfDeregistration": "2021-01-15T09:45:00",
          |  "singleMarketIndicator": 0
          |}
          |""".stripMargin
      )

      val model = json.as[TraderKnownFactsResponse]

      model.vatRegNumber          shouldBe 987654321
      model.traderName            shouldBe Some("XYZ Ltd")
      model.tradeClass            shouldBe Some("1234")
      model.dateOfDeregistration  shouldBe Some(LocalDateTime.of(2021, 1, 15, 9, 45))
      model.singleMarketIndicator shouldBe Some(0)
    }

    "handle missing optional fields" in {
      val json = Json.parse(
        """
          |{
          |  "vatRegNumber": 111222333
          |}
          |""".stripMargin
      )

      val model = json.as[TraderKnownFactsResponse]

      model.vatRegNumber          shouldBe 111222333
      model.traderName            shouldBe None
      model.addressLine1          shouldBe None
      model.tradeClass            shouldBe None
      model.dateOfRegistration    shouldBe None
      model.singleMarketIndicator shouldBe None
    }

    "round-trip JSON (serialize then deserialize)" in {
      val original = TraderKnownFactsResponse(
        vatRegNumber       = 555666777,
        traderName         = Some("Round Trip Ltd"),
        tradeClass         = Some("9999"),
        dateOfRegistration = Some(LocalDateTime.of(2022, 3, 1, 8, 0))
      )

      val json = Json.toJson(original)
      val parsed = json.as[TraderKnownFactsResponse]

      parsed shouldBe original
    }
  }
}
