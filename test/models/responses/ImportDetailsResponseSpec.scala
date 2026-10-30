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

class ImportDetailsResponseSpec extends AnyFreeSpec with Matchers {

  "ImportDetailsResponse JSON" - {

    "must read the get-import-details response from euvat-refunds" in {
      val json = Json.parse("""
        {
          "applicationId": 404,
          "itemNumber": 4,
          "issuingDate": "2026-10-01T00:00:00",
          "supplierName": "Example Supplier",
          "supplierAddressLine1": "10 Street",
          "supplierCountryCode": "IN",
          "goodsDescriptionCategory": "10",
          "goodsDescriptionText": "Imported equipment",
          "hasSadReferenceNumber": "true",
          "sadReferenceNumber": "SAD123",
          "currencyCode": "EUR",
          "taxableAmount": 100,
          "vatAmount": 20,
          "deductibleVatAmount": 10,
          "updateSequenceNumber": 2,
          "periodStartDate": "2026-01-01T00:00:00"
        }
      """)

      val result = json.as[ImportDetailsResponse]

      result.issuingDate mustBe Some(LocalDateTime.of(2026, 10, 1, 0, 0))
      result.hasSadReferenceNumber mustBe Some("true")
      result.goodsDescriptionSubcategory mustBe None
      result.deductibleVatAmount mustBe Some(BigDecimal(10))
      result.updateSequenceNumber mustBe 2
    }

    "must fail to read when the item number is missing" in {
      Json.obj("applicationId" -> 404, "goodsDescriptionCategory" -> "1", "updateSequenceNumber" -> 1)
        .validate[ImportDetailsResponse]
        .isError mustBe true
    }
  }
}
