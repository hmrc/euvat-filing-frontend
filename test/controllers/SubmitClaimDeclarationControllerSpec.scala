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
import models.RefundPeriod
import org.jsoup.Jsoup
import models.responses.{ApplicationCountsResponse, ApplicationResponse}
import org.mockito.ArgumentMatchers.{any, eq as mockitoEq}
import org.mockito.Mockito.{verify, when}
import pages.claim.{RefundPeriodPage, RefundingCountryNamePage}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.ClaimApplicationResponseQuery
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDateTime
import scala.concurrent.Future

class SubmitClaimDeclarationControllerSpec extends SpecBase {

  private val applicationId = 111L

  private def userAnswers = emptyUserAnswers
    .set(ClaimApplicationResponseQuery, ApplicationResponse(applicationId, "App1", 1))
    .success
    .value
    .set(RefundingCountryNamePage, "Austria")
    .success
    .value
    .set(
      RefundPeriodPage,
      RefundPeriod(
        LocalDateTime.of(2025, 9, 1, 0, 0),
        LocalDateTime.of(2025, 12, 31, 0, 0)
      )
    )
    .success
    .value

  "SubmitClaimDeclarationController" - {
    "loads claim details, uses backend counts and renders the declaration view" in {
      when(mockEuVatRefundsService.getApplicationCounts(mockitoEq(applicationId))(any[HeaderCarrier]()))
        .thenReturn(Future.successful(ApplicationCountsResponse(purchaseCount = 1, importCount = 0, attachmentCount = 0)))

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val result = route(application, FakeRequest(GET, routes.SubmitClaimDeclarationController.onPageLoad.url)).value
        val body = contentAsString(result)
        val document = Jsoup.parse(body)

        status(result) mustBe OK
        body must include("Austria")
        body must include("September 2025")
        body must include("December 2025")
        body must include("1 item added to claim")
        body must include("No files uploaded to claim")
        body must include("By submitting this application, I declare that:")
        body must include(
          "during the period covered by this application, the applicant business made no supply of goods or services in the Member State of Refund (other than goods and services in respect of which tax is payable solely by the person to whom they are supplied, and certain supporting exempted transport services)"
        )
        body must include(
          "the details given in this application are true, and I acknowledge that monies wrongly claimed shall be recovered directly together with any penalty and interest"
        )
        body must include(
          "I understand and accept that further communications about this application will be conducted by email to the email address I supplied."
        )
        body must include("Make declaration and submit your claim - EU VAT - GOV.UK")
        document.select(".govuk-summary-list__key.govuk-\\!-width-one-half").size() mustBe 5
        document.select(".govuk-summary-list.govuk-\\!-width-one-half").size() mustBe 0
        document.select(".govuk-summary-list__actions").size() mustBe 0
        document.select(".govuk-back-link").size() mustBe 1
        document.select(s"form[action='${routes.SubmitClaimDeclarationController.onSubmit.url}'][autocomplete='off']").size() mustBe 1
        document.select(".govuk-button[disabled]").size() mustBe 0
        document.select(".govuk-button").text() mustBe "Accept and submit"
        verify(mockEuVatRefundsService).getApplicationCounts(mockitoEq(applicationId))(any[HeaderCarrier]())
      }
    }

    "renders plural item and file counts" in {
      when(mockEuVatRefundsService.getApplicationCounts(mockitoEq(applicationId))(any[HeaderCarrier]()))
        .thenReturn(Future.successful(ApplicationCountsResponse(purchaseCount = 3, importCount = 2, attachmentCount = 4)))

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val result = route(application, FakeRequest(GET, routes.SubmitClaimDeclarationController.onPageLoad.url)).value
        val body = contentAsString(result)

        status(result) mustBe OK
        body must include("5 items added to claim")
        body must include("4 files uploaded to claim")
      }
    }

    "redirects to journey recovery when the counts request fails" in {
      when(mockEuVatRefundsService.getApplicationCounts(mockitoEq(applicationId))(any[HeaderCarrier]()))
        .thenReturn(Future.failed(new RuntimeException("backend unavailable")))

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val result = route(application, FakeRequest(GET, routes.SubmitClaimDeclarationController.onPageLoad.url)).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "redirects to journey recovery through the navigator when Accept and submit is selected" in {
      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val result = route(application, FakeRequest(POST, routes.SubmitClaimDeclarationController.onSubmit.url)).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
