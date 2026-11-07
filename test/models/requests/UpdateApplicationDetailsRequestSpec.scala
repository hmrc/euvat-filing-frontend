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

import models.responses.ApplicationDetails
import models.{AccountHolder, BankDetails, EncryptionStatus, UserAnswers}
import org.mockito.Mockito.when
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{OptionValues, TryValues}
import org.scalatestplus.mockito.MockitoSugar
import pages.bankdetails.{AccountHolderPage, BankDetailsPage}
import play.api.libs.json.{JsObject, Json}
import utils.BankDetailsEncryption

import java.time.{Clock, LocalDateTime}

class UpdateApplicationDetailsRequestSpec extends AnyWordSpec with Matchers with TryValues with OptionValues with MockitoSugar {

  private val fullRequest = UpdateApplicationDetailsRequest(
    applicationId              = 133,
    applicationLanguage        = "en",
    refundingCountry           = "LV",
    periodStartDate            = LocalDateTime.of(2011, 6, 1, 0, 0, 0),
    periodEndDate              = LocalDateTime.of(2011, 10, 31, 23, 59, 59),
    applicantEmailAddress      = "test@hotmail.com",
    applicantPhoneNumber       = Some("01952233299"),
    representativeCountry      = Some("AO"),
    representativeEmailAddress = Some("johnbloggs@hotmail.com"),
    representativePhoneNumber  = Some("01952233248"),
    bankAccountOwnerName       = Some("test account 1"),
    bankAccountOwnerType       = Some("applicant"),
    ibanCode                   = Some("RoCe/FJvoYSi6rSsMQ5D8UU9QirCB9MZXjC6wuYDyhc="),
    bicCode                    = Some("FOJFkUjXAjPmZTcK4WRCyw=="),
    bankAccountCurrencyCode    = Some("EUR"),
    businessActivityCode2      = Some("4477"),
    businessActivityCode3      = Some("2233"),
    cipherText                 = Some("badd2539694a0b3fd547b7cfcb77eefe"),
    encryptionStatus           = Some("M"),
    updateSequenceNumber       = 30
  )

  private val fullJson: JsObject = Json.obj(
    "applicationId"              -> 133,
    "applicationLanguage"        -> "en",
    "refundingCountry"           -> "LV",
    "periodStartDate"            -> "2011-06-01T00:00:00",
    "periodEndDate"              -> "2011-10-31T23:59:59",
    "applicantEmailAddress"      -> "test@hotmail.com",
    "applicantPhoneNumber"       -> "01952233299",
    "representativeCountry"      -> "AO",
    "representativeEmailAddress" -> "johnbloggs@hotmail.com",
    "representativePhoneNumber"  -> "01952233248",
    "bankAccountOwnerName"       -> "test account 1",
    "bankAccountOwnerType"       -> "applicant",
    "ibanCode"                   -> "RoCe/FJvoYSi6rSsMQ5D8UU9QirCB9MZXjC6wuYDyhc=",
    "bicCode"                    -> "FOJFkUjXAjPmZTcK4WRCyw==",
    "bankAccountCurrencyCode"    -> "EUR",
    "businessActivityCode2"      -> "4477",
    "businessActivityCode3"      -> "2233",
    "cipherText"                 -> "badd2539694a0b3fd547b7cfcb77eefe",
    "encryptionStatus"           -> "M",
    "updateSequenceNumber"       -> 30
  )

  private val mandatoryOnlyJson: JsObject = Json.obj(
    "applicationId"         -> 133,
    "applicationLanguage"   -> "en",
    "refundingCountry"      -> "LV",
    "periodStartDate"       -> "2011-06-01T00:00:00",
    "periodEndDate"         -> "2011-10-31T23:59:59",
    "applicantEmailAddress" -> "test@hotmail.com",
    "updateSequenceNumber"  -> 30
  )

  "UpdateApplicationDetailsRequest JSON format" should {

    "serialize to JSON correctly" in {
      Json.toJson(fullRequest) shouldBe fullJson
    }

    "deserialize from JSON correctly" in {
      fullJson.as[UpdateApplicationDetailsRequest] shouldBe fullRequest
    }

    "deserialize when only mandatory fields are present" in {
      mandatoryOnlyJson.as[UpdateApplicationDetailsRequest] shouldBe fullRequest.copy(
        applicantPhoneNumber       = None,
        representativeCountry      = None,
        representativeEmailAddress = None,
        representativePhoneNumber  = None,
        bankAccountOwnerName       = None,
        bankAccountOwnerType       = None,
        ibanCode                   = None,
        bicCode                    = None,
        bankAccountCurrencyCode    = None,
        businessActivityCode2      = None,
        businessActivityCode3      = None,
        cipherText                 = None,
        encryptionStatus           = None
      )
    }

    Seq(
      "applicationId",
      "applicationLanguage",
      "refundingCountry",
      "periodStartDate",
      "periodEndDate",
      "applicantEmailAddress",
      "updateSequenceNumber"
    ).foreach { field =>
      s"fail to deserialize when $field is missing" in {
        (mandatoryOnlyJson - field).validate[UpdateApplicationDetailsRequest].isError shouldBe true
      }
    }
  }

