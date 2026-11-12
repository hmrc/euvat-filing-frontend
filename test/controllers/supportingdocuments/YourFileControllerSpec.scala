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

package controllers.supportingdocuments

import base.SpecBase
import org.jsoup.Jsoup
import models.UserAnswers
import pages.supportingdocuments.YourFileNamePage
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.UploadedFileStatusQuery
import repositories.SessionRepository

class YourFileControllerSpec extends SpecBase {

  private val uploadAnswers = emptyUserAnswers
    .set(YourFileNamePage, "receipt1.jpg")
    .success
    .value
    .set(UploadedFileStatusQuery, YourFileController.AwaitingUpload)
    .success
    .value

  private val validatedUploadAnswers = uploadAnswers
    .set(UploadedFileStatusQuery, YourFileController.Validated)
    .success
    .value

  private def applicationWithMockSessionRepository(answers: UserAnswers = uploadAnswers) =
    applicationBuilder(userAnswers = Some(answers))
      .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
      .build()

  "YourFileController" - {

    "show the uploading state, disable continue and refresh while upload is pending" in {
      val application = applicationWithMockSessionRepository()

      running(application) {
        val request = FakeRequest(GET, routes.YourFileController.onPageLoad().url)
        val result = route(application, request).value
        val content = contentAsString(result)

        status(result) mustEqual OK
        content must include("<title>Upload file - EU VAT - GOV.UK</title>")
        content must include("<h1 class=\"govuk-heading-l govuk-!-margin-bottom-3\">")
        Jsoup
          .parse(content)
          .select("#main-content > .govuk-grid-row > .govuk-grid-column-two-thirds > h1")
          .size mustBe 1
        content must include("receipt1.jpg")
        content must include("govuk-tag--yellow")
        content must include("Uploading")
        content must include("disabled")
        header("Refresh", result) mustBe Some("5")
      }
    }

    "show the uploaded state and an enabled continue link for VALIDATED" in {
      val application = applicationWithMockSessionRepository(validatedUploadAnswers)

      running(application) {
        val request = FakeRequest(GET, routes.YourFileController.onPageLoad().url)
        val result = route(application, request).value
        val content = contentAsString(result)

        status(result) mustEqual OK
        content must include("govuk-tag--green")
        content must include("Uploaded")
        val continueButton = Jsoup.parse(content).select(s"form[action='${routes.YourFileController.onSubmit().url}'] button")
        continueButton.text() mustBe messages(application)("site.continue")
        continueButton.attr("type") mustBe "submit"
        content must not include "disabled"
        header("Refresh", result) mustBe None
      }
    }

    "navigate to journey recovery when Continue is submitted for a validated file" in {
      val application = applicationWithMockSessionRepository(validatedUploadAnswers)

      running(application) {
        val request = FakeRequest(POST, routes.YourFileController.onSubmit().url)
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustBe controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "keep the user on the page when Continue is submitted before the file is validated" in {
      val application = applicationWithMockSessionRepository()

      running(application) {
        val request = FakeRequest(POST, routes.YourFileController.onSubmit().url)
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustBe routes.YourFileController.onPageLoad().url
      }
    }

    "return verification failures to the upload form with its matching error" in {
      val application = applicationWithMockSessionRepository()

      running(application) {
        val request = FakeRequest(GET, s"${routes.YourFileController.onPageLoad().url}?status=VERIFICATION_FAILED")
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UploadFileController.onPageLoad().url
        flash(result).get("uploadError") mustBe Some("supportingDocuments.uploadFile.error.verificationFailed")
      }
    }

    "remove the UI upload record and return to the upload form" in {
      val application = applicationWithMockSessionRepository()

      running(application) {
        val request = FakeRequest(POST, routes.YourFileController.onRemove().url)
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UploadFileController.onPageLoad().url
        val savedAnswers = org.mockito.ArgumentCaptor.forClass(classOf[models.UserAnswers])
        org.mockito.Mockito.verify(mockSessionRepository).set(savedAnswers.capture())
        savedAnswers.getValue.get(YourFileNamePage) mustBe None
        savedAnswers.getValue.get(UploadedFileStatusQuery) mustBe None
      }
    }
  }
}
