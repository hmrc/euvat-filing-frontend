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
import models.{Fuel, NormalMode, Other, RefundingLanguage, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.{times, verify}
import pages.{DescribeItemsOnImportDocPage, ImportSubCodePage, ImportTypePage, RefundingCountryPage, RefundingLanguagePage}
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository

class DescribeItemsOnImportDocControllerSpec extends SpecBase {

  private def onwardRoute: Call = Call("GET", "/foo")
  private def describeRoute = routes.DescribeItemsOnImportDocController.onPageLoad(NormalMode).url
  private def submitRoute = routes.DescribeItemsOnImportDocController.onSubmit(NormalMode).url
  private def journeyRecoveryUrl = controllers.routes.JourneyRecoveryController.onPageLoad().url

  private def answersWithoutSubCode: UserAnswers =
    emptyUserAnswers
      .set(RefundingCountryPage, "AT")
      .success
      .value
      .set(ImportTypePage, Other)
      .success
      .value
      .set(RefundingLanguagePage, RefundingLanguage.French)
      .success
      .value

  private def answersWithNoneSubCode: UserAnswers =
    emptyUserAnswers
      .set(RefundingCountryPage, "BE")
      .success
      .value
      .set(ImportTypePage, Other)
      .success
      .value
      .set(ImportSubCodePage, "__none__")
      .success
      .value
      .set(RefundingLanguagePage, RefundingLanguage.French)
      .success
      .value

  private def savedAnswers: UserAnswers = {
    val captor = ArgumentCaptor.forClass(classOf[UserAnswers])
    verify(mockSessionRepository, times(1)).set(captor.capture())
    captor.getValue
  }

  "DescribeItemsOnImportDoc Controller" - {

    "must return OK and render the question when the sub code page was skipped" in {
      val application = applicationBuilder(userAnswers = Some(answersWithoutSubCode)).build()

      running(application) {
        val result = route(application, FakeRequest(GET, describeRoute)).value

        status(result) mustEqual OK
        val content = contentAsString(result)
        content must include("Import details")
        content must include(messages(application)("describeItemsOnImportDoc.heading"))
      }
    }

    "must return OK when the user selected None of these on the sub code page" in {
      val application = applicationBuilder(userAnswers = Some(answersWithNoneSubCode)).build()

      running(application) {
        val result = route(application, FakeRequest(GET, describeRoute)).value

        status(result) mustEqual OK
      }
    }

    "must show the refunding language in the hint" in {
      val application = applicationBuilder(userAnswers = Some(answersWithoutSubCode)).build()

      running(application) {
        val result = route(application, FakeRequest(GET, describeRoute)).value

        val language = messages(application)("refundingLanguage.french")

        status(result) mustEqual OK
        contentAsString(result) must include(messages(application)("describeItemsOnImportDoc.hint", language))
      }
    }

    "must populate the view correctly when the question has previously been answered" in {
      val userAnswers = answersWithoutSubCode.set(DescribeItemsOnImportDocPage, "Office chairs").success.value
      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val result = route(application, FakeRequest(GET, describeRoute)).value

        status(result) mustEqual OK
        contentAsString(result) must include("Office chairs")
      }
    }

    "must redirect to Journey Recovery for a GET when the import type is not Other" in {
      val userAnswers = answersWithoutSubCode.set(ImportTypePage, Fuel).success.value
      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val result = route(application, FakeRequest(GET, describeRoute)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual journeyRecoveryUrl
      }
    }

    "must redirect to Journey Recovery for a GET when a real sub code was selected" in {
      val userAnswers = answersWithNoneSubCode.set(ImportSubCodePage, "10.17").success.value
      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val result = route(application, FakeRequest(GET, describeRoute)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual journeyRecoveryUrl
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {
      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val result = route(application, FakeRequest(GET, describeRoute)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual journeyRecoveryUrl
      }
    }

    "must save the description and redirect to the next page when valid data is submitted" in {
      val application = applicationBuilder(userAnswers = Some(answersWithoutSubCode))
        .overrides(
          bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
          bind[SessionRepository].toInstance(mockSessionRepository)
        )
        .build()

      running(application) {
        val request = FakeRequest(POST, submitRoute).withFormUrlEncodedBody(("value", "Office chairs"))
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
        savedAnswers.get(DescribeItemsOnImportDocPage) mustBe Some("Office chairs")
      }
    }

    "must return a Bad Request with the length error when more than 255 characters are submitted" in {
      val application = applicationBuilder(userAnswers = Some(answersWithoutSubCode)).build()

      running(application) {
        val request = FakeRequest(POST, submitRoute).withFormUrlEncodedBody(("value", "a" * 256))
        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        val content = contentAsString(result)
        content must include("There is a problem")
        content must include(messages(application)("describeItemsOnImportDoc.error.length"))
      }
    }

    "must return a Bad Request with the required error when nothing is submitted" in {
      val application = applicationBuilder(userAnswers = Some(answersWithoutSubCode)).build()

      running(application) {
        val request = FakeRequest(POST, submitRoute).withFormUrlEncodedBody()
        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) must include(messages(application)("describeItemsOnImportDoc.error.required"))
      }
    }

    "must redirect to Journey Recovery for a POST when the import type is not Other" in {
      val userAnswers = answersWithoutSubCode.set(ImportTypePage, Fuel).success.value
      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(POST, submitRoute).withFormUrlEncodedBody(("value", "Office chairs"))
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual journeyRecoveryUrl
      }
    }
  }
}
