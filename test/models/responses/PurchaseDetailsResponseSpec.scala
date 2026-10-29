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
import play.api.libs.json.Json

class PurchaseDetailsResponseSpec extends AnyWordSpec with Matchers {

  "PurchaseDetailsResponse JSON format" should {

    "deserialize a response with omitted optional fields" in {
      Json
        .obj(
          "goodsDescriptionCode" -> "1",
          "updateSequenceNumber" -> 7
        )
        .as[PurchaseDetailsResponse] shouldBe PurchaseDetailsResponse(
        goodsDescriptionCode       = "1",
        goodsDescriptionSubCode    = None,
        goodsDescriptionText       = None,
        simplifiedInvoiceIndicator = None,
        supplierName               = None,
        supplierAddressLine1       = None,
        supplierAddressLine2       = None,
        supplierAddressLine3       = None,
        supplierVatNumber          = None,
        supplierTaxIdentifier      = None,
        invoiceDate                = None,
        invoiceNumber              = None,
        currencyCode               = None,
        taxableAmount              = None,
        vatAmount                  = None,
        deductibleVatAmount        = None,
        updateSequenceNumber       = 7
      )
    }
  }
}
