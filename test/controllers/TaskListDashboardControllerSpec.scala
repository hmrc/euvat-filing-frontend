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
import org.mockito.Mockito.{never, reset, times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import viewmodels.taskList.ClaimDetailsStatus
import views.html.TaskListDashboardView

import scala.concurrent.Future

class TaskListDashboardControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private val mockSessionRepository: SessionRepository = mock[SessionRepository]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockSessionRepository)
    when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))
  }

  private def application(userAnswers: Option[UserAnswers]) =
    applicationBuilder(userAnswers = userAnswers)
      .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
      .build()

  "TaskListDashboardController.onPageLoad" - {

    "when no UserAnswers row exists for the user" - {

      "must seed an empty UserAnswers row, persist it via SessionRepository.set, and render with NotStarted status" in {
        val app = application(userAnswers = None)

        running(app) {
          val request = FakeRequest(GET, routes.TaskListDashboardController.onPageLoad().url)
          val result  = route(app, request).value
          val view    = app.injector.instanceOf[TaskListDashboardView]
          val config  = app.injector.instanceOf[FrontendAppConfig]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(config.claimDashboardUrl, ClaimDetailsStatus.NotStarted)(
            request,
            messages(app)
          ).toString

          val captor = ArgumentCaptor.forClass(classOf[UserAnswers])
          verify(mockSessionRepository, times(1)).set(captor.capture())
          captor.getValue.id mustEqual userAnswersId
        }
      }
    }

    "when an empty UserAnswers row exists for the user" - {

      "must reuse the existing row, NOT call SessionRepository.set, and render with NotStarted status" in {
        val app = application(userAnswers = Some(emptyUserAnswers))

        running(app) {
          val request = FakeRequest(GET, routes.TaskListDashboardController.onPageLoad().url)
          val result  = route(app, request).value
          val view    = app.injector.instanceOf[TaskListDashboardView]
          val config  = app.injector.instanceOf[FrontendAppConfig]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(config.claimDashboardUrl, ClaimDetailsStatus.NotStarted)(
            request,
            messages(app)
          ).toString

          verify(mockSessionRepository, never).set(any())
        }
      }
    }

    "when refreshed (returning visit) after a fresh seed" - {

      "must seed exactly once on first visit, then reuse the existing row on refresh — no second set call" in {
        // First visit: no UserAnswers row → controller seeds.
        val app1 = application(userAnswers = None)
        running(app1) {
          val request = FakeRequest(GET, routes.TaskListDashboardController.onPageLoad().url)
          status(route(app1, request).value) mustEqual OK
          verify(mockSessionRepository, times(1)).set(any())
        }

        // Reset the mock to isolate verification of the second call.
        reset(mockSessionRepository)
        when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))

        // Refresh: row now exists → controller must NOT call set again.
        val app2 = application(userAnswers = Some(emptyUserAnswers))
        running(app2) {
          val request = FakeRequest(GET, routes.TaskListDashboardController.onPageLoad().url)
          status(route(app2, request).value) mustEqual OK
          verify(mockSessionRepository, never).set(any())
        }
      }
    }

    "rendering — locked rows" - {

      "must show the five other task-list rows as Cannot start yet regardless of UserAnswers contents" in {
        val app = application(userAnswers = Some(emptyUserAnswers))

        running(app) {
          val request = FakeRequest(GET, routes.TaskListDashboardController.onPageLoad().url)
          val result  = route(app, request).value

          val body = contentAsString(result)
          // five locked rows: Add a purchase, Add an import, Add supporting documents, Add bank details, Submit claim
          body.split("Cannot start yet").length - 1 mustEqual 5
        }
      }
    }
  }
}
