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

import play.api.libs.json.{Json, OFormat}

case class PurchaseDetailsResponse(
  applicationId: Long,
  itemNumber: Int,
  goodsDescriptionCode: String,
  goodsDescriptionSubCode: String,
  goodsDescriptionText: String,
  simplifiedInvoiceIndicator: String,
  supplierName: String,
  supplierAddressLine1: String,
  supplierAddressLine2: String,
  supplierAddressLine3: String,
  supplierVatNumber: String,
  supplierTaxIdentifier: String,
  invoiceDate: String,
  invoiceNumber: String,
  currencyCode: String,
  taxableAmount: BigDecimal,
  vatAmount: BigDecimal,
  deductibleVatAmount: BigDecimal,
  updateSequenceNumber: Int
)

object PurchaseDetailsResponse {
  implicit val format: OFormat[PurchaseDetailsResponse] = Json.format[PurchaseDetailsResponse]
}