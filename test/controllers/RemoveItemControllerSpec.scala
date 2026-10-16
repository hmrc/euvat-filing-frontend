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
import models.{PurchaseImport, UserAnswers}
import models.responses.{ApplicationResponse, PurchaseImportListResponse}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.{ClaimApplicationResponseQuery, RemoveItemNumberQuery}
import repositories.SessionRepository

import scala.concurrent.Future

class RemoveItemControllerSpec extends SpecBase {

  "RemoveItem Controller" - {

    def purchaseImport(itemNumber: Int, itemType: String, deductibleVatAmount: BigDecimal): PurchaseImport =
      PurchaseImport(
        itemNumber                  = itemNumber,
        itemType                    = itemType,
        goodsDescriptionCategory    = "1",
        goodsDescriptionSubCategory = Some("1.2.3"),
        currencyCode                = Some("EU"),
        taxableAmount               = BigDecimal(300),
        vatAmount                   = BigDecimal(200),
        deductibleVatAmount         = deductibleVatAmount
      )

    val completePurchase = purchaseImport(itemNumber = 1, itemType = "P", deductibleVatAmount = BigDecimal(100))
    val incompleteImport = purchaseImport(itemNumber = 2, itemType = "I", deductibleVatAmount = BigDecimal(0))

    val twoItemsResponse = PurchaseImportListResponse(
      totalItems         = 2,
      totalVatClaims     = BigDecimal(100),
      purchaseImportList = List(completePurchase, incompleteImport)
    )

    val oneItemResponse = PurchaseImportListResponse(
      totalItems         = 1,
      totalVatClaims     = BigDecimal(100),
      purchaseImportList = List(completePurchase)
    )

    def userAnswersWithItem(itemNumber: Int): UserAnswers =
      emptyUserAnswers
        .set(ClaimApplicationResponseQuery, ApplicationResponse(111, "App1", 1))
        .success
        .value
        .set(RemoveItemNumberQuery, itemNumber)
        .success
        .value

    lazy val removeItemRoute = routes.RemoveItemController.onPageLoad.url
    lazy val submitRoute = routes.RemoveItemController.onSubmit.url

    "save the item number from the url and reload the page" in {
      when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))
      val userAnswers = emptyUserAnswers.set(ClaimApplicationResponseQuery, ApplicationResponse(111, "App1", 1)).success.value
      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
        .build()

      running(application) {
        val request = FakeRequest(GET, s"$removeItemRoute?itemNumber=1")
        val result = route(application, request).value
        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual removeItemRoute
      }
    }

    "return OK and show the type and VAT claim for a complete item" in {
      when(mockEuVatRefundsService.getPurchaseImportList(any())(any())).thenReturn(Future.successful(twoItemsResponse))
      val application = applicationBuilder(userAnswers = Some(userAnswersWithItem(1))).build()

      running(application) {
        val request = FakeRequest(GET, removeItemRoute)
        val result = route(application, request).value
        status(result) mustEqual OK
        contentAsString(result) must include("Purchase")
        contentAsString(result) must include("100")
      }
    }

    "show Not provided for an incomplete item" in {
      when(mockEuVatRefundsService.getPurchaseImportList(any())(any())).thenReturn(Future.successful(twoItemsResponse))
      val application = applicationBuilder(userAnswers = Some(userAnswersWithItem(2))).build()

      running(application) {
        val request = FakeRequest(GET, removeItemRoute)
        val result = route(application, request).value
        status(result) mustEqual OK
        contentAsString(result) must include("Import")
        contentAsString(result) must include(messages(application)("removeItem.notProvided"))
      }
    }

    "redirect to JourneyRecovery when the item is not found" in {
      when(mockEuVatRefundsService.getPurchaseImportList(any())(any())).thenReturn(Future.successful(twoItemsResponse))
      val application = applicationBuilder(userAnswers = Some(userAnswersWithItem(99))).build()

      running(application) {
        val request = FakeRequest(GET, removeItemRoute)
        val result = route(application, request).value
        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "redirect to Journey Recovery when there is no item number in the session" in {
      val userAnswers = emptyUserAnswers.set(ClaimApplicationResponseQuery, ApplicationResponse(111, "App1", 1)).success.value
      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, removeItemRoute)
        val result = route(application, request).value
        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "redirect to Journey Recovery when no existing data is found" in {
      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, removeItemRoute)
        val result = route(application, request).value
        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "redirect to the summary page when the user selects yes and other items remain" in {
      when(mockEuVatRefundsService.getPurchaseImportList(any())(any())).thenReturn(Future.successful(twoItemsResponse))
      when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))
      val application = applicationBuilder(userAnswers = Some(userAnswersWithItem(1)))
        .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
        .build()

      running(application) {
        val request = FakeRequest(POST, submitRoute).withFormUrlEncodedBody("value" -> "true")
        val result = route(application, request).value
        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.PurchaseImportSummaryController.onPageLoad.url
      }
    }

    "redirect to the task list when the user selects yes and it's the only item" in {
      when(mockEuVatRefundsService.getPurchaseImportList(any())(any())).thenReturn(Future.successful(oneItemResponse))
      when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))
      val application = applicationBuilder(userAnswers = Some(userAnswersWithItem(1)))
        .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
        .build()

      running(application) {
        val request = FakeRequest(POST, submitRoute).withFormUrlEncodedBody("value" -> "true")
        val result = route(application, request).value
        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.TaskListDashboardController.onPageLoad().url
      }
    }

    "redirect to the summary page when the user selects no" in {
      when(mockEuVatRefundsService.getPurchaseImportList(any())(any())).thenReturn(Future.successful(oneItemResponse))
      when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))
      val application = applicationBuilder(userAnswers = Some(userAnswersWithItem(1)))
        .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
        .build()

      running(application) {
        val request = FakeRequest(POST, submitRoute).withFormUrlEncodedBody("value" -> "false")
        val result = route(application, request).value
        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.PurchaseImportSummaryController.onPageLoad.url
      }
    }

    "redirect BAD REQUEST when no answer is submitted" in {
      when(mockEuVatRefundsService.getPurchaseImportList(any())(any())).thenReturn(Future.successful(twoItemsResponse))
      val application = applicationBuilder(userAnswers = Some(userAnswersWithItem(1))).build()

      running(application) {
        val request = FakeRequest(POST, submitRoute).withFormUrlEncodedBody()
        val result = route(application, request).value
        status(result) mustEqual BAD_REQUEST
        contentAsString(result) must include(messages(application)("removeItem.error.required"))
      }
    }
  }
}
