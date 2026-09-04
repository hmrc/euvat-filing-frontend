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

package controllers.imports

import base.SpecBase
import models.{ImportType, UserAnswers}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.{ImportSubCodePage, ImportTypePage, RefundingCountryPage}
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository

import scala.concurrent.Future

class ImportSubCodeControllerSpec extends SpecBase with MockitoSugar {

  private def fuelRoute = controllers.imports.routes.ImportSubCodeController.onPageLoad("fuel").url
  private def journeyRecoveryUrl = controllers.routes.JourneyRecoveryController.onPageLoad().url
  private def taskListUrl = controllers.routes.TaskListDashboardController.onPageLoad().url

  private def answers(importType: ImportType = ImportType.Fuel, country: String = "AT"): UserAnswers =
    emptyUserAnswers
      .set(RefundingCountryPage, country)
      .success
      .value
      .set(ImportTypePage, importType)
      .success
      .value

  "ImportSubCode Controller" - {

    "must return OK with the fuel question and the member state's sub-code options for a GET" in {
      val application = applicationBuilder(userAnswers = Some(answers())).build()

      running(application) {
        val result = route(application, FakeRequest(GET, fuelRoute)).value

        status(result) mustEqual OK
        val content = contentAsString(result)
        content must include("What is the fuel used for?")
        content must include("Import details")
        content must include("value=\"1.3\"")
        content must include("value=\"__none__\"")
        """>\s*None\s*<""".r.findFirstIn(content) mustBe defined
        content must not include "value=\"1.2.6\""
      }
    }

    "must render the transport question when the transport category was selected" in {
      val application = applicationBuilder(userAnswers = Some(answers(ImportType.Transport))).build()

      running(application) {
        val transportRoute = controllers.imports.routes.ImportSubCodeController.onPageLoad("transport").url
        val result = route(application, FakeRequest(GET, transportRoute)).value

        status(result) mustEqual OK
        contentAsString(result) must include("What is the type of transport cost?")
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {
      val userAnswers = answers().set(ImportSubCodePage, "1.3").success.value
      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val result = route(application, FakeRequest(GET, fuelRoute)).value

        status(result) mustEqual OK
        """value="1\.3"[^>]*\bchecked""".r.findFirstIn(contentAsString(result)) mustBe defined
      }
    }

    "must redirect to Journey Recovery for a GET when the member state only offers the 10.99 sub-code" in {
      val application = applicationBuilder(userAnswers = Some(answers(ImportType.Other))).build()

      running(application) {
        val otherRoute = controllers.imports.routes.ImportSubCodeController.onPageLoad("other").url
        val result = route(application, FakeRequest(GET, otherRoute)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual journeyRecoveryUrl
      }
    }

    "must redirect to Journey Recovery for a GET when the URL category does not match the import type answer" in {
      val application = applicationBuilder(userAnswers = Some(answers(ImportType.Transport))).build()

      running(application) {
        val result = route(application, FakeRequest(GET, fuelRoute)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual journeyRecoveryUrl
      }
    }

    "must redirect to Journey Recovery for a GET when no import type has been answered" in {
      val userAnswers = emptyUserAnswers.set(RefundingCountryPage, "AT").success.value
      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val result = route(application, FakeRequest(GET, fuelRoute)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual journeyRecoveryUrl
      }
    }

    "must redirect to Journey Recovery for a GET when no member state has been answered" in {
      val userAnswers = emptyUserAnswers.set(ImportTypePage, ImportType.Fuel).success.value
      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val result = route(application, FakeRequest(GET, fuelRoute)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual journeyRecoveryUrl
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {
      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val result = route(application, FakeRequest(GET, fuelRoute)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual journeyRecoveryUrl
      }
    }

    "must save the selected sub-code and redirect when valid data is submitted" in {
      val mockSessionRepository = mock[SessionRepository]
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application = applicationBuilder(userAnswers = Some(answers()))
        .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
        .build()

      running(application) {
        val request = FakeRequest(POST, fuelRoute).withFormUrlEncodedBody("value" -> "1.3")
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual taskListUrl
      }
    }

    "must save the none marker and redirect when the last option is submitted" in {
      val mockSessionRepository = mock[SessionRepository]
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application = applicationBuilder(userAnswers = Some(answers()))
        .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
        .build()

      running(application) {
        val request = FakeRequest(POST, fuelRoute).withFormUrlEncodedBody("value" -> "__none__")
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual taskListUrl
      }
    }

    "must return a Bad Request with the category's error message when no option is submitted" in {
      val application = applicationBuilder(userAnswers = Some(answers())).build()

      running(application) {
        val request = FakeRequest(POST, fuelRoute).withFormUrlEncodedBody()
        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) must include("Select what the fuel is used for")
      }
    }

    "must redirect to Journey Recovery when a sub-code outside the member state's options is submitted" in {
      val application = applicationBuilder(userAnswers = Some(answers())).build()

      running(application) {
        val request = FakeRequest(POST, fuelRoute).withFormUrlEncodedBody("value" -> "9.9")
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual journeyRecoveryUrl
      }
    }
  }
}
