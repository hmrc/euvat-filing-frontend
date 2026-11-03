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
import models.{AccountHolder, CheckMode}
import pages.bankdetails.AccountHolderPage
import play.api.test.CSRFTokenHelper.*
import play.api.test.FakeRequest
import play.api.test.Helpers.*

class CheckYourBankDetailsControllerSpec extends SpecBase {

  "CheckYourBankDetails Controller" - {

    lazy val applicantAnswers = emptyUserAnswers
      .set(AccountHolderPage, AccountHolder.Applicant)
      .success
      .value

    "onPageLoad" - {

      "must return OK and render the heading, caption and card title" in {
        val application = applicationBuilder(userAnswers = Some(applicantAnswers)).build()

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

      "must not show the account holder row when no answer is saved" in {
        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, routes.CheckYourBankDetailsController.onPageLoad().url).withCSRFToken
          val result = route(application, request).value

          status(result) mustEqual OK
          contentAsString(result) must not include messages(application)("checkYourBankDetails.accountHolder.label")
        }
      }

      "must link the card Change action to the account holder page in CheckMode" in {
        val application = applicationBuilder(userAnswers = Some(applicantAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, routes.CheckYourBankDetailsController.onPageLoad().url).withCSRFToken
          val result = route(application, request).value

          contentAsString(result) must include(routes.AccountHolderController.onPageLoad(CheckMode).url)
        }
      }

      "must not render a back link" in {
        val application = applicationBuilder(userAnswers = Some(applicantAnswers)).build()

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
        val application = applicationBuilder(userAnswers = Some(applicantAnswers)).build()

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
