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
import forms.PurchaseImportSummaryFormProvider
import models.PurchaseImport
import models.responses.{ApplicationResponse, PurchaseImportListResponse}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import pages.PurchaseImportSummaryPage
import play.api.data.Form
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.ClaimApplicationResponseQuery
import repositories.SessionRepository

import scala.concurrent.Future

class PurchaseImportSummaryControllerSpec extends SpecBase {
  val formProvider = new PurchaseImportSummaryFormProvider()
  val form: Form[Boolean] = formProvider()

  "PurchaseImportSummary Controller" - {
    val purchaseImportResponse = PurchaseImportListResponse(
      totalItems     = 2,
      totalVatClaims = BigDecimal(334),
      purchaseImportList = List(
        PurchaseImport(
          itemNumber                  = 123,
          itemType                    = "P",
          goodsDescriptionCategory    = "1",
          goodsDescriptionSubCategory = Some("1.2.3"),
          currencyCode                = "EU",
          taxableAmount               = BigDecimal(300),
          vatAmount                   = BigDecimal(200),
          deductibleVatAmount         = BigDecimal(100)
        ),
        PurchaseImport(
          itemNumber                  = 456,
          itemType                    = "I",
          goodsDescriptionCategory    = "7",
          goodsDescriptionSubCategory = Some("7.9"),
          currencyCode                = "EU",
          taxableAmount               = BigDecimal(456),
          vatAmount                   = BigDecimal(345),
          deductibleVatAmount         = BigDecimal(234)
        )
      )
    )

    "return OK on page load" in {
      val userAnswers = emptyUserAnswers.set(ClaimApplicationResponseQuery, ApplicationResponse(111, "App1", 1)).success.value
      when(mockEuVatRefundsService.getPurchaseImportList(any())(any())).thenReturn(Future.successful(purchaseImportResponse))

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .build()

      running(application) {
        val request = FakeRequest(GET, routes.PurchaseImportSummaryController.onPageLoad.url)
        val result = route(application, request).value
        status(result) mustEqual OK
      }
    }

    "redirect to PurchaseOrImportController when user selects yes" in {
      when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))
      val userAnswers = emptyUserAnswers.set(PurchaseImportSummaryPage, true).success.value
      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
        .build()

      running(application) {
        val request = FakeRequest(POST, routes.PurchaseImportSummaryController.onSubmit.url).withFormUrlEncodedBody(
          "value" -> "true"
        )
        val result = route(application, request).value
        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.PurchaseOrImportController.onPageLoad.url
      }
    }

    "redirect to TaskListDashboardController when user selects no" in {
      when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
        .build()

      running(application) {
        val request = FakeRequest(POST, routes.PurchaseImportSummaryController.onSubmit.url).withFormUrlEncodedBody(
          "value" -> "false"
        )
        val result = route(application, request).value
        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.TaskListDashboardController.onPageLoad().url
      }
    }

    "return BAD_REQUEST when no answer is submitted" in {
      val userAnswers = emptyUserAnswers.set(ClaimApplicationResponseQuery, ApplicationResponse(111, "App1", 1)).success.value

      val summaryResponse = PurchaseImportListResponse(
        purchaseImportList = List.empty,
        totalItems         = 0,
        totalVatClaims     = java.math.BigDecimal.ZERO
      )

      when(mockEuVatRefundsService.getPurchaseImportList(any())(any())).thenReturn(Future.successful(summaryResponse))

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(POST, routes.PurchaseImportSummaryController.onSubmit.url).withFormUrlEncodedBody()

        val result = route(application, request).value
        status(result) mustEqual BAD_REQUEST
      }
    }

  }
}
