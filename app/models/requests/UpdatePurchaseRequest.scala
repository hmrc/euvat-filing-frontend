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

import models.{InvoiceType, PurchaseOrImportType, UserAnswers}
import models.responses.ApplicationResponse
import pages.purchase.*
import play.api.libs.json.{Json, OFormat}
import queries.UpdateSequenceNumberQuery

import java.time.LocalDateTime

case class UpdatePurchaseRequest(
  applicationId: Long,
  itemNumber: Int,
  goodsDescriptionCategory: String,
  goodsDescriptionSubCategory: Option[String],
  goodsDescriptionText: Option[String],
  simplifiedInvoiceIndicator: Option[String],
  supplierName: Option[String],
  supplierAddress1: Option[String],
  supplierAddress2: Option[String],
  supplierAddress3: Option[String],
  supplierVatRegNumber: Option[String],
  supplierTaxIdentifier: Option[String],
  invoiceDate: Option[LocalDateTime],
  invoiceNumber: Option[String],
  currencyCode: Option[String],
  taxableAmount: Option[BigDecimal],
  vatAmount: Option[BigDecimal],
  deductibleVatAmount: Option[BigDecimal],
  updateSequenceNumber: Int
)

object UpdatePurchaseRequest {
  implicit val format: OFormat[UpdatePurchaseRequest] = Json.format[UpdatePurchaseRequest]

  def from(
    answers: UserAnswers,
    purchaseType: PurchaseOrImportType,
    claimResponse: ApplicationResponse
  ): UpdatePurchaseRequest = {
    UpdatePurchaseRequest(
      applicationId               = claimResponse.applicationId,
      itemNumber                  = answers.get(AddPurchaseResponsePage).map(_.itemNumber).getOrElse(0), // TODO: Default to 0 or fail?
      goodsDescriptionCategory    = PurchaseOrImportType.codes(purchaseType),
      goodsDescriptionSubCategory = answers.get(PurchaseSubCategoryPage),
      goodsDescriptionText        = answers.get(DescribeItemsOnInvoicePage),
      simplifiedInvoiceIndicator  = answers.get(InvoiceTypePage).map(_ == InvoiceType.SimplifiedInvoice).map(_.toString),
      supplierName                = answers.get(SuppliersNamePage),
      supplierAddress1            = answers.get(SupplierAddressPage).map(_.line1),
      supplierAddress2            = answers.get(SupplierAddressPage).flatMap(_.line2),
      supplierAddress3            = answers.get(SupplierAddressPage).flatMap(_.line3),
      supplierVatRegNumber        = answers.get(SupplierVatRegistrationNumberPage),
      supplierTaxIdentifier       = answers.get(SupplierTaxIdentifierNumberPage),
      invoiceDate                 = answers.get(InvoiceDatePage).map(_.atStartOfDay()), // TODO: Check if start of day makes sense
      invoiceNumber               = answers.get(InvoiceNumberPage),
      currencyCode                = answers.get(RefundingCurrencyPage),
      taxableAmount               = answers.get(TotalPurchaseAmountBeforeVatPage),
      vatAmount                   = answers.get(TotalVatPaidPage),
      deductibleVatAmount         = answers.get(TotalVatClaimPage),
      updateSequenceNumber        = answers.get(UpdateSequenceNumberQuery).getOrElse(claimResponse.updateSeqNumber)
    )
  }
}
