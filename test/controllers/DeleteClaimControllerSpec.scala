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
import forms.DeleteClaimFormProvider
import models.responses.ApplicationResponse
import models.{RefundPeriod, UserAnswers}
import org.mockito.ArgumentCaptor
import utils.DeleteClaimHelper
import play.api.mvc.Results
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.claim.{RefundPeriodPage, RefundingCountryNamePage}
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.ClaimApplicationResponseQuery
import repositories.SessionRepository
import utils.DateTimeFormats.shortMonthYearFormat
import views.html.DeleteClaimView

import java.time.LocalDateTime
import scala.concurrent.Future

class DeleteClaimControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute = Call("GET", "/foo")

  val formProvider = new DeleteClaimFormProvider()
  val form = formProvider()

  lazy val deleteClaimRoute = routes.DeleteClaimController.onPageLoad().url

  val testRefundPeriod = RefundPeriod(
    startDate = LocalDateTime.of(2025, 4, 1, 0, 0),
    endDate   = LocalDateTime.of(2025, 8, 31, 23, 59, 59, 999000000)
  )

  val populatedAnswers = emptyUserAnswers
    .set(RefundingCountryNamePage, "Poland")
    .success
    .value
    .set(RefundPeriodPage, testRefundPeriod)
    .success
    .value
    .set(ClaimApplicationResponseQuery, ApplicationResponse(123, "APP123", 1))
    .success
    .value

  "DeleteClaim Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(populatedAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, deleteClaimRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[DeleteClaimView]
        implicit val msgs = messages(application)
        implicit val lang = msgs.lang

        val expectedMemberState = "Poland"
        val expectedStart = testRefundPeriod.startDate.format(shortMonthYearFormat())
        val expectedEnd = testRefundPeriod.endDate.format(shortMonthYearFormat())

        status(result) mustEqual OK
        contentAsString(result) must include(expectedMemberState)
        contentAsString(result) must include(expectedStart)
        contentAsString(result) must include(expectedEnd)
      }
    }

    "must redirect to the management frontend when 'Yes' is submitted" in {

      val mockDeleteHelper = mock[DeleteClaimHelper]

      when(mockDeleteHelper.deleteAndRedirect(any())(any())) thenReturn Future.successful(Results.Redirect("/manage"))

      val application =
        applicationBuilder(userAnswers = Some(populatedAnswers))
          .overrides(
            bind[utils.DeleteClaimHelper].toInstance(mockDeleteHelper)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, deleteClaimRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual "/manage"

        verify(mockDeleteHelper).deleteAndRedirect(any())(any())
      }
    }

    "must redirect to the task list dashboard when 'No' is submitted" in {

      val application = applicationBuilder(userAnswers = Some(populatedAnswers)).build()

      val mockSessionRepository = mock[SessionRepository]
      val mockEuVatRefundsService = mock[services.EuVatRefundsService]

      running(application) {
        val request =
          FakeRequest(POST, deleteClaimRoute)
            .withFormUrlEncodedBody(("value", "false"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.TaskListDashboardController.onPageLoad().url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(populatedAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, deleteClaimRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[DeleteClaimView]
        implicit val msgs = messages(application)
        implicit val lang = msgs.lang

        val expectedMemberState = "Poland"
        val expectedStart = testRefundPeriod.startDate.format(shortMonthYearFormat())
        val expectedEnd = testRefundPeriod.endDate.format(shortMonthYearFormat())

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) must include(expectedMemberState)
        contentAsString(result) must include(expectedStart)
        contentAsString(result) must include(expectedEnd)
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, deleteClaimRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, deleteClaimRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
