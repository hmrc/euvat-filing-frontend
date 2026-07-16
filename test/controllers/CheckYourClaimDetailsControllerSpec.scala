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

package controllers

import base.SpecBase
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import org.scalatestplus.mockito.MockitoSugar
import pages.ClaimDetailsCompletedPage
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import viewmodels.govuk.SummaryListFluency
import views.html.CheckYourClaimDetailsView
import models.UserAnswers

import scala.concurrent.Future

class CheckYourClaimDetailsControllerSpec extends SpecBase with SummaryListFluency with MockitoSugar {

  "Check Your Answers Controller" - {

    "must return OK and the correct view for a GET" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
      running(application) {
        val request = FakeRequest(GET, routes.CheckYourClaimDetailsController.onPageLoad().url)
        val result = route(application, request).value

        status(result) mustEqual OK
      }
    }

    "must redirect to the task list on submit and set ClaimDetailsCompletedPage to true" in {
      val mockSessionRepository = mock[SessionRepository]
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
        .build()

      running(application) {
        val request = FakeRequest(POST, routes.CheckYourClaimDetailsController.onSubmit().url)
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.TaskListDashboardController.onPageLoad().url

        val captor = ArgumentCaptor.forClass(classOf[UserAnswers])
        verify(mockSessionRepository).set(captor.capture())
        captor.getValue.get(ClaimDetailsCompletedPage) mustBe Some(true)
      }
    }

    "must still redirect to task list on submit even if sessionRepository.set returns false" in {
      val mockSessionRepository = mock[SessionRepository]
      when(mockSessionRepository.set(any())) thenReturn Future.successful(false)

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
        .build()

      running(application) {
        val request = FakeRequest(POST, routes.CheckYourClaimDetailsController.onSubmit().url)
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.TaskListDashboardController.onPageLoad().url
      }
    }

    "must NOT include language label when country has only one language" in {
      val ua = emptyUserAnswers
        .set(pages.RefundingCountryPage, "CZ")
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(ua)).build()

      running(application) {
        val request = FakeRequest(GET, routes.CheckYourClaimDetailsController.onPageLoad().url)
        val result = route(application, request).value
        val html = contentAsString(result)

        html must not include (messages(application)("checkYourClaimDetails.refundingLanguage.label"))
      }
    }

    "must include change links for each section" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, routes.CheckYourClaimDetailsController.onPageLoad().url)
        val result = route(application, request).value
        val html = contentAsString(result)

        html must include("change")
      }
    }

    "must not display phone number when it's missing" in {
      val contact = models.ContactDetails("a@b.com", None)
      val ua = emptyUserAnswers.set(pages.ContactDetailsPage, contact).success.value
      val application = applicationBuilder(userAnswers = Some(ua)).build()

      running(application) {
        val request = FakeRequest(GET, routes.CheckYourClaimDetailsController.onPageLoad().url)
        val result = route(application, request).value
        val html = contentAsString(result)

        html must include(contact.email)
        html must include(messages(application)("checkYourClaimDetails.contactPhone.subLabel"))
        html must include(messages(application)("Not provided"))
      }
    }

    "must include language row with change link when country has multiple languages" in {
      val ua = emptyUserAnswers
        .set(pages.RefundingCountryPage, "BE")
        .success
        .value
        .set(pages.RefundingLanguagePage, models.RefundingLanguage.English)
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(ua)).build()

      running(application) {
        val request = FakeRequest(GET, routes.CheckYourClaimDetailsController.onPageLoad().url)
        val result = route(application, request).value
        val html = contentAsString(result)

        html must include(messages(application)("checkYourClaimDetails.refundingLanguage.label"))
        html must include(routes.RefundingLanguageController.onPageLoad(models.CheckMode).url)
      }
    }

    "must NOT include language section when country has only one language" in {
      val ua = emptyUserAnswers
        .set(pages.RefundingCountryPage, "CZ")
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(ua)).build()

      running(application) {
        val request = FakeRequest(GET, routes.CheckYourClaimDetailsController.onPageLoad().url)
        val result = route(application, request).value
        val html = contentAsString(result)

        html must not include messages(application)("checkYourClaimDetails.refundingLanguage.label")
      }
    }

    "must include currency section when country has multiple currencies" in {
      val ua = emptyUserAnswers
        .set(pages.RefundingCountryPage, "BG")
        .success
        .value
        .set(pages.RefundingCurrencyPage, "BGN")
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(ua)).build()

      running(application) {
        val request = FakeRequest(GET, routes.CheckYourClaimDetailsController.onPageLoad().url)
        val result = route(application, request).value
        val html = contentAsString(result)

        html must include(messages(application)("checkYourClaimDetails.refundingCurrency.label"))
        html must include(routes.RefundingCurrencyController.onPageLoad(models.CheckMode).url)
      }
    }

    "must NOT include currency section when country has only one currency" in {
      val ua = emptyUserAnswers
        .set(pages.RefundingCountryPage, "AT")
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(ua)).build()

      running(application) {
        val request = FakeRequest(GET, routes.CheckYourClaimDetailsController.onPageLoad().url)
        val result = route(application, request).value
        val html = contentAsString(result)

        html must not include messages(application)("checkYourClaimDetails.refundingCurrency.label")
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {
      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, routes.CheckYourClaimDetailsController.onPageLoad().url)
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
