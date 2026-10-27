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
import models.requests.UpdateImportRequest
import models.responses.{AddImportResponse, ApplicationResponse, UpdateImportResponse}
import models.{CheckMode, ImportSupplierAddress, Other, UserAnswers}
import org.jsoup.Jsoup
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{never, times, verify, when}
import pages.AddImportResponsePage
import pages.claim.RefundingCountryPage
import pages.imports.*
import play.api.Application
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.{ClaimApplicationResponseQuery, UpdateSequenceNumberQuery}
import repositories.SessionRepository
import utils.ConfigPurchaseOrImportMapping

import java.time.{LocalDate, LocalDateTime}
import scala.concurrent.Future

class CheckYourImportDetailsControllerSpec extends SpecBase {

  private def completeAnswers: UserAnswers =
    emptyUserAnswers
      .set(RefundingCountryPage, "AT")
      .flatMap(_.set(ImportTypePage, Other))
      .flatMap(_.set(DescribeItemsOnImportDocPage, "Imported equipment"))
      .flatMap(_.set(SadReferenceCheckPage, true))
      .flatMap(_.set(SadReferenceNumberPage, "SAD123"))
      .flatMap(_.set(ImportDatePage, LocalDate.of(2026, 10, 1)))
      .flatMap(_.set(ImportSuppliersNamePage, "Example Supplier"))
      .flatMap(
        _.set(
          ImportSupplierAddressPage,
          ImportSupplierAddress("10 Street", None, None, "IN")
        )
      )
      .flatMap(_.set(TotalAmountWithoutVatPage, BigDecimal(100)))
      .flatMap(_.set(TotalVatPaidImportPage, BigDecimal(20)))
      .flatMap(_.set(ImportTotalVatClaimPage, BigDecimal(10)))
      .success
      .value

  // complete answers for an import that add-import has already created (itemNumber 4, sequence number 2)
  private def savedAnswers: UserAnswers =
    completeAnswers
      .set(ClaimApplicationResponseQuery, ApplicationResponse(404, "GB001", 1))
      .flatMap(_.set(AddImportResponsePage, AddImportResponse(itemNumber = 4, updateSequenceNumber = 2)))
      .success
      .value

  private def buildApplication(
                                answers: Option[UserAnswers],
                                repository: SessionRepository
                              ): Application = {
    when(repository.set(any[UserAnswers]()))
      .thenReturn(Future.successful(true))

    applicationBuilder(userAnswers = answers)
      .overrides(
        bind[SessionRepository].toInstance(repository),
        bind[ConfigPurchaseOrImportMapping]
          .toInstance(new ConfigPurchaseOrImportMapping())
      )
      .build()
  }

