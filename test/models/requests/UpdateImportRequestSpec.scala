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
import play.api.libs.json.{JsObject, Json}

import java.time.LocalDateTime

class UpdateImportRequestSpec extends AnyFreeSpec with Matchers {

  private val request = UpdateImportRequest(
    applicationId               = 404,
    itemNumber                  = 4,
    goodsDescriptionCategory    = "10",
    goodsDescriptionSubCategory = Some("10.4.1"),
    goodsDescriptionText        = Some("Equipment"),
    hasSadReferenceNumber       = Some("Y"),
    sadReferenceNumber          = Some("SAD123456789"),
    referenceInformation        = None,
    supplierName                = Some("Finnish International"),
    supplierAddressLine1        = Some("356 High Street"),
    supplierAddressLine2        = Some("Rochdale"),
    supplierAddressLine3        = None,
    supplierCountryCode         = Some("GB"),
    issuingDate                 = Some(LocalDateTime.of(2026, 5, 14, 0, 0)),
    currencyCode                = Some("EUR"),
    taxableAmount               = Some(BigDecimal(1000)),
    vatAmount                   = Some(BigDecimal(99)),
    deductibleVatAmount         = Some(BigDecimal(40)),
    updateSequenceNumber        = 1
  )

  "UpdateImportRequest JSON" - {

    "must use the field names euvat-refunds expects" in {
      val json = Json.toJson(request).as[JsObject]

      json.keys mustEqual Set(
        "applicationId", "itemNumber", "goodsDescriptionCategory", "goodsDescriptionSubCategory", "goodsDescriptionText",
        "hasSadReferenceNumber", "sadReferenceNumber", "supplierName", "supplierAddressLine1", "supplierAddressLine2",
        "supplierCountryCode", "issuingDate", "currencyCode", "taxableAmount", "vatAmount", "deductibleVatAmount",
        "updateSequenceNumber"
      )
      (json \\ "issuingDate").head.as[String] mustEqual "2026-05-14T00:00:00"
    }

    "must round trip through JSON" in {
      Json.toJson(request).as[UpdateImportRequest] mustEqual request
    }

    "must fail to read when a mandatory field is missing" in {
      (Json.toJson(request).as[JsObject] - "itemNumber").validate[UpdateImportRequest].isError mustBe true
    }
  }
}
