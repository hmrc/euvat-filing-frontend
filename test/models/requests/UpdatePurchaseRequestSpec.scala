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

import base.SpecBase
import models.InvoiceType.SimplifiedInvoice
import models.responses.ApplicationResponse
import models.{InvoiceType, Other, PurchaseOrImportType, SupplierAddress}
import org.scalatest.OptionValues
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import pages.purchase.*
import play.api.libs.json.{JsObject, Json}
import queries.{ClaimApplicationResponseQuery, UpdateSequenceNumberQuery}

import java.time.{LocalDate, LocalDateTime}

class UpdatePurchaseRequestSpec extends SpecBase {

  private val year = 2026
  private val month = 5
  private val dayOfMonth = 14
  private val hour = 0
  private val minute = 0
  private val invoiceDate: LocalDateTime = LocalDateTime.of(year, month, dayOfMonth, hour, minute)

  private val request = UpdatePurchaseRequest(
    applicationId               = 404,
    itemNumber                  = 4,
    goodsDescriptionCategory    = "10",
    goodsDescriptionSubCategory = Some("10.4.1"),
    goodsDescriptionText        = Some("Some goods"),
    simplifiedInvoiceIndicator  = Some("true"),
    supplierName                = Some("Finnish International"),
    supplierAddress1            = Some("356 High Street"),
    supplierAddress2            = Some("Rochdale"),
    supplierAddress3            = Some("Country"),
    supplierVatRegNumber        = Some("12345"),
    supplierTaxIdentifier       = Some("123456"),
    invoiceDate                 = Some(invoiceDate),
    invoiceNumber               = Some("0987"),
    currencyCode                = Some("EUR"),
    taxableAmount               = Some(BigDecimal(1000)),
    vatAmount                   = Some(BigDecimal(99)),
    deductibleVatAmount         = Some(BigDecimal(40)),
    updateSequenceNumber        = 1
  )

  "UpdatePurchaseRequest JSON" - {

    "must successfully be parsed to JSON" in {
      val json = Json.toJson(request).as[JsObject]

      Json.prettyPrint(json) mustEqual Json.prettyPrint(Json.parse(s"""
          {
             "applicationId" : ${request.applicationId},
             "itemNumber" : ${request.itemNumber},
             "goodsDescriptionCategory" : "${request.goodsDescriptionCategory}",
             "goodsDescriptionSubCategory" : "${request.goodsDescriptionSubCategory.value}",
             "goodsDescriptionText" : "${request.goodsDescriptionText.value}",
             "simplifiedInvoiceIndicator" : "${request.simplifiedInvoiceIndicator.value}",
             "supplierName" : "${request.supplierName.value}",
             "supplierAddress1" : "${request.supplierAddress1.value}",
             "supplierAddress2" : "${request.supplierAddress2.value}",
             "supplierAddress3" : "${request.supplierAddress3.value}",
             "supplierVatRegNumber" : "${request.supplierVatRegNumber.value}",
             "supplierTaxIdentifier" : "${request.supplierTaxIdentifier.value}",
             "invoiceDate" : "2026-05-14T00:00:00",
             "invoiceNumber" : "${request.invoiceNumber.value}",
             "currencyCode" : "${request.currencyCode.value}",
             "taxableAmount" : ${request.taxableAmount.value},
             "vatAmount" : ${request.vatAmount.value},
             "deductibleVatAmount" : ${request.deductibleVatAmount.value},
             "updateSequenceNumber" : ${request.updateSequenceNumber}
          }
          """))
    }

    "must successfully be parsed from JSON" in {
      Json.toJson(request).as[UpdatePurchaseRequest] mustEqual request
    }
  }
}
