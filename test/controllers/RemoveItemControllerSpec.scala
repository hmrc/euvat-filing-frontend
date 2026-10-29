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
import models.UserAnswers
import models.requests.DeleteItemRequest
import models.responses.{ApplicationResponse, DeleteItemResponse, ImportDetailsResponse, PurchaseDetailsResponse}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{never, verify, when}
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.{ClaimApplicationResponseQuery, PurchaseImportTotalItemsQuery, RemoveItemNumberQuery, RemoveItemTypeQuery, UpdateSequenceNumberQuery}
import repositories.SessionRepository

import scala.concurrent.Future

class RemoveItemControllerSpec extends SpecBase {

  "RemoveItem Controller" - {

    val importDetailsResponse = ImportDetailsResponse(
      applicationId               = 111,
      itemNumber                  = 2,
      issuingDate                 = Some(java.time.LocalDateTime.of(2026, 10, 21, 0, 0)),
      supplierName                = Some("Supplier"),
      supplierAddressLine1        = Some("Address line 1"),
      supplierAddressLine2        = None,
      supplierAddressLine3        = None,
      supplierCountryCode         = Some("FR"),
      goodsDescriptionCategory    = "1",
      currencyCode                = Some("EU"),
      taxableAmount               = Some(BigDecimal(300)),
      vatAmount                   = Some(BigDecimal(200)),
      deductibleVatAmount         = Some(BigDecimal(42)),
      hasSadReferenceNumber       = Some("N"),
      sadReferenceNumber          = None,
      referenceInformation        = None,
      goodsDescriptionText        = Some("Import details"),
      goodsDescriptionSubcategory = Some("1.2.3"),
      updateSequenceNumber        = 7
    )

    val purchaseDetailsResponse = PurchaseDetailsResponse(
      goodsDescriptionCode       = "1",
      goodsDescriptionSubCode    = Some("1.2.3"),
      goodsDescriptionText       = Some("Purchase details"),
      simplifiedInvoiceIndicator = Some("N"),
      supplierName               = Some("Supplier"),
      supplierAddressLine1       = Some("Address line 1"),
      supplierAddressLine2       = Some("Address line 2"),
      supplierAddressLine3       = Some("Address line 3"),
      supplierVatNumber          = Some("VAT123"),
      supplierTaxIdentifier      = Some("TAX123"),
      invoiceDate                = Some(java.time.LocalDateTime.of(2026, 10, 21, 0, 0)),
      invoiceNumber              = Some("INV123"),
      currencyCode               = Some("EUR"),
      taxableAmount              = Some(BigDecimal(300)),
      vatAmount                  = Some(BigDecimal(200)),
      deductibleVatAmount        = Some(BigDecimal(100)),
      updateSequenceNumber       = 9
    )

    def userAnswersWithItem(itemNumber: Int, totalItems: Int = 2): UserAnswers =
      emptyUserAnswers
        .set(ClaimApplicationResponseQuery, ApplicationResponse(111, "App1", 1))
        .success
        .value
        .set(RemoveItemNumberQuery, itemNumber)
        .success
        .value
        .set(RemoveItemTypeQuery, if (itemNumber == 1) "P" else "I")
        .success
        .value
        .set(PurchaseImportTotalItemsQuery, totalItems)
        .success
        .value

    def removeItemRoute(itemType: String, itemNumber: Int): String = routes.RemoveItemController.onPageLoad(itemType, itemNumber).url
    lazy val submitRoute = "/file-eu-vat/purchases-imports/remove-item"

    "generate remove-item routes under purchases-imports" in {
      removeItemRoute("P", 1).endsWith("/purchases-imports/remove-item/P/1") mustBe true
      routes.RemoveItemController.onSubmit.url.endsWith("/purchases-imports/remove-item") mustBe true
      submitRoute.endsWith("/purchases-imports/remove-item") mustBe true

      val router = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build().injector.instanceOf[play.api.routing.Router]
      val removeRoutes = router.documentation.filter(_._3.contains("RemoveItemController"))
      withClue(removeRoutes.mkString("\n")) {
        removeRoutes.exists { case (method, path, _) => method == "POST" && path.endsWith("/purchases-imports/remove-item") } mustBe true
      }
    }

    "save the item type and number from the path and display returned import VAT" in {
      when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))
      when(mockEuVatRefundsService.getImportDetails(eqTo(111L), eqTo(2))(any())).thenReturn(Future.successful(importDetailsResponse))
      val userAnswers = emptyUserAnswers
        .set(ClaimApplicationResponseQuery, ApplicationResponse(111, "App1", 1))
        .success
        .value
        .set(PurchaseImportTotalItemsQuery, 2)
        .success
        .value
      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
        .build()

