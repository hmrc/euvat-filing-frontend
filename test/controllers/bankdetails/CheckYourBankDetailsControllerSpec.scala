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
import controllers.bankdetails.routes
import models.{AccountHolder, BankDetails, CheckMode}
import pages.bankdetails.{AccountHolderPage, BankDetailsPage}
import play.api.test.CSRFTokenHelper.*
import play.api.test.FakeRequest
import play.api.test.Helpers.*

class CheckYourBankDetailsControllerSpec extends SpecBase {

  "CheckYourBankDetails Controller" - {

    lazy val applicantAnswers = emptyUserAnswers
      .set(AccountHolderPage, AccountHolder.Applicant)
      .success
      .value

    lazy val fullAnswers = applicantAnswers
      .set(BankDetailsPage, BankDetails("Joe Bloggs", "NWBKGB2LXXX", "GB29NWBK60161331926819", "GBP"))
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
          html must include(msgs("checkYourBankDetails.heading"))
          html must include(msgs("checkYourBankDetails.caption"))
          html must include(msgs("checkYourBankDetails.card.title"))
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

          html must include(msgs("checkYourBankDetails.accountHolder.label"))
          html must include(msgs("checkYourBankDetails.accountHolder.applicant"))
        }
      }

      "must show the account holder row as Third party" in {
        val ua = emptyUserAnswers.set(AccountHolderPage, AccountHolder.ThirdParty).success.value
        val application = applicationBuilder(userAnswers = Some(ua)).build()

        running(application) {
          val request = FakeRequest(GET, routes.CheckYourBankDetailsController.onPageLoad().url).withCSRFToken
          val result = route(application, request).value

          contentAsString(result) must include(messages(application)("checkYourBankDetails.accountHolder.thirdParty"))
        }
      }

      "must show account name, BIC, IBAN and currency rows" in {
        val baseApp = applicationBuilder(userAnswers = None).build()
        val currency = baseApp.injector.instanceOf[config.InternationalCurrencyProvider].currencies.head

        val answers = applicantAnswers
          .set(BankDetailsPage, BankDetails("Joe Bloggs", "NWBKGB2LXXX", "GB29NWBK60161331926819", currency.code))
          .success
          .value

        val application = applicationBuilder(userAnswers = Some(answers)).build()

        running(application) {
          val request = FakeRequest(GET, routes.CheckYourBankDetailsController.onPageLoad().url).withCSRFToken
          val result = route(application, request).value
          val html = contentAsString(result)
          val msgs = messages(application)

          html must include(msgs("checkYourBankDetails.accountName.label"))
          html must include("Joe Bloggs")
          html must include(msgs("checkYourBankDetails.bic.label"))
          html must include("NWBKGB2LXXX")
          html must include(msgs("checkYourBankDetails.iban.label"))
          html must include("GB29NWBK60161331926819")
          html must include(msgs("checkYourBankDetails.currency.label"))
          html must include(play.twirl.api.HtmlFormat.escape(currency.description).toString)
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
          html must not include msgs("checkYourBankDetails.accountHolder.label")
          html must not include msgs("checkYourBankDetails.accountName.label")
          html must not include msgs("checkYourBankDetails.iban.label")
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

      "must redirect to the task list" in {
        val application = applicationBuilder(userAnswers = Some(fullAnswers)).build()

        running(application) {
          val request = FakeRequest(POST, routes.CheckYourBankDetailsController.onSubmit().url)
          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.TaskListDashboardController.onPageLoad().url
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