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
import config.FrontendAppConfig
import models.UserAnswers
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import org.scalatestplus.mockito.MockitoSugar
import pages.{ClaimDetailsCompletedPage, QuestionPage}
import play.api.inject.bind
import play.api.libs.json.{JsPath, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository

import scala.concurrent.Future

class TaskListDashboardControllerSpec extends SpecBase with MockitoSugar {

  "TaskListDashboard Controller" - {

    case object DummyPage extends QuestionPage[String] {
      override def path: JsPath = JsPath \ "dummy"
    }

    "must return OK for a GET when no user answers exist" in {
      val mockSessionRepository = mock[SessionRepository]
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application = applicationBuilder(userAnswers = None)
        .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
        .build()

      running(application) {
        val request = FakeRequest(GET, routes.TaskListDashboardController.onPageLoad().url)
        val result = route(application, request).value

        status(result) mustEqual OK
      }
    }

    "must return OK for a GET when claim details are not yet completed" in {
      val mockSessionRepository = mock[SessionRepository]
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
        .build()

      running(application) {
        val request = FakeRequest(GET, routes.TaskListDashboardController.onPageLoad().url)
        val result = route(application, request).value

        status(result) mustEqual OK
      }
    }

    "must return OK for a GET when claim details are completed" in {
      val mockSessionRepository = mock[SessionRepository]
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val userAnswers = emptyUserAnswers.set(ClaimDetailsCompletedPage, true).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
        .build()

      running(application) {
        val request = FakeRequest(GET, routes.TaskListDashboardController.onPageLoad().url)
        val result = route(application, request).value

        status(result) mustEqual OK
      }
    }

    "must still return OK even if sessionRepository.set returns false" in {
      val mockSessionRepository = mock[SessionRepository]
      when(mockSessionRepository.set(any())) thenReturn Future.successful(false)

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
        .build()

      running(application) {
        val request = FakeRequest(GET, routes.TaskListDashboardController.onPageLoad().url)
        val result = route(application, request).value

        status(result) mustEqual OK
      }
    }

    "redirectToManageClaim must clear session data and redirect" in {
      val mockSessionRepository = mock[SessionRepository]
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val populatedAnswers = emptyUserAnswers.set(DummyPage, "value").success.value

      val application =
        applicationBuilder(userAnswers = Some(populatedAnswers))
          .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
          .build()

      running(application) {
        val request = FakeRequest(GET, routes.TaskListDashboardController.redirectToManageClaim().url)
        val result = route(application, request).value

        val config = application.injector.instanceOf[FrontendAppConfig]

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual config.claimDashboardUrl

        val captor = ArgumentCaptor.forClass(classOf[UserAnswers])
        verify(mockSessionRepository).set(captor.capture())
        captor.getValue.data mustBe Json.obj()
      }
    }

    "redirectToManageClaim must still redirect even if sessionRepository.set returns true" in {
      val mockSessionRepository = mock[SessionRepository]
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(userAnswers = None)
          .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
          .build()

      running(application) {
        val request = FakeRequest(GET, routes.TaskListDashboardController.redirectToManageClaim().url)
        val result = route(application, request).value

        val config = application.injector.instanceOf[FrontendAppConfig]

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual config.claimDashboardUrl
      }
    }

    "redirectToManageClaim must still redirect even if sessionRepository.set returns false" in {
      val mockSessionRepository = mock[SessionRepository]
      when(mockSessionRepository.set(any())) thenReturn Future.successful(false)

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
          .build()

      running(application) {
        val request = FakeRequest(GET, routes.TaskListDashboardController.redirectToManageClaim().url)
        val result = route(application, request).value

        val config = application.injector.instanceOf[FrontendAppConfig]

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual config.claimDashboardUrl
      }
    }

    "must show delete link when claim details are completed" in {
      val mockSessionRepository = mock[SessionRepository]
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val userAnswers = emptyUserAnswers.set(ClaimDetailsCompletedPage, true).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
        .build()

      running(application) {
        val request = FakeRequest(GET, routes.TaskListDashboardController.onPageLoad().url)
        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) must include(messages(application)("taskListDashboard.deleteLink"))
      }
    }

    "must not show delete link when claim details are not completed" in {
      val mockSessionRepository = mock[SessionRepository]
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
        .build()

      running(application) {
        val request = FakeRequest(GET, routes.TaskListDashboardController.onPageLoad().url)
        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) must not include messages(application)("taskListDashboard.deleteLink")
      }
    }
  }
}