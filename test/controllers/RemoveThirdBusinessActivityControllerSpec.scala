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
import forms.RemoveBusinessActivityFormProvider
import models.NormalMode
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.{BusinessActivityCodeThreePage, BusinessActivityCodeTwoPage, BusinessActivityTwoPage}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import play.api.inject.bind

import scala.concurrent.Future

class RemoveThirdBusinessActivityControllerSpec extends SpecBase with MockitoSugar {

  val formProvider = new RemoveBusinessActivityFormProvider()
  val form = formProvider()

  lazy val removeRoute = routes.RemoveThirdBusinessActivityController.onPageLoad().url + "?origin=business-activity-3"

  "RemoveThirdBusinessActivity Controller" - {

    "must return OK for a GET" in {
      val userAnswers = emptyUserAnswers.set(BusinessActivityCodeThreePage, "39200").success.value
      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, removeRoute)
        val result = route(application, request).value

        status(result) mustEqual OK
      }
    }

    "must remove 3rd activity and redirect to business-activity-2 when yes selected" in {
      val userAnswers = emptyUserAnswers.set(BusinessActivityCodeThreePage, "39200").success.value

      val mockSessionRepository = mock[SessionRepository]
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application = applicationBuilder(userAnswers = Some(userAnswers)).overrides(bind[SessionRepository].toInstance(mockSessionRepository)).build()

      running(application) {
        val request = FakeRequest(POST, removeRoute).withFormUrlEncodedBody(("value", "true"))
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.BusinessActivityTwoController.onPageLoad(NormalMode).url
      }
    }

    "must redirect back to business-activity-3 when no is selected" in {
      val userAnswers = emptyUserAnswers.set(BusinessActivityCodeThreePage, "39200").success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(POST, removeRoute).withFormUrlEncodedBody(("value", "false"))
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.BusinessActivityThreeController.onPageLoad().url
      }
    }
  }
}
