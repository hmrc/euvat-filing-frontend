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

case class ImportDetailsResponse(
  applicationId: Long,
  itemNumber: Int,
  issuingDate: String,
  supplierName: Option[String],
  supplierAddressLine1: Option[String],
  supplierAddressLine2: Option[String],
  supplierAddressLine3: Option[String],
  supplierCountryCode: Option[String],
  goodsDescriptionCategory: String,
  currencyCode: String,
  taxableAmount: BigDecimal,
  vatAmount: BigDecimal,
  deductibleVatAmount: BigDecimal,
  hasSadReferenceNumber: Option[String],
  sadReferenceNumber: Option[String],
  referenceInformation: Option[String],
  goodsDescriptionText: Option[String],
  goodsDescriptionSubcategory: Option[String],
  updateSequenceNumber: Int
)

object ImportDetailsResponse {
  implicit val format: OFormat[ImportDetailsResponse] = Json.format[ImportDetailsResponse]
}