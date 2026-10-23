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

package controllers.purchase

import base.SpecBase
import models.requests.UpdatePurchaseRequest
import models.responses.{AddPurchaseResponse, PurchaseDetailResponse, UpdatePurchaseResponse}
import models.{Fuel, InvoiceType, SupplierAddress}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import org.mockito.{ArgumentCaptor, Mockito}
import org.scalatestplus.mockito.MockitoSugar
import pages.claim.RefundingCountryPage
import pages.purchase.*
import play.api.inject.bind
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import play.api.{Application, Configuration}
import repositories.SessionRepository
import services.EuVatRefundsService
import utils.{Currency, CurrencyConfig}

import java.time.LocalDate
import scala.concurrent.Future

class CheckYourPurchaseDetailsControllerSpec extends SpecBase with MockitoSugar {

  "CheckYourPurchaseDetailsController" - {

    "should use PurchaseSubTypePage value for goodsCategory and fall back to InvoiceType when simplified flag missing" in {
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      when(mockEuVatRefundsService.updatePurchase(any())(any())) thenReturn Future.successful(
        UpdatePurchaseResponse(updateSequenceNumber = 5)
      )

      val userAnswers = emptyUserAnswers
        .set(PurchaseTypePage, Fuel)
        .success
        .value
        .set(PurchaseSubTypePage, "1.2")
        .success
        .value
        .set(AddPurchaseResponsePage, AddPurchaseResponse(itemNumber = 1, updateSequenceNumber = 1))
        .success
        .value
        .set(queries.ClaimApplicationResponseQuery, models.responses.ApplicationResponse(1, "GB001", 1))
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
        .build()

      running(application) {
        val request = FakeRequest(POST, controllers.purchase.routes.CheckYourPurchaseDetailsController.onSubmit().url)
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        verify(mockSessionRepository).set(any())
      }
    }

    "should derive simplified indicator from InvoiceType when explicit flag missing" in {
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      when(mockEuVatRefundsService.updatePurchase(any())(any())) thenReturn Future.successful(
        UpdatePurchaseResponse(updateSequenceNumber = 7)
      )

      val userAnswers = emptyUserAnswers
        .set(InvoiceTypePage, InvoiceType.SimplifiedInvoice)
        .success
        .value
        .set(AddPurchaseResponsePage, AddPurchaseResponse(itemNumber = 2, updateSequenceNumber = 2))
        .success
        .value
        .set(queries.ClaimApplicationResponseQuery, models.responses.ApplicationResponse(2, "GB002", 2))
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
        .build()

      running(application) {
        val request = FakeRequest(POST, controllers.purchase.routes.CheckYourPurchaseDetailsController.onSubmit().url)
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        verify(mockSessionRepository).set(any())
      }
    }

    "should use explicit SupplierVatRegCheckPage when present (true)" in {
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val captor: ArgumentCaptor[UpdatePurchaseRequest] = ArgumentCaptor.forClass(classOf[UpdatePurchaseRequest])
      when(mockEuVatRefundsService.updatePurchase(captor.capture())(any())) thenReturn Future.successful(
        UpdatePurchaseResponse(updateSequenceNumber = 9)
      )

      val userAnswers = emptyUserAnswers
        .set(PurchaseTypePage, Fuel)
        .success
        .value
        .set(PurchaseSubTypePage, "1.2")
        .success
        .value
        .set(SupplierVatRegCheckPage, true)
        .success
        .value
        .set(AddPurchaseResponsePage, AddPurchaseResponse(itemNumber = 3, updateSequenceNumber = 3))
        .success
        .value
        .set(queries.ClaimApplicationResponseQuery, models.responses.ApplicationResponse(3, "GB003", 3))
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(POST, controllers.purchase.routes.CheckYourPurchaseDetailsController.onSubmit().url)
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        val sent = captor.getValue
        sent.goodsDescriptionCategory    must include("1")
        sent.goodsDescriptionSubCategory must contain("1.2")
        sent.simplifiedInvoiceIndicator  must contain("true")
      }
    }

    "should use explicit SupplierVatRegCheckPage when present (false)" in {
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val captor: ArgumentCaptor[UpdatePurchaseRequest] = ArgumentCaptor.forClass(classOf[UpdatePurchaseRequest])
      when(mockEuVatRefundsService.updatePurchase(captor.capture())(any())) thenReturn Future.successful(
        UpdatePurchaseResponse(updateSequenceNumber = 10)
      )

      val userAnswers = emptyUserAnswers
        .set(PurchaseTypePage, Fuel)
        .success
        .value
        .set(PurchaseSubTypePage, "1.2")
        .success
        .value
        .set(SupplierVatRegCheckPage, false)
        .success
        .value
        .set(AddPurchaseResponsePage, AddPurchaseResponse(itemNumber = 4, updateSequenceNumber = 4))
        .success
        .value
        .set(queries.ClaimApplicationResponseQuery, models.responses.ApplicationResponse(4, "GB004", 4))
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(POST, controllers.purchase.routes.CheckYourPurchaseDetailsController.onSubmit().url)
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        val sent = captor.getValue
        sent.simplifiedInvoiceIndicator must contain("false")
      }
    }

    "should submit no simplified indicator when invoice type is standard even if simplified flag remains true" in {
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val captor: ArgumentCaptor[UpdatePurchaseRequest] = ArgumentCaptor.forClass(classOf[UpdatePurchaseRequest])
      when(mockEuVatRefundsService.updatePurchase(captor.capture())(any())) thenReturn Future.successful(
        UpdatePurchaseResponse(updateSequenceNumber = 12)
      )

      val userAnswers = emptyUserAnswers
        .set(InvoiceTypePage, InvoiceType.StandardInvoice)
        .success
        .value
        .set(SupplierVatRegCheckPage, true)
        .success
        .value
        .set(AddPurchaseResponsePage, AddPurchaseResponse(itemNumber = 5, updateSequenceNumber = 5))
        .success
        .value
        .set(queries.ClaimApplicationResponseQuery, models.responses.ApplicationResponse(5, "GB005", 5))
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(POST, controllers.purchase.routes.CheckYourPurchaseDetailsController.onSubmit().url)
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        val sent = captor.getValue
        sent.simplifiedInvoiceIndicator mustBe None
      }
    }

    "should submit simplified false indicator when invoice type is simplified and vat reg check is no" in {
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val captor: ArgumentCaptor[UpdatePurchaseRequest] = ArgumentCaptor.forClass(classOf[UpdatePurchaseRequest])
      when(mockEuVatRefundsService.updatePurchase(captor.capture())(any())) thenReturn Future.successful(
        UpdatePurchaseResponse(updateSequenceNumber = 13)
      )

      val userAnswers = emptyUserAnswers
        .set(InvoiceTypePage, InvoiceType.SimplifiedInvoice)
        .success
        .value
        .set(SupplierVatRegCheckPage, false)
        .success
        .value
        .set(AddPurchaseResponsePage, AddPurchaseResponse(itemNumber = 6, updateSequenceNumber = 6))
        .success
        .value
        .set(queries.ClaimApplicationResponseQuery, models.responses.ApplicationResponse(6, "GB006", 6))
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(POST, controllers.purchase.routes.CheckYourPurchaseDetailsController.onSubmit().url)
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        val sent = captor.getValue
        sent.simplifiedInvoiceIndicator must contain("false")
      }
    }

    "should redirect to JourneyRecovery when service call fails" in {
      when(mockEuVatRefundsService.updatePurchase(any())(any())).thenReturn(Future.failed(new RuntimeException("boom")))

      val userAnswers = emptyUserAnswers
        .set(PurchaseSubTypePage, "1.2")
        .success
        .value
        .set(AddPurchaseResponsePage, AddPurchaseResponse(itemNumber = 5, updateSequenceNumber = 5))
        .success
        .value
        .set(queries.ClaimApplicationResponseQuery, models.responses.ApplicationResponse(5, "GB005", 5))
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(POST, controllers.purchase.routes.CheckYourPurchaseDetailsController.onSubmit().url)
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value must include(controllers.routes.JourneyRecoveryController.onPageLoad().url)
      }
    }

    "should redirect to JourneyRecovery when ClaimApplicationResponseQuery missing" in {
      when(mockEuVatRefundsService.updatePurchase(any())(any())) thenReturn Future.successful(
        models.responses.UpdatePurchaseResponse(updateSequenceNumber = 11)
      )

      val userAnswers = emptyUserAnswers
        .set(PurchaseSubTypePage, "1.2")
        .success
        .value
        .set(AddPurchaseResponsePage, AddPurchaseResponse(itemNumber = 6, updateSequenceNumber = 6))
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(POST, controllers.purchase.routes.CheckYourPurchaseDetailsController.onSubmit().url)
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value must include(controllers.routes.JourneyRecoveryController.onPageLoad().url)
      }
    }

    "onPageLoad should redirect to changeYourAnswers when PurchaseDetailRouteQuery exists" in {
      val ua = emptyUserAnswers
        .set(queries.PurchaseDetailRouteQuery, queries.PurchaseDetailRoute(123L, 7))
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(ua)).build()

      running(application) {
        val request = FakeRequest(GET, controllers.purchase.routes.CheckYourPurchaseDetailsController.onPageLoad().url)
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.purchase.routes.CheckYourPurchaseDetailsController.changeYourAnswers(123L, 7).url
      }
    }

    "changeYourAnswers should reuse stored purchase detail response without fetching again" in {
      val ua = emptyUserAnswers
        .set(RefundingCountryPage, "FR")
        .success
        .value
        .set(InvoiceTypePage, InvoiceType.SimplifiedInvoice)
        .success
        .value
        .set(SupplierVatRegCheckPage, false)
        .success
        .value
        .set(queries.PurchaseDetailRouteQuery, queries.PurchaseDetailRoute(123L, 7))
        .success
        .value
        .set(
          queries.PurchaseDetailResponseQuery,
          PurchaseDetailResponse(
            goodsDescriptionCode       = Some("1"),
            goodsDescriptionSubCode    = Some("1.2"),
            goodsDescriptionText       = Some("Fuel"),
            simplifiedInvoiceIndicator = Some("false"),
            supplierName               = Some("Supplier"),
            supplierAddressLine1       = None,
            supplierAddressLine2       = None,
            supplierAddressLine3       = None,
            supplierVatNumber          = None,
            supplierTaxIdentifier      = None,
            invoiceDate                = None,
            invoiceNumber              = Some("INV-1"),
            currencyCode               = Some("EUR"),
            taxableAmount              = Some(BigDecimal(100)),
            vatAmount                  = Some(BigDecimal(20)),
            deductibleVatAmount        = Some(BigDecimal(10)),
            updateSequenceNumber       = Some(1)
          )
        )
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(ua)).build()

      running(application) {
        val request = FakeRequest(GET, controllers.purchase.routes.CheckYourPurchaseDetailsController.changeYourAnswers(123L, 7).url)
        val result = route(application, request).value

        status(result) mustEqual OK
        verify(mockEuVatRefundsService, never()).getPurchaseDetails(any[Long], any[Int])(any())
      }
    }

    "changeYourAnswers should show Continue when cached purchase detail matches current answers" in {
      val cachedResponse = PurchaseDetailResponse(
        goodsDescriptionCode       = Some("1"),
        goodsDescriptionSubCode    = Some("1.2"),
        goodsDescriptionText       = Some("Fuel"),
        simplifiedInvoiceIndicator = Some("false"),
        supplierName               = Some("Supplier"),
        supplierAddressLine1       = None,
        supplierAddressLine2       = None,
        supplierAddressLine3       = None,
        supplierVatNumber          = None,
        supplierTaxIdentifier      = None,
        invoiceDate                = None,
        invoiceNumber              = Some("INV-1"),
        currencyCode               = Some("EUR"),
        taxableAmount              = Some(BigDecimal(100)),
        vatAmount                  = Some(BigDecimal(20)),
        deductibleVatAmount        = Some(BigDecimal(10)),
        updateSequenceNumber       = Some(1)
      )

      val ua = emptyUserAnswers
        .set(RefundingCountryPage, "FR")
        .success
        .value
        .set(PurchaseTypePage, Fuel)
        .success
        .value
        .set(PurchaseSubTypePage, "1.2")
        .success
        .value
        .set(DescribeItemsOnInvoicePage, "Fuel")
        .success
        .value
        .set(InvoiceTypePage, InvoiceType.SimplifiedInvoice)
        .success
        .value
        .set(SupplierVatRegCheckPage, false)
        .success
        .value
        .set(SuppliersNamePage, "Supplier")
        .success
        .value
        .set(InvoiceNumberPage, "INV-1")
        .success
        .value
        .set(RefundingCurrencyPage, "EUR")
        .success
        .value
        .set(TotalPurchaseAmountBeforeVatPage, BigDecimal(100))
        .success
        .value
        .set(TotalVatPaidPage, BigDecimal(20))
        .success
        .value
        .set(TotalVatClaimPage, BigDecimal(10))
        .success
        .value
        .set(AddPurchaseResponsePage, AddPurchaseResponse(itemNumber = 4, updateSequenceNumber = 6))
        .success
        .value
        .set(queries.PurchaseDetailRouteQuery, queries.PurchaseDetailRoute(123L, 3))
        .success
        .value
        .set(queries.PurchaseDetailResponseQuery, cachedResponse)
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(ua))
        .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
        .build()

      running(application) {
        val request = FakeRequest(GET, controllers.purchase.routes.CheckYourPurchaseDetailsController.changeYourAnswers(123L, 3).url)
        val result = route(application, request).value
        val body = contentAsString(result)

        status(result) mustEqual OK
        body must include(messages(application)("site.continue"))
        body must not include messages(application)("site.save.continue")
      }
    }

    "changeYourAnswers should show Save and continue when cached purchase detail differs and route query exists" in {
      val ua = emptyUserAnswers
        .set(RefundingCountryPage, "FR")
        .success
        .value
        .set(PurchaseTypePage, Fuel)
        .success
        .value
        .set(PurchaseSubTypePage, "1.2")
        .success
        .value
        .set(DescribeItemsOnInvoicePage, "Changed fuel")
        .success
        .value
        .set(InvoiceTypePage, InvoiceType.SimplifiedInvoice)
        .success
        .value
        .set(SupplierVatRegCheckPage, false)
        .success
        .value
        .set(InvoiceNumberPage, "INV-1")
        .success
        .value
        .set(RefundingCurrencyPage, "EUR")
        .success
        .value
        .set(TotalPurchaseAmountBeforeVatPage, BigDecimal(100))
        .success
        .value
        .set(TotalVatPaidPage, BigDecimal(20))
        .success
        .value
        .set(TotalVatClaimPage, BigDecimal(10))
        .success
        .value
        .set(AddPurchaseResponsePage, AddPurchaseResponse(itemNumber = 7, updateSequenceNumber = 1))
        .success
        .value
        .set(queries.PurchaseDetailRouteQuery, queries.PurchaseDetailRoute(123L, 7))
        .success
        .value
        .set(
          queries.PurchaseDetailResponseQuery,
          PurchaseDetailResponse(
            goodsDescriptionCode       = Some("1"),
            goodsDescriptionSubCode    = Some("1.2"),
            goodsDescriptionText       = Some("Fuel"),
            simplifiedInvoiceIndicator = Some("false"),
            supplierName               = None,
            supplierAddressLine1       = None,
            supplierAddressLine2       = None,
            supplierAddressLine3       = None,
            supplierVatNumber          = None,
            supplierTaxIdentifier      = None,
            invoiceDate                = None,
            invoiceNumber              = Some("INV-1"),
            currencyCode               = Some("EUR"),
            taxableAmount              = Some(BigDecimal(100)),
            vatAmount                  = Some(BigDecimal(20)),
            deductibleVatAmount        = Some(BigDecimal(10)),
            updateSequenceNumber       = Some(1)
          )
        )
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(ua)).build()

      running(application) {
        val request = FakeRequest(GET, controllers.purchase.routes.CheckYourPurchaseDetailsController.changeYourAnswers(123L, 7).url)
        val result = route(application, request).value
        val body = contentAsString(result)

        status(result) mustEqual OK
        body must include(messages(application)("site.save.continue"))
      }
    }

    "changeYourAnswers should fetch the selected item when the cached route is for another item" in {
      val savedCaptor: ArgumentCaptor[models.UserAnswers] = ArgumentCaptor.forClass(classOf[models.UserAnswers])
      when(mockSessionRepository.set(savedCaptor.capture())) thenReturn Future.successful(true)
      val selectedItem = PurchaseDetailResponse(
        goodsDescriptionCode       = Some("3"),
        goodsDescriptionSubCode    = None,
        goodsDescriptionText       = None,
        simplifiedInvoiceIndicator = None,
        supplierName               = None,
        supplierAddressLine1       = None,
        supplierAddressLine2       = None,
        supplierAddressLine3       = None,
        supplierVatNumber          = None,
        supplierTaxIdentifier      = None,
        invoiceDate                = None,
        invoiceNumber              = None,
        currencyCode               = None,
        taxableAmount              = None,
        vatAmount                  = None,
        deductibleVatAmount        = None,
        updateSequenceNumber       = Some(2)
      )
      when(mockEuVatRefundsService.getPurchaseDetails(org.mockito.ArgumentMatchers.eq(123L), org.mockito.ArgumentMatchers.eq(3))(any()))
        .thenReturn(Future.successful(selectedItem))

      val ua = emptyUserAnswers
        .set(RefundingCountryPage, "FR")
        .success
        .value
        .set(PurchaseTypePage, Fuel)
        .success
        .value
        .set(PurchaseSubTypePage, "1.2")
        .success
        .value
        .set(DescribeItemsOnInvoicePage, "Changed fuel")
        .success
        .value
        .set(InvoiceTypePage, InvoiceType.SimplifiedInvoice)
        .success
        .value
        .set(SupplierVatRegCheckPage, false)
        .success
        .value
        .set(InvoiceNumberPage, "INV-1")
        .success
        .value
        .set(RefundingCurrencyPage, "EUR")
        .success
        .value
        .set(TotalPurchaseAmountBeforeVatPage, BigDecimal(100))
        .success
        .value
        .set(TotalVatPaidPage, BigDecimal(20))
        .success
        .value
        .set(TotalVatClaimPage, BigDecimal(10))
        .success
        .value
        .set(AddPurchaseResponsePage, AddPurchaseResponse(itemNumber = 7, updateSequenceNumber = 1))
        .success
        .value
        .set(queries.PurchaseDetailRouteQuery, queries.PurchaseDetailRoute(123L, 7))
        .success
        .value
        .set(
          queries.PurchaseDetailResponseQuery,
          PurchaseDetailResponse(
            goodsDescriptionCode       = Some("1"),
            goodsDescriptionSubCode    = Some("1.2"),
            goodsDescriptionText       = Some("Fuel"),
            simplifiedInvoiceIndicator = Some("false"),
            supplierName               = None,
            supplierAddressLine1       = None,
            supplierAddressLine2       = None,
            supplierAddressLine3       = None,
            supplierVatNumber          = None,
            supplierTaxIdentifier      = None,
            invoiceDate                = None,
            invoiceNumber              = Some("INV-1"),
            currencyCode               = Some("EUR"),
            taxableAmount              = Some(BigDecimal(100)),
            vatAmount                  = Some(BigDecimal(20)),
            deductibleVatAmount        = Some(BigDecimal(10)),
            updateSequenceNumber       = Some(1)
          )
        )
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(ua))
        .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
        .build()

      running(application) {
        val request = FakeRequest(GET, controllers.purchase.routes.CheckYourPurchaseDetailsController.changeYourAnswers(123L, 3).url)
        val result = route(application, request).value

        status(result) mustEqual OK
        verify(mockEuVatRefundsService).getPurchaseDetails(org.mockito.ArgumentMatchers.eq(123L), org.mockito.ArgumentMatchers.eq(3))(any())
        val saved = savedCaptor.getValue
        saved.get(queries.PurchaseDetailRouteQuery) mustBe Some(queries.PurchaseDetailRoute(123L, 3))
        saved.get(PurchaseTypePage) mustBe Some(models.Transport)
        saved.get(DescribeItemsOnInvoicePage) mustBe None
        saved.get(SuppliersNamePage) mustBe None
      }
    }

    "changeYourAnswers should fetch, map and persist purchase details when not already cached" in {
      val savedCaptor: ArgumentCaptor[models.UserAnswers] = ArgumentCaptor.forClass(classOf[models.UserAnswers])
      when(mockSessionRepository.set(savedCaptor.capture())) thenReturn Future.successful(true)

      val purchaseDetail = PurchaseDetailResponse(
        goodsDescriptionCode       = Some("1"),
        goodsDescriptionSubCode    = Some("1.2"),
        goodsDescriptionText       = Some("Fuel purchase"),
        simplifiedInvoiceIndicator = Some("false"),
        supplierName               = Some("Supplier Ltd"),
        supplierAddressLine1       = Some("Line 1"),
        supplierAddressLine2       = Some("Line 2"),
        supplierAddressLine3       = Some("Line 3"),
        supplierVatNumber          = Some("FR123456789"),
        supplierTaxIdentifier      = Some("TAX123"),
        invoiceDate                = Some(java.time.LocalDateTime.of(2026, 1, 2, 0, 0)),
        invoiceNumber              = Some("INV-123"),
        currencyCode               = Some("EUR"),
        taxableAmount              = Some(BigDecimal(100)),
        vatAmount                  = Some(BigDecimal(20)),
        deductibleVatAmount        = Some(BigDecimal(10)),
        updateSequenceNumber       = Some(11)
      )
      when(mockEuVatRefundsService.getPurchaseDetails(org.mockito.ArgumentMatchers.eq(123L), org.mockito.ArgumentMatchers.eq(7))(any()))
        .thenReturn(Future.successful(purchaseDetail))

      val ua = emptyUserAnswers
        .set(RefundingCountryPage, "FR")
        .success
        .value
        .set(AddPurchaseResponsePage, AddPurchaseResponse(itemNumber = 7, updateSequenceNumber = 11))
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(ua))
        .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
        .build()

      running(application) {
        val request = FakeRequest(GET, controllers.purchase.routes.CheckYourPurchaseDetailsController.changeYourAnswers(123L, 7).url)
        val result = route(application, request).value

        status(result) mustEqual OK

        val saved = savedCaptor.getValue
        saved.get(PurchaseTypePage) mustBe Some(Fuel)
        saved.get(PurchaseSubTypePage) mustBe Some("1.2")
        saved.get(DescribeItemsOnInvoicePage) mustBe Some("Fuel purchase")
        saved.get(InvoiceTypePage) mustBe Some(InvoiceType.SimplifiedInvoice)
        saved.get(SupplierVatRegCheckPage) mustBe Some(false)
        saved.get(SuppliersNamePage) mustBe Some("Supplier Ltd")
        saved.get(SupplierVatRegistrationNumberPage) mustBe Some("FR123456789")
        saved.get(queries.PurchaseDetailResponseQuery) mustBe Some(purchaseDetail)
        saved.get(queries.PurchaseDetailRouteQuery) mustBe Some(queries.PurchaseDetailRoute(123L, 7))
      }
    }

    "changeYourAnswers should redirect to JourneyRecovery when fetching purchase details fails" in {
      when(mockEuVatRefundsService.getPurchaseDetails(org.mockito.ArgumentMatchers.eq(123L), org.mockito.ArgumentMatchers.eq(7))(any()))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, controllers.purchase.routes.CheckYourPurchaseDetailsController.changeYourAnswers(123L, 7).url)
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "onSubmit should clear cached purchase detail response and route after successful update" in {
      val savedCaptor: ArgumentCaptor[models.UserAnswers] = ArgumentCaptor.forClass(classOf[models.UserAnswers])
      when(mockSessionRepository.set(savedCaptor.capture())) thenReturn Future.successful(true)
      when(mockEuVatRefundsService.updatePurchase(any())(any())) thenReturn Future.successful(
        models.responses.UpdatePurchaseResponse(updateSequenceNumber = 77)
      )

      val userAnswers = emptyUserAnswers
        .set(PurchaseSubTypePage, "1.2")
        .success
        .value
        .set(AddPurchaseResponsePage, AddPurchaseResponse(itemNumber = 8, updateSequenceNumber = 8))
        .success
        .value
        .set(queries.ClaimApplicationResponseQuery, models.responses.ApplicationResponse(8, "GB008", 8))
        .success
        .value
        .set(queries.PurchaseDetailRouteQuery, queries.PurchaseDetailRoute(8L, 8))
        .success
        .value
        .set(
          queries.PurchaseDetailResponseQuery,
          PurchaseDetailResponse(None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, Some(8))
        )
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
        .build()

      running(application) {
        val request = FakeRequest(POST, controllers.purchase.routes.CheckYourPurchaseDetailsController.onSubmit().url)
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        val saved = savedCaptor.getValue
        saved.get(queries.PurchaseDetailResponseQuery) mustBe None
        saved.get(queries.PurchaseDetailRouteQuery) mustBe None
      }
    }

    "onSubmit should update the selected purchase rather than the last created purchase" in {
      val updateCaptor: ArgumentCaptor[UpdatePurchaseRequest] = ArgumentCaptor.forClass(classOf[UpdatePurchaseRequest])
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
      when(mockEuVatRefundsService.updatePurchase(updateCaptor.capture())(any())) thenReturn Future.successful(
        UpdatePurchaseResponse(updateSequenceNumber = 2)
      )

      val selectedPurchase = PurchaseDetailResponse(
        goodsDescriptionCode       = Some("1"),
        goodsDescriptionSubCode    = Some("1.2"),
        goodsDescriptionText       = Some("Fuel"),
        simplifiedInvoiceIndicator = None,
        supplierName               = None,
        supplierAddressLine1       = None,
        supplierAddressLine2       = None,
        supplierAddressLine3       = None,
        supplierVatNumber          = None,
        supplierTaxIdentifier      = None,
        invoiceDate                = None,
        invoiceNumber              = None,
        currencyCode               = None,
        taxableAmount              = None,
        vatAmount                  = None,
        deductibleVatAmount        = None,
        updateSequenceNumber       = Some(1)
      )

      val userAnswers = emptyUserAnswers
        .set(PurchaseTypePage, Fuel)
        .success
        .value
        .set(PurchaseSubTypePage, "1.2")
        .success
        .value
        .set(DescribeItemsOnInvoicePage, "Fuel")
        .success
        .value
        .set(InvoiceTypePage, InvoiceType.StandardInvoice)
        .success
        .value
        .set(AddPurchaseResponsePage, AddPurchaseResponse(itemNumber = 4, updateSequenceNumber = 6))
        .success
        .value
        .set(queries.ClaimApplicationResponseQuery, models.responses.ApplicationResponse(660, "GB660", 6))
        .success
        .value
        .set(queries.PurchaseDetailRouteQuery, queries.PurchaseDetailRoute(660L, 3))
        .success
        .value
        .set(queries.PurchaseDetailResponseQuery, selectedPurchase)
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(POST, controllers.purchase.routes.CheckYourPurchaseDetailsController.onSubmit().url)
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        updateCaptor.getValue.applicationId mustBe 660L
        updateCaptor.getValue.itemNumber mustBe 3
        updateCaptor.getValue.updateSequenceNumber mustBe 1
      }
    }

    "onPageLoad should include currency row when currency selection required" in {
      val ua = emptyUserAnswers.set(RefundingCountryPage, "EE").success.value

      val application = applicationBuilder(userAnswers = Some(ua)).build()

      running(application) {
        implicit val app: Application = application
        val request = FakeRequest(GET, controllers.purchase.routes.CheckYourPurchaseDetailsController.onPageLoad().url)
        val result = route(application, request).value
        val body = contentAsString(result)
        val msgs = messages(application)

        status(result) mustEqual OK
        body must include("Currency")
      }
    }

    "onPageLoad should NOT include currency row when currency selection not required" in {
      val ua = emptyUserAnswers.set(RefundingCountryPage, "AT").success.value

      val application = applicationBuilder(userAnswers = Some(ua)).build()

      running(application) {
        val request = FakeRequest(GET, controllers.purchase.routes.CheckYourPurchaseDetailsController.onPageLoad().url)
        val result = route(application, request).value
        val body = contentAsString(result)
        val msgs = messages(application)

        status(result) mustEqual OK
        body must not include "Currency"
      }
    }

    "changeYourAnswers should not display supplier VAT registration check for standard non-Germany invoices even if stale flag exists" in {
      val ua = emptyUserAnswers
        .set(RefundingCountryPage, "FR")
        .success
        .value
        .set(InvoiceTypePage, InvoiceType.StandardInvoice)
        .success
        .value
        .set(SupplierVatRegCheckPage, true)
        .success
        .value
        .set(SupplierVatRegistrationNumberPage, "FR123456789")
        .success
        .value
        .set(queries.PurchaseDetailRouteQuery, queries.PurchaseDetailRoute(1L, 1))
        .success
        .value
        .set(
          queries.PurchaseDetailResponseQuery,
          models.responses.PurchaseDetailResponse(
            goodsDescriptionCode       = None,
            goodsDescriptionSubCode    = None,
            goodsDescriptionText       = None,
            simplifiedInvoiceIndicator = None,
            supplierName               = None,
            supplierAddressLine1       = None,
            supplierAddressLine2       = None,
            supplierAddressLine3       = None,
            supplierVatNumber          = Some("FR123456789"),
            supplierTaxIdentifier      = None,
            invoiceDate                = None,
            invoiceNumber              = None,
            currencyCode               = None,
            taxableAmount              = None,
            vatAmount                  = None,
            deductibleVatAmount        = None,
            updateSequenceNumber       = Some(1)
          )
        )
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(ua)).build()

      running(application) {
        val request = FakeRequest(GET, controllers.purchase.routes.CheckYourPurchaseDetailsController.changeYourAnswers(1L, 1).url)
        val result = route(application, request).value
        val body = contentAsString(result)

        status(result) mustEqual OK
        body must not include "Supplier VAT registration check"
      }
    }

    "onSubmit should map subcategory, invoiceDate.atStartOfDay, supplier address and vat/tax ids" in {
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val captor: ArgumentCaptor[UpdatePurchaseRequest] = ArgumentCaptor.forClass(classOf[UpdatePurchaseRequest])
      when(mockEuVatRefundsService.updatePurchase(captor.capture())(any())) thenReturn Future.successful(
        models.responses.UpdatePurchaseResponse(updateSequenceNumber = 21)
      )

      val invoiceDate = LocalDate.of(2026, 1, 2)
      val supplierAddr = SupplierAddress(line1 = "L1", line2 = Some("L2"), line3 = Some("L3"))

      val userAnswers = emptyUserAnswers
        .set(PurchaseSubTypePage, "1.2")
        .success
        .value
        .set(PurchaseSubCategoryPage, "7.1")
        .success
        .value
        .set(InvoiceDatePage, invoiceDate)
        .success
        .value
        .set(SupplierAddressPage, supplierAddr)
        .success
        .value
        .set(SupplierVatRegistrationNumberPage, "VAT123")
        .success
        .value
        .set(SupplierTaxIdentifierNumberPage, "TAX456")
        .success
        .value
        .set(AddPurchaseResponsePage, AddPurchaseResponse(itemNumber = 7, updateSequenceNumber = 7))
        .success
        .value
        .set(queries.ClaimApplicationResponseQuery, models.responses.ApplicationResponse(7, "GB007", 7))
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(POST, controllers.purchase.routes.CheckYourPurchaseDetailsController.onSubmit().url)
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        val sent = captor.getValue
        sent.goodsDescriptionSubCategory must contain("7.1")
        sent.invoiceDate                 must contain(invoiceDate.atStartOfDay())
        sent.supplierAddress1            must contain("L1")
        sent.supplierAddress2            must contain("L2")
        sent.supplierAddress3            must contain("L3")
        sent.supplierVatRegNumber        must contain("VAT123")
        sent.supplierTaxIdentifier       must contain("TAX456")
      }
    }

    "should persist returned updateSequenceNumber replacing previous value" in {
      val savedCaptor: ArgumentCaptor[models.UserAnswers] = ArgumentCaptor.forClass(classOf[models.UserAnswers])

      when(mockSessionRepository.set(savedCaptor.capture())) thenReturn Future.successful(true)

      when(mockEuVatRefundsService.updatePurchase(any())(any())) thenReturn Future.successful(
        models.responses.UpdatePurchaseResponse(updateSequenceNumber = 99)
      )

      val userAnswers = emptyUserAnswers
        .set(PurchaseSubTypePage, "1.2")
        .success
        .value
        .set(AddPurchaseResponsePage, AddPurchaseResponse(itemNumber = 8, updateSequenceNumber = 8))
        .success
        .value
        .set(queries.ClaimApplicationResponseQuery, models.responses.ApplicationResponse(8, "GB008", 8))
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(
          bind[SessionRepository].toInstance(mockSessionRepository)
        )
        .build()

      running(application) {
        val request = FakeRequest(POST, controllers.purchase.routes.CheckYourPurchaseDetailsController.onSubmit().url)
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        val saved = savedCaptor.getValue
        val stored = saved.get(AddPurchaseResponsePage).value
        stored.updateSequenceNumber mustBe 99
        stored.itemNumber mustEqual 8
      }
    }
  }
}
