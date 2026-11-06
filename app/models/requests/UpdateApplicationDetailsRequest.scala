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

import models.{ApplicationDetails, EncryptionStatus, UserAnswers}
import pages.bankdetails.{AccountHolderPage, BankDetailsPage}
import play.api.libs.json.{Json, OFormat}

import java.time.LocalDateTime

case class UpdateApplicationDetailsRequest(
  applicationId: Long,
  applicationLanguage: String,
  refundingCountry: String,
  periodStartDate: LocalDateTime,
  periodEndDate: LocalDateTime,
  applicantEmailAddress: String,
  applicantPhoneNumber: Option[String],
  representativeCountry: Option[String],
  representativeEmailAddress: Option[String],
  representativePhoneNumber: Option[String],
  bankAccountOwnerName: Option[String],
  bankAccountOwnerType: Option[String],
  ibanCode: Option[String],
  bicCode: Option[String],
  bankAccountCurrencyCode: Option[String],
  businessActivityCode2: Option[String],
  businessActivityCode3: Option[String],
  cipherText: Option[String],
  encryptionStatus: Option[String],
  updateSequenceNumber: Int
)

object UpdateApplicationDetailsRequest {
  implicit val format: OFormat[UpdateApplicationDetailsRequest] = Json.format[UpdateApplicationDetailsRequest]

  def from(userAnswers: UserAnswers, applicationDetails: ApplicationDetails): UpdateApplicationDetailsRequest = {

    val bankAccountHolderType = userAnswers.get(AccountHolderPage).map(_.toString)
    val maybeBankDetails = userAnswers.get(BankDetailsPage)
    val countryCode = applicationDetails.refundingCountryCode.getOrElse(throw new RuntimeException("Country code missing"))
    val languageCode = applicationDetails.applicationLanguage.getOrElse(throw new RuntimeException("Language code missing"))
    val refundPeriodStartDate = applicationDetails.periodStartDate.getOrElse(throw new RuntimeException("Refund period start date missing"))
    val refundPeriodEndDate = applicationDetails.periodEndDate.getOrElse(throw new RuntimeException("Refund period end date missing"))
    val applicantEmailAddress = applicationDetails.applicantEmailAddress.getOrElse(throw new RuntimeException("Applicant email missing"))

    UpdateApplicationDetailsRequest(
      applicationId              = applicationDetails.applicationId,
      applicationLanguage        = languageCode,
      refundingCountry           = countryCode,
      periodStartDate            = refundPeriodStartDate,
      periodEndDate              = refundPeriodEndDate,
      applicantEmailAddress      = applicantEmailAddress,
      applicantPhoneNumber       = applicationDetails.applicantTelephoneNumber,
      representativeCountry      = applicationDetails.representativeCountry,
      representativeEmailAddress = applicationDetails.representativeEmailAddress,
      representativePhoneNumber  = applicationDetails.representativeTelephoneNum,
      bankAccountOwnerName       = maybeBankDetails.map(_.NameOnTheAccount),
      bankAccountOwnerType       = bankAccountHolderType,
      ibanCode                   = maybeBankDetails.map(_.accountNumber),
      bicCode                    = maybeBankDetails.map(_.BIC),
      bankAccountCurrencyCode    = maybeBankDetails.map(_.CurrencyOfAccount),
      businessActivityCode2      = applicationDetails.businessActivityCode2,
      businessActivityCode3      = applicationDetails.businessActivityCode3,
      cipherText                 = applicationDetails.cipherText,
      encryptionStatus           = applicationDetails.encryptionStatus,
      updateSequenceNumber       = applicationDetails.updateSeqNumber
    )
  }
}
