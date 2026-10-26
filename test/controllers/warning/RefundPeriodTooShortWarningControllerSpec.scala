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

package controllers.warning

import base.SpecBase
import controllers.claim.routes.{CheckYourClaimDetailsController, ContactDetailsController, RefundPeriodController}
import controllers.routes.JourneyRecoveryController
import models.{CheckMode, NormalMode, RefundPeriod}
import pages.claim.RefundPeriodPage
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import views.html.warning.RefundPeriodTooShortWarningView
import java.time.LocalDateTime

class RefundPeriodTooShortWarningControllerSpec extends SpecBase {

  "RefundPeriodTooShortWarning Controller" - {

    "must return OK and the correct view for a GET in NormalMode" in {
      val refundPeriod = RefundPeriod(
        startDate = LocalDateTime.of(2024, 3, 1, 0, 0),
        endDate   = LocalDateTime.of(2024, 4, 30, 23, 59)
      )
      val userAnswers = emptyUserAnswers.set(RefundPeriodPage, refundPeriod).success.value
      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, routes.RefundPeriodTooShortWarningController.onPageLoad(NormalMode).url)
        val result = route(application, request).value
        val view = application.injector.instanceOf[RefundPeriodTooShortWarningView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          RefundPeriodController.onPageLoad(NormalMode),
          NormalMode
        )(request, messages(application)).toString
      }
    }

    "must return OK and the correct view for a GET in CheckMode" in {
      val refundPeriod = RefundPeriod(
        startDate = LocalDateTime.of(2024, 3, 1, 0, 0),
        endDate   = LocalDateTime.of(2024, 4, 30, 23, 59)
      )
      val userAnswers = emptyUserAnswers.set(RefundPeriodPage, refundPeriod).success.value
      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, routes.RefundPeriodTooShortWarningController.onPageLoad(CheckMode).url)
        val result = route(application, request).value
        val view = application.injector.instanceOf[RefundPeriodTooShortWarningView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          RefundPeriodController.onPageLoad(CheckMode),
          CheckMode
        )(request, messages(application)).toString
      }
    }

    "must redirect to a Journey Recovery for a GET if no existing data is found " in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, routes.RefundPeriodTooShortWarningController.onPageLoad(NormalMode).url)
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to ContactDetailsController on submit in NormalMode " in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(POST, routes.RefundPeriodTooShortWarningController.onSubmit(NormalMode).url)
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual ContactDetailsController.onPageLoad(NormalMode).url
      }
    }

    "must redirect to CheckYourClaimDetailsController on submit in CheckMode " in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(POST, routes.RefundPeriodTooShortWarningController.onSubmit(CheckMode).url)
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual CheckYourClaimDetailsController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(POST, routes.RefundPeriodTooShortWarningController.onSubmit(NormalMode).url)
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
