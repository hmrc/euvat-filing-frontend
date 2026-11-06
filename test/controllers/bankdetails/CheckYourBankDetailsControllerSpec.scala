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

package controllers.bankdetails

import base.SpecBase
import models.ApplicationDetails
import models.requests.UpdateApplicationDetailsRequest
import models.responses.*
import models.{AccountHolder, BankDetails, CheckMode, EncryptionStatus}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{any, eq as mockitoEq}
import org.mockito.Mockito.when
import pages.bankdetails.{AccountHolderPage, BankDetailsPage}
import play.api.test.CSRFTokenHelper.*
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.{BankDetailsCompletedQuery, ClaimApplicationResponseQuery}
import uk.gov.hmrc.http.HeaderCarrier

import java.time.{Clock, LocalDateTime}
import scala.concurrent.{ExecutionContext, Future}

class CheckYourBankDetailsControllerSpec extends SpecBase {

  implicit val ec: ExecutionContext = ExecutionContext.global
  implicit val hc: HeaderCarrier = HeaderCarrier()

  "CheckYourBankDetails Controller" - {

    lazy val applicantAnswers = emptyUserAnswers
      .set(AccountHolderPage, AccountHolder.Applicant)
      .success
      .value

    val bankDetails = BankDetails("Joe Bloggs", "NWBKGB2LXXX", "GB29NWBK60161331926819", "GBP")
    lazy val fullAnswers = applicantAnswers
      .set(BankDetailsPage, bankDetails)
      .success
      .value

    "onPageLoad" - {

      "must return OK and render the heading, caption and card title" in {
        val application = applicationBuilder(userAnswers = Some(fullAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, routes.CheckYourBankDetailsController.onPageLoad().url).withCSRFToken
          val result = route(application, request).value
          val html = contentAsString(result)
          val msgs = messages(application)

          status(result) mustEqual OK
          html must include(msgs("bankDetails.checkYourBankDetails.heading"))
          html must include(msgs("bankDetails.caption"))
          html must include(msgs("bankDetails.checkYourBankDetails.card.title"))
          html must include(msgs("site.save.continue"))
        }
      }

      "must show the account holder row as Applicant" in {
        val application = applicationBuilder(userAnswers = Some(applicantAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, routes.CheckYourBankDetailsController.onPageLoad().url).withCSRFToken
          val result = route(application, request).value
          val html = contentAsString(result)
          val msgs = messages(application)

          html must include(msgs("bankDetails.checkYourBankDetails.accountHolder.label"))
          html must include(msgs("bankDetails.checkYourBankDetails.accountHolder.applicant"))
        }
      }

      "must show the account holder row as Third party" in {
        val ua = emptyUserAnswers.set(AccountHolderPage, AccountHolder.ThirdParty).success.value
        val application = applicationBuilder(userAnswers = Some(ua)).build()

        running(application) {
          val request = FakeRequest(GET, routes.CheckYourBankDetailsController.onPageLoad().url).withCSRFToken
          val result = route(application, request).value

          contentAsString(result) must include(messages(application)("bankDetails.checkYourBankDetails.accountHolder.thirdParty"))
        }
      }

      "must show account name, BIC (hidden), IBAN (hidden) and currency rows when BankDetailsCompletedQuery is not set" in {
        val answers = applicantAnswers
          .set(BankDetailsPage, BankDetails("Joe Bloggs", "NWBKGB2LXXX", "GB29NWBK60161331926819", "GBP"))
          .success
          .value

        val application = applicationBuilder(userAnswers = Some(answers)).build()

        running(application) {
          val request = FakeRequest(GET, routes.CheckYourBankDetailsController.onPageLoad().url).withCSRFToken
          val result = route(application, request).value
          val html = contentAsString(result)
          val msgs = messages(application)

          html must include(msgs("bankDetails.checkYourBankDetails.accountName.label"))
          html must include("Joe Bloggs")
          html must include(msgs("bankDetails.checkYourBankDetails.bic.label"))
          html must include("Hidden for security reasons")
          html must include(msgs("bankDetails.checkYourBankDetails.iban.label"))
          html must include("Hidden for security reasons")
          html must include(msgs("bankDetails.checkYourBankDetails.currency.label"))
          html must include("United Kingdom (pounds)")
        }
      }

      "must show account name, BIC (hidden), IBAN (hidden) and currency rows when BankDetailsCompletedQuery is set to false" in {
        val answers = applicantAnswers
          .set(BankDetailsPage, BankDetails("Joe Bloggs", "NWBKGB2LXXX", "GB29NWBK60161331926819", "GBP"))
          .success
          .value
          .set(BankDetailsCompletedQuery, false)
          .success
          .value

        val application = applicationBuilder(userAnswers = Some(answers)).build()

        running(application) {
          val request = FakeRequest(GET, routes.CheckYourBankDetailsController.onPageLoad().url).withCSRFToken
          val result = route(application, request).value
          val html = contentAsString(result)
          val msgs = messages(application)

          html must include(msgs("bankDetails.checkYourBankDetails.accountName.label"))
          html must include("Joe Bloggs")
          html must include(msgs("bankDetails.checkYourBankDetails.bic.label"))
          html must include("Hidden for security reasons")
          html must include(msgs("bankDetails.checkYourBankDetails.iban.label"))
          html must include("Hidden for security reasons")
          html must include(msgs("bankDetails.checkYourBankDetails.currency.label"))
          html must include("United Kingdom (pounds)")
        }
      }

      "must show account name, BIC (masked), IBAN (masked) and currency rows when BankDetailsCompletedQuery is set to true" in {
        val answers = applicantAnswers
          .set(BankDetailsPage, BankDetails("Joe Bloggs", "NWBKGB2LXXX", "GB29NWBK60161331926819", "GBP"))
          .success
          .value
          .set(BankDetailsCompletedQuery, true)
          .success
          .value

        val application = applicationBuilder(userAnswers = Some(answers)).build()

        running(application) {
          val request = FakeRequest(GET, routes.CheckYourBankDetailsController.onPageLoad().url).withCSRFToken
          val result = route(application, request).value
          val html = contentAsString(result)
          val msgs = messages(application)

          html must include(msgs("bankDetails.checkYourBankDetails.accountName.label"))
          html must include("Joe Bloggs")
          html must include(msgs("bankDetails.checkYourBankDetails.bic.label"))
          html must include("*******LXXX")
          html must include(msgs("bankDetails.checkYourBankDetails.iban.label"))
          html must include("******************6819")
          html must include(msgs("bankDetails.checkYourBankDetails.currency.label"))
          html must include("United Kingdom (pounds)")
        }
      }

      "must fall back to the currency code when it is not in config" in {
        val ua = applicantAnswers
          .set(BankDetailsPage, BankDetails("Joe Bloggs", "NWBKGB2LXXX", "GB29NWBK60161331926819", "XXX"))
          .success
          .value
        val application = applicationBuilder(userAnswers = Some(ua)).build()

        running(application) {
          val request = FakeRequest(GET, routes.CheckYourBankDetailsController.onPageLoad().url).withCSRFToken
          val result = route(application, request).value

          contentAsString(result) must include("XXX")
        }
      }

      "must not show any rows when no answers are saved" in {
        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, routes.CheckYourBankDetailsController.onPageLoad().url).withCSRFToken
          val result = route(application, request).value
          val html = contentAsString(result)
          val msgs = messages(application)

          status(result) mustEqual OK
          html must not include msgs("bankDetails.checkYourBankDetails.accountHolder.label")
          html must not include msgs("bankDetails.checkYourBankDetails.accountName.label")
          html must not include msgs("bankDetails.checkYourBankDetails.iban.label")
        }
      }