      running(application) {
        val request = FakeRequest(GET, removeItemRoute("I", 2))
        val result = route(application, request).value
        status(result) mustEqual OK
        contentAsString(result) must include("42")
        verify(mockEuVatRefundsService).getImportDetails(eqTo(111L), eqTo(2))(any())
        verify(mockEuVatRefundsService, never()).getPurchaseImportList(any())(any())

        val answersCaptor = ArgumentCaptor.forClass(classOf[UserAnswers])
        verify(mockSessionRepository, org.mockito.Mockito.atLeastOnce()).set(answersCaptor.capture())
        answersCaptor.getValue.get(RemoveItemNumberQuery) mustEqual Some(2)
        answersCaptor.getValue.get(RemoveItemTypeQuery) mustEqual Some("I")
        answersCaptor.getValue.get(UpdateSequenceNumberQuery) mustEqual Some(7)
      }
    }

    "return OK and show the type and VAT claim for a complete item" in {
      when(mockEuVatRefundsService.getPurchaseDetails(eqTo(111L), eqTo(1))(any())).thenReturn(Future.successful(purchaseDetailsResponse))
      when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))
      val application = applicationBuilder(userAnswers = Some(userAnswersWithItem(1)))
        .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
        .build()

      running(application) {
        val request = FakeRequest(GET, removeItemRoute("P", 1))
        val result = route(application, request).value
        status(result) mustEqual OK
        contentAsString(result) must include("Purchase")
        contentAsString(result) must include("100")
        verify(mockEuVatRefundsService).getPurchaseDetails(eqTo(111L), eqTo(1))(any())
        verify(mockEuVatRefundsService, never()).getPurchaseImportList(any())(any())

        val answersCaptor = ArgumentCaptor.forClass(classOf[UserAnswers])
        verify(mockSessionRepository, org.mockito.Mockito.atLeastOnce()).set(answersCaptor.capture())
        answersCaptor.getValue.get(UpdateSequenceNumberQuery) mustEqual Some(9)
      }
    }

    "show Not provided for an incomplete item" in {
      when(mockEuVatRefundsService.getImportDetails(eqTo(111L), eqTo(2))(any()))
        .thenReturn(Future.successful(importDetailsResponse.copy(deductibleVatAmount = None)))
      val application = applicationBuilder(userAnswers = Some(userAnswersWithItem(2))).build()

      running(application) {
        val request = FakeRequest(GET, removeItemRoute("I", 2))
        val result = route(application, request).value
        status(result) mustEqual OK
        contentAsString(result) must include("Import")
        contentAsString(result) must include(messages(application)("removeItem.notProvided"))
      }
    }

    "redirect to JourneyRecovery when the item is not found" in {
      when(mockEuVatRefundsService.getPurchaseDetails(eqTo(111L), eqTo(99))(any()))
        .thenReturn(Future.failed(new RuntimeException("Item not found")))
      val application = applicationBuilder(userAnswers = Some(userAnswersWithItem(99))).build()

      running(application) {
        val request = FakeRequest(GET, removeItemRoute("P", 99))
        val result = route(application, request).value
        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "redirect to JourneyRecovery without API calls when the summary item count is missing" in {
      val userAnswers = userAnswersWithItem(1).remove(PurchaseImportTotalItemsQuery).success.value
      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, removeItemRoute("P", 1))
        val result = route(application, request).value
        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        verify(mockEuVatRefundsService, never()).getPurchaseImportList(any())(any())
        verify(mockEuVatRefundsService, never()).getPurchaseDetails(any(), any())(any())
      }
    }

    "redirect to Journey Recovery when no existing data is found" in {
      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, removeItemRoute("P", 1))
        val result = route(application, request).value
        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "redirect to the summary page when the user selects yes and other items remain" in {
      when(mockEuVatRefundsService.deletePurchase(any())(any())).thenReturn(Future.successful(DeleteItemResponse(2)))
      when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))
      val application = applicationBuilder(userAnswers = Some(userAnswersWithItem(1)))
        .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
        .build()

      running(application) {
        val request = FakeRequest(POST, submitRoute).withFormUrlEncodedBody("value" -> "true")
        val result = route(application, request).value
        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.PurchaseImportSummaryController.onPageLoad.url
        val requestCaptor = ArgumentCaptor.forClass(classOf[DeleteItemRequest])
        verify(mockEuVatRefundsService).deletePurchase(requestCaptor.capture())(any())
        requestCaptor.getValue mustEqual DeleteItemRequest(111, 1, 1)
        verify(mockEuVatRefundsService, never()).deleteImport(any())(any())

        val answersCaptor = ArgumentCaptor.forClass(classOf[UserAnswers])
        verify(mockSessionRepository).set(answersCaptor.capture())
        answersCaptor.getValue.get(UpdateSequenceNumberQuery) mustEqual Some(2)
      }
    }

    "redirect to the task list when the user selects yes and it's the only item" in {
      when(mockEuVatRefundsService.deletePurchase(any())(any())).thenReturn(Future.successful(DeleteItemResponse(2)))
      when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))
      val application = applicationBuilder(userAnswers = Some(userAnswersWithItem(1, totalItems = 1)))
        .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
        .build()

      running(application) {
        val request = FakeRequest(POST, submitRoute).withFormUrlEncodedBody("value" -> "true")
        val result = route(application, request).value
        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.TaskListDashboardController.onPageLoad().url
        val requestCaptor = ArgumentCaptor.forClass(classOf[DeleteItemRequest])
        verify(mockEuVatRefundsService).deletePurchase(requestCaptor.capture())(any())
        requestCaptor.getValue mustEqual DeleteItemRequest(111, 1, 1)
      }
    }

    "delete an import using the import endpoint" in {
      when(mockEuVatRefundsService.deleteImport(any())(any())).thenReturn(Future.successful(DeleteItemResponse(2)))
      when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))
      val importAnswers = userAnswersWithItem(2).set(UpdateSequenceNumberQuery, 7).success.value
      val application = applicationBuilder(userAnswers = Some(importAnswers))
        .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
        .build()

      running(application) {
        val request = FakeRequest(POST, submitRoute).withFormUrlEncodedBody("value" -> "true")
        val result = route(application, request).value
        status(result) mustEqual SEE_OTHER
        val requestCaptor = ArgumentCaptor.forClass(classOf[DeleteItemRequest])
        verify(mockEuVatRefundsService).deleteImport(requestCaptor.capture())(any())
        requestCaptor.getValue mustEqual DeleteItemRequest(111, 2, 7)
        verify(mockEuVatRefundsService, never()).deletePurchase(any())(any())
      }
    }

    "redirect to the summary page when the user selects no" in {
      when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))
      val application = applicationBuilder(userAnswers = Some(userAnswersWithItem(1)))
        .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
        .build()

      running(application) {
        val request = FakeRequest(POST, submitRoute).withFormUrlEncodedBody("value" -> "false")
        val result = route(application, request).value
        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.PurchaseImportSummaryController.onPageLoad.url
        verify(mockEuVatRefundsService, never()).deletePurchase(any())(any())
        verify(mockEuVatRefundsService, never()).deleteImport(any())(any())
      }
    }

    "redirect BAD REQUEST when no answer is submitted" in {
      when(mockEuVatRefundsService.getPurchaseDetails(eqTo(111L), eqTo(1))(any())).thenReturn(Future.successful(purchaseDetailsResponse))
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