  "CheckYourImportDetailsController" - {

    "must return OK and render the saved answers for a GET" in {
      val repository = mock[SessionRepository]
      val application = buildApplication(Some(completeAnswers), repository)

      running(application) {
        val request =
          FakeRequest(GET, routes.CheckYourImportDetailsController.onPageLoad().url)

        val result = route(application, request).value

        status(result) mustBe OK

        val document = Jsoup.parse(contentAsString(result))

        document.select("h1").text() mustBe
          messages(application)("checkYourImportDetails.heading")

        val values = document.select(".govuk-summary-list__value").text()

        values must include("Example Supplier")
        values must include("SAD123")
        values must include("Imported equipment")

        document.select("form").attr("action") mustBe
          routes.CheckYourImportDetailsController.onSubmit().url

        document.select("a.govuk-back-link").attr("href") mustBe
          routes.TotalVatClaimController.onPageLoad(models.NormalMode).url
      }
    }

    "must not save answers on GET" in {
      val repository = mock[SessionRepository]
      val application = buildApplication(Some(completeAnswers), repository)

      running(application) {
        val request =
          FakeRequest(GET, routes.CheckYourImportDetailsController.onPageLoad().url)

        val result = route(application, request).value

        status(result) mustBe OK

        verify(repository, never()).set(any[UserAnswers]())
      }
    }

    "must display Not provided and an Add action for a missing supplier name" in {
      val repository = mock[SessionRepository]
      val answers = completeAnswers
        .remove(ImportSuppliersNamePage)
        .success
        .value

      val application = buildApplication(Some(answers), repository)

      running(application) {
        val request =
          FakeRequest(GET, routes.CheckYourImportDetailsController.onPageLoad().url)

        val result = route(application, request).value

        status(result) mustBe OK

        val document = Jsoup.parse(contentAsString(result))
        val supplierUrl =
          routes.ImportSuppliersNameController.onPageLoad(CheckMode).url

        val matchingRows = document
          .select(".govuk-summary-list__row")
          .iterator()

        var supplierRowFound = false

        while (matchingRows.hasNext) {
          val row = matchingRows.next()
          val links = row.select(".govuk-summary-list__actions a")

          if (!links.isEmpty && links.first().attr("href") == supplierUrl) {
            supplierRowFound = true

            row.select(".govuk-summary-list__value").text() mustBe
              "Not provided"

            val actionLink = links.first().clone()
            actionLink.select(".govuk-visually-hidden").remove()

            actionLink.text() mustBe
              messages(application)("site.add")
          }
        }
        supplierRowFound mustBe true
      }
    }

    "must redirect to Journey Recovery on POST when add-import has not created the item" in {
      val repository = mock[SessionRepository]
      val application = buildApplication(Some(completeAnswers), repository)

      running(application) {
        val result = route(application, FakeRequest(POST, routes.CheckYourImportDetailsController.onSubmit().url)).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe controllers.routes.JourneyRecoveryController.onPageLoad().url

        verify(mockEuVatRefundsService, never()).updateImport(any())(any())
        verify(repository, never()).set(any[UserAnswers]())
      }
    }

    "must update the import and redirect to the purchases and imports summary on POST when everything is answered" in {
      val repository = mock[SessionRepository]
      when(mockEuVatRefundsService.updateImport(any())(any()))
        .thenReturn(Future.successful(UpdateImportResponse(updateSequenceNumber = 3)))
      val application = buildApplication(Some(savedAnswers), repository)

      running(application) {
        val result = route(application, FakeRequest(POST, routes.CheckYourImportDetailsController.onSubmit().url)).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe controllers.routes.PurchaseImportSummaryController.onPageLoad.url

        val requestCaptor = ArgumentCaptor.forClass(classOf[UpdateImportRequest])
        verify(mockEuVatRefundsService).updateImport(requestCaptor.capture())(any())
        requestCaptor.getValue mustBe UpdateImportRequest(
          applicationId               = 404,
          itemNumber                  = 4,
          goodsDescriptionCategory    = "10",
          goodsDescriptionSubCategory = None,
          goodsDescriptionText        = Some("Imported equipment"),
          hasSadReferenceNumber       = Some(true),
          sadReferenceNumber          = Some("SAD123"),
          referenceInformation        = None,
          supplierName                = Some("Example Supplier"),
          supplierAddressLine1        = Some("10 Street"),
          supplierAddressLine2        = None,
          supplierAddressLine3        = None,
          supplierCountryCode         = Some("IN"),
          issuingDate                 = Some(LocalDateTime.of(2026, 10, 1, 0, 0)),
          currencyCode                = Some("EUR"),
          taxableAmount               = Some(BigDecimal(100)),
          vatAmount                   = Some(BigDecimal(20)),
          deductibleVatAmount         = Some(BigDecimal(10)),
          updateSequenceNumber        = 2
        )

        val answersCaptor = ArgumentCaptor.forClass(classOf[UserAnswers])
        verify(repository, times(1)).set(answersCaptor.capture())
        val saved = answersCaptor.getValue
        saved.get(AddImportResponsePage) mustBe Some(AddImportResponse(itemNumber = 4, updateSequenceNumber = 3))
        saved.get(UpdateSequenceNumberQuery) mustBe Some(3)
      }
    }

    "must prefer the latest application sequence number over the one returned by add-import" in {
      val repository = mock[SessionRepository]
      when(mockEuVatRefundsService.updateImport(any())(any())).thenReturn(Future.successful(UpdateImportResponse(8)))
      val answers = savedAnswers.set(UpdateSequenceNumberQuery, 7).success.value
      val application = buildApplication(Some(answers), repository)

      running(application) {
        status(route(application, FakeRequest(POST, routes.CheckYourImportDetailsController.onSubmit().url)).value) mustBe SEE_OTHER

        val requestCaptor = ArgumentCaptor.forClass(classOf[UpdateImportRequest])
        verify(mockEuVatRefundsService).updateImport(requestCaptor.capture())(any())
        requestCaptor.getValue.updateSequenceNumber mustBe 7
      }
    }

    "must send only the document information when there is no SAD reference" in {
      val repository = mock[SessionRepository]
      when(mockEuVatRefundsService.updateImport(any())(any())).thenReturn(Future.successful(UpdateImportResponse(3)))
      // SadReferenceNumberPage still holds the earlier "Yes" answer and must not be sent
      val answers = savedAnswers
        .set(SadReferenceCheckPage, false)
        .flatMap(_.set(ImportDetailsInfoPage, "Document 123"))
        .success
        .value
      val application = buildApplication(Some(answers), repository)

      running(application) {
        status(route(application, FakeRequest(POST, routes.CheckYourImportDetailsController.onSubmit().url)).value) mustBe SEE_OTHER

        val requestCaptor = ArgumentCaptor.forClass(classOf[UpdateImportRequest])
        verify(mockEuVatRefundsService).updateImport(requestCaptor.capture())(any())
        requestCaptor.getValue.hasSadReferenceNumber mustBe Some(false)
        requestCaptor.getValue.sadReferenceNumber mustBe None
        requestCaptor.getValue.referenceInformation mustBe Some("Document 123")
      }
    }

    "must send the selected currency when one has been chosen" in {
      val repository = mock[SessionRepository]
      when(mockEuVatRefundsService.updateImport(any())(any())).thenReturn(Future.successful(UpdateImportResponse(3)))
      val answers = savedAnswers.set(ImportCurrencyPage, "SEK").success.value
      val application = buildApplication(Some(answers), repository)

      running(application) {
        status(route(application, FakeRequest(POST, routes.CheckYourImportDetailsController.onSubmit().url)).value) mustBe SEE_OTHER

        val requestCaptor = ArgumentCaptor.forClass(classOf[UpdateImportRequest])
        verify(mockEuVatRefundsService).updateImport(requestCaptor.capture())(any())
        requestCaptor.getValue.currencyCode mustBe Some("SEK")
      }
    }

    "must redirect to Journey Recovery and keep the answers when the backend call fails" in {
      val repository = mock[SessionRepository]
      when(mockEuVatRefundsService.updateImport(any())(any())).thenReturn(Future.failed(new RuntimeException("boom")))
      val application = buildApplication(Some(savedAnswers), repository)

      running(application) {
        val result = route(application, FakeRequest(POST, routes.CheckYourImportDetailsController.onSubmit().url)).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe controllers.routes.JourneyRecoveryController.onPageLoad().url

        // nothing is saved when the backend call fails, so the sequence number and item response are untouched
        verify(repository, never()).set(any[UserAnswers]())
      }
    }

    "must redirect to Journey Recovery without calling the backend when no import has been added" in {
      val repository = mock[SessionRepository]
      val application = buildApplication(Some(completeAnswers), repository)

      running(application) {
        val result = route(application, FakeRequest(POST, routes.CheckYourImportDetailsController.onSubmit().url)).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe controllers.routes.JourneyRecoveryController.onPageLoad().url
        verify(mockEuVatRefundsService, never()).updateImport(any())(any())
      }
    }

    Seq(GET, POST).foreach { method =>
      s"must redirect to Journey Recovery for a $method without session data" in {
        val repository = mock[SessionRepository]
        val application = buildApplication(None, repository)

        running(application) {
          val url =
            if (method == GET) {
              routes.CheckYourImportDetailsController.onPageLoad().url
            } else {
              routes.CheckYourImportDetailsController.onSubmit().url
            }

          val result = route(application, FakeRequest(method, url)).value

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe
            controllers.routes.JourneyRecoveryController.onPageLoad().url

          verify(repository, never()).set(any[UserAnswers]())
        }
      }
    }
  }
}