      "must link the card Change action to the account holder page in CheckMode" in {
        val application = applicationBuilder(userAnswers = Some(fullAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, routes.CheckYourBankDetailsController.onPageLoad().url).withCSRFToken
          val result = route(application, request).value

          contentAsString(result) must include(routes.AccountHolderController.onPageLoad(CheckMode).url)
        }
      }

      "must not render a back link" in {
        val application = applicationBuilder(userAnswers = Some(fullAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, routes.CheckYourBankDetailsController.onPageLoad().url).withCSRFToken
          val result = route(application, request).value

          contentAsString(result) must not include "govuk-back-link"
        }
      }

      "must redirect to Journey Recovery if no existing data is found" in {
        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val request = FakeRequest(GET, routes.CheckYourBankDetailsController.onPageLoad().url)
          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }

    "onSubmit" - {
      val applicationId = 1L
      val applicationNumber = "123"
      val updateSeqNumber = 0
      val fixedTime = LocalDateTime.now(Clock.systemUTC())
      val applicationDetails = ApplicationDetails(
        applicationId              = applicationId,
        applicationNumber          = Option(applicationNumber),
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
        updateSeqNumber            = updateSeqNumber,
        applicationStatus          = Some("D"),
        submissionStatus           = Some("S")
      )

      val applicationDetailsResponse = ApplicationDetailsResponse(applicationDetails = List(applicationDetails))

      "must update application and redirect to the task list when ClaimApplicationResponseQuery is in session" in {
        val accountHolder = AccountHolder.Applicant.toString
        val applicationResponse = ApplicationResponse(applicationId, applicationNumber, updateSeqNumber)
        val answers = fullAnswers.set(ClaimApplicationResponseQuery, applicationResponse).success.value
        val applicationDetailsUpdateResponse = UpdateApplicationDetailsResponse(applicationDetails.updateSeqNumber + 1)
        val updateRequestCaptor = ArgumentCaptor.forClass(classOf[UpdateApplicationDetailsRequest])

        when(mockEuVatRefundsService.getApplicationDetails(mockitoEq(applicationId))(any[HeaderCarrier]()))
          .thenReturn(Future.successful(applicationDetailsResponse))
        when(mockEuVatRefundsService.updateApplicationDetails(updateRequestCaptor.capture())(any[HeaderCarrier]()))
          .thenReturn(Future.successful(applicationDetailsUpdateResponse))

        val application = applicationBuilder(userAnswers = Some(answers)).build()

        running(application) {
          val request = FakeRequest(POST, routes.CheckYourBankDetailsController.onSubmit().url)
          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.TaskListDashboardController.onPageLoad().url

          val actualUpdateRequest = updateRequestCaptor.getValue
          actualUpdateRequest.applicationId mustEqual applicationDetails.applicationId
          actualUpdateRequest.bankAccountOwnerName.value mustEqual bankDetails.NameOnTheAccount
          actualUpdateRequest.ibanCode.value mustEqual bankDetails.accountNumber
          actualUpdateRequest.bicCode.value mustEqual bankDetails.BIC
          actualUpdateRequest.bankAccountCurrencyCode.value mustEqual bankDetails.CurrencyOfAccount
        }
      }

      "must redirect to Journey Recovery when service returns empty application details" in {
        val applicationResponse = ApplicationResponse(applicationId = applicationId, applicationNumber = "123", updateSeqNumber = 2)
        val emptyApplicationDetailsResponse = ApplicationDetailsResponse(applicationDetails = Nil)
        val answers = fullAnswers.set(ClaimApplicationResponseQuery, applicationResponse).success.value

        when(mockEuVatRefundsService.getApplicationDetails(any())(any[HeaderCarrier]()))
          .thenReturn(Future.successful(emptyApplicationDetailsResponse))

        val application = applicationBuilder(userAnswers = Some(answers)).build()

        running(application) {
          val request = FakeRequest(POST, routes.CheckYourBankDetailsController.onSubmit().url)
          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to Journey Recovery when service.updateApplicationDetails returns an error" in {
        val applicationResponse = ApplicationResponse(applicationId = applicationId, applicationNumber = "123", updateSeqNumber = 2)
        val answers = fullAnswers.set(ClaimApplicationResponseQuery, applicationResponse).success.value

        when(mockEuVatRefundsService.getApplicationDetails(any())(any[HeaderCarrier]()))
          .thenReturn(Future.successful(applicationDetailsResponse))
        when(mockEuVatRefundsService.updateApplicationDetails(any())(any[HeaderCarrier]()))
          .thenReturn(Future.failed(Exception("Update application error")))

        val application = applicationBuilder(userAnswers = Some(answers)).build()

        running(application) {
          val request = FakeRequest(POST, routes.CheckYourBankDetailsController.onSubmit().url)
          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to Journey Recovery when service.getApplicationDetails returns an error" in {
        val applicationResponse = ApplicationResponse(applicationId = applicationId, applicationNumber = "123", updateSeqNumber = 2)
        val answers = fullAnswers.set(ClaimApplicationResponseQuery, applicationResponse).success.value

        when(mockEuVatRefundsService.getApplicationDetails(any())(any[HeaderCarrier]()))
          .thenReturn(Future.failed(Exception("Get application error")))

        val application = applicationBuilder(userAnswers = Some(answers)).build()

        running(application) {
          val request = FakeRequest(POST, routes.CheckYourBankDetailsController.onSubmit().url)
          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to Journey Recovery when ClaimApplicationResponseQuery is NOT in session" in {
        val application = applicationBuilder(userAnswers = Some(fullAnswers)).build()

        running(application) {
          val request = FakeRequest(POST, routes.CheckYourBankDetailsController.onSubmit().url)
          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to Journey Recovery if no existing data is found" in {
        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val request = FakeRequest(POST, routes.CheckYourBankDetailsController.onSubmit().url)
          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }
  }
}