  "UpdateApplicationDetailsRequest.withEncryptedDetails" should {

    val bankDetails = BankDetails("Joe Bloggs", "NWBKGB2LXXX", "GB29NWBK60161331926819", "GBP")
    val userAnswers = UserAnswers("id")
      .set(BankDetailsPage, bankDetails)
      .success
      .value
      .set(AccountHolderPage, AccountHolder.Applicant)
      .success
      .value

    val fixedTime = LocalDateTime.now(Clock.systemUTC())
    val applicationDetails = ApplicationDetails(
      applicationId              = 1L,
      applicationNumber          = Option("123"),
      refundingCountryCode       = Option("GB"),
      applicationVersion         = Some(fixedTime),
      periodStartDate            = Some(fixedTime),
      periodEndDate              = Some(fixedTime),
      applicantVatRegNumber      = Option("VAT reg 1234"),
      applicantEmailAddress      = Option("applicant@mail.com"),
      applicantTelephoneNumber   = Option("tel 12345678"),
      representativeId           = Option("Rep 123"),
      representativeIdType       = None,
      representativeCountry      = Option("FR"),
      representativeEmailAddress = Option("representative@mail.com"),
      representativeTelephoneNum = Option("tel 87654321"),
      applicationLanguage        = Some("EN"),
      bankAccountOwnerName       = None,
      bankAccountOwnerType       = None,
      ibanCode                   = None,
      bicCode                    = None,
      bankAccountCurrencyCode    = None,
      businessActivityCode1      = Some("10001"),
      businessActivityCode2      = Some("10002"),
      businessActivityCode3      = Some("10003"),
      messageId                  = Some("message 123"),
      correlationId              = Some("correlationId 123"),
      cipherText                 = None,
      encryptionStatus           = None,
      updateSeqNumber            = 0,
      applicationStatus          = Some("D"),
      submissionStatus           = Some("S")
    )

    val encryptedIban = "encryptedIban"
    val encryptedBIC = "encryptedBIC"
    val encryptedKnownText = "encryptedKnownText"

    val mockCryptoProvider = mock[BankDetailsEncryption]
    when(mockCryptoProvider.encrypt(bankDetails.accountNumber)).thenReturn(encryptedIban)
    when(mockCryptoProvider.encrypt(bankDetails.BIC)).thenReturn(encryptedBIC)
    when(mockCryptoProvider.knownCipherText).thenReturn(encryptedKnownText)

    "create encrypted UpdateApplicationDetailsRequest from userAnswers and applicationDetails" in {
      val result = UpdateApplicationDetailsRequest.withEncryptedDetails(userAnswers, applicationDetails, mockCryptoProvider)

      result shouldBe UpdateApplicationDetailsRequest(
        applicationId              = applicationDetails.applicationId,
        applicationLanguage        = applicationDetails.applicationLanguage.value,
        refundingCountry           = applicationDetails.refundingCountryCode.value,
        periodStartDate            = applicationDetails.periodStartDate.value,
        periodEndDate              = applicationDetails.periodEndDate.value,
        applicantEmailAddress      = applicationDetails.applicantEmailAddress.value,
        applicantPhoneNumber       = applicationDetails.applicantTelephoneNumber,
        representativeCountry      = applicationDetails.representativeCountry,
        representativeEmailAddress = applicationDetails.representativeEmailAddress,
        representativePhoneNumber  = applicationDetails.representativeTelephoneNum,
        bankAccountOwnerName       = Some(bankDetails.NameOnTheAccount),
        bankAccountOwnerType       = Some(AccountHolder.Applicant.toString),
        ibanCode                   = Some(encryptedIban),
        bicCode                    = Some(encryptedBIC),
        bankAccountCurrencyCode    = Some(bankDetails.CurrencyOfAccount),
        businessActivityCode2      = applicationDetails.businessActivityCode2,
        businessActivityCode3      = applicationDetails.businessActivityCode3,
        cipherText                 = Some(encryptedKnownText),
        encryptionStatus           = Some(EncryptionStatus.E.toString),
        updateSequenceNumber       = applicationDetails.updateSeqNumber
      )
    }

    "fail when refundingCountryCode is missing in ApplicationDetails" in {
      val updatedApplicationDetails = applicationDetails.copy(refundingCountryCode = None)

      val exception = the[Exception] thrownBy {
        UpdateApplicationDetailsRequest.withEncryptedDetails(userAnswers, updatedApplicationDetails, mockCryptoProvider)
      }

      exception should have message "Country code missing"
    }

    "fail when applicationLanguage is missing in ApplicationDetails" in {
      val updatedApplicationDetails = applicationDetails.copy(applicationLanguage = None)

      val exception = the[Exception] thrownBy {
        UpdateApplicationDetailsRequest.withEncryptedDetails(userAnswers, updatedApplicationDetails, mockCryptoProvider)
      }

      exception should have message "Language code missing"
    }

    "fail when periodStartDate is missing in ApplicationDetails" in {
      val updatedApplicationDetails = applicationDetails.copy(periodStartDate = None)

      val exception = the[Exception] thrownBy {
        UpdateApplicationDetailsRequest.withEncryptedDetails(userAnswers, updatedApplicationDetails, mockCryptoProvider)
      }

      exception should have message "Refund period start date missing"
    }

    "fail when periodEndDate is missing in ApplicationDetails" in {
      val updatedApplicationDetails = applicationDetails.copy(periodEndDate = None)

      val exception = the[Exception] thrownBy {
        UpdateApplicationDetailsRequest.withEncryptedDetails(userAnswers, updatedApplicationDetails, mockCryptoProvider)
      }

      exception should have message "Refund period end date missing"
    }

    "fail when applicantEmailAddress is missing in ApplicationDetails" in {
      val updatedApplicationDetails = applicationDetails.copy(applicantEmailAddress = None)

      val exception = the[Exception] thrownBy {
        UpdateApplicationDetailsRequest.withEncryptedDetails(userAnswers, updatedApplicationDetails, mockCryptoProvider)
      }

      exception should have message "Applicant email missing"
    }
  }
}
