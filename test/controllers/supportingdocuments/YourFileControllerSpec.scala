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
import models.UserAnswers
import models.YourFile
import org.jsoup.Jsoup
import forms.supportingdocuments.YourFileFormProvider.{descriptionFieldName, descriptionMaxLength, descriptionMaxLengthMessage}
import pages.supportingdocuments.YourFilePage
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository

class YourFileControllerSpec extends SpecBase {

  private val uploadAnswers = emptyUserAnswers
    .set(YourFilePage, YourFile("receipt1.jpg", Some("Supporting evidence")))
    .success
    .value

  private def applicationWithMockSessionRepository(answers: UserAnswers = uploadAnswers) =
    applicationBuilder(userAnswers = Some(answers))
      .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
      .build()

  "YourFileController" - {

    "always show the uploaded state and ignore the status query parameter" in {
      val application = applicationWithMockSessionRepository()

      running(application) {
        val request = FakeRequest(GET, s"${routes.YourFileController.onPageLoad().url}?status=AWAITING_UPLOAD")
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
        content must include("govuk-tag--green")
        content must include("Uploaded")
        content must include("File uploaded.")
        val description = Jsoup.parse(content).select("#fileupload-description")
        description.size mustBe 1
        description.text() mustBe "Supporting evidence"
        content must include(s"You have $descriptionMaxLength characters remaining")
        content must include(messages(application)("supportingDocuments.yourFile.description.label"))
        content must not include "govuk-tag--yellow"
        content must not include "disabled"
        content must not include "Refresh page to update file upload progress"
        val continueButton = Jsoup.parse(content).select(s"form[action='${routes.YourFileController.onSubmit().url}'] button")
        continueButton.text() mustBe messages(application)("site.continue")
        continueButton.attr("type") mustBe "submit"
        header("Refresh", result) mustBe None
      }
    }

    "navigate to journey recovery when Continue is submitted for a file" in {
      val application = applicationWithMockSessionRepository()

      running(application) {
        val request = FakeRequest(POST, routes.YourFileController.onSubmit().url)
          .withFormUrlEncodedBody(descriptionFieldName -> "A receipt for travel")
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustBe controllers.routes.JourneyRecoveryController.onPageLoad().url
        val savedAnswers = org.mockito.ArgumentCaptor.forClass(classOf[models.UserAnswers])
        org.mockito.Mockito.verify(mockSessionRepository).set(savedAnswers.capture())
        savedAnswers.getValue.get(YourFilePage) mustBe Some(YourFile("receipt1.jpg", Some("A receipt for travel")))
      }
    }

    "show the description validation error and retain the invalid value" in {
      val application = applicationWithMockSessionRepository()
      val invalidDescription = "x" * (descriptionMaxLength + 1)

      running(application) {
        val request = FakeRequest(POST, routes.YourFileController.onSubmit().url)
          .withFormUrlEncodedBody(descriptionFieldName -> invalidDescription)
        val result = route(application, request).value
        val content = contentAsString(result)

        status(result) mustEqual BAD_REQUEST
        content must include(
          messages(application)(descriptionMaxLengthMessage, descriptionMaxLength)
        )
        val document = Jsoup.parse(content)
        document.select(".govuk-error-summary__list a").attr("href") mustBe "#fileupload-description"
        document.select("#fileupload-description").text() mustBe invalidDescription
        document.select("#file-upload-1-hint").first().nextElementSibling().hasClass("govuk-error-summary") mustBe true
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
        savedAnswers.getValue.get(YourFilePage) mustBe None
      }
    }
  }
}
