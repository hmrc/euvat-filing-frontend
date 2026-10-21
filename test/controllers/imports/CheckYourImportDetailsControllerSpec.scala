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
import models.{CheckMode, ImportSupplierAddress, Other, UserAnswers}
import org.jsoup.Jsoup
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{never, verify, when}
import pages.claim.RefundingCountryPage
import pages.imports.*
import play.api.Application
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import utils.ConfigPurchaseOrImportMapping

import java.time.LocalDate
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

    "must not save answers on GET when the missing-answer journey is inactive" in {
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

    "must clear and save the missing-answer journey flag on GET" in {
      val repository = mock[SessionRepository]
      val answers = completeAnswers
        .set(ImportMissingAnswersJourneyPage, true)
        .success
        .value

      val application = buildApplication(Some(answers), repository)

      running(application) {
        val request =
          FakeRequest(GET, routes.CheckYourImportDetailsController.onPageLoad().url)

        val result = route(application, request).value

        status(result) mustBe OK

        val captor = ArgumentCaptor.forClass(classOf[UserAnswers])
        verify(repository).set(captor.capture())

        val savedAnswers = captor.getValue

        savedAnswers.get(ImportMissingAnswersJourneyPage) mustBe None
        savedAnswers.get(ImportSuppliersNamePage) mustBe
          answers.get(ImportSuppliersNamePage)
        savedAnswers.get(ImportTotalVatClaimPage) mustBe
          answers.get(ImportTotalVatClaimPage)
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

    "must save the recovery flag and redirect to the earliest missing page on POST" in {
      val repository = mock[SessionRepository]
      val answers = completeAnswers
        .remove(ImportSuppliersNamePage)
        .flatMap(_.remove(ImportTotalVatClaimPage))
        .success
        .value

      val application = buildApplication(Some(answers), repository)

      running(application) {
        val request =
          FakeRequest(POST, routes.CheckYourImportDetailsController.onSubmit().url)

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe
          routes.ImportSuppliersNameController.onPageLoad(CheckMode).url

        val captor = ArgumentCaptor.forClass(classOf[UserAnswers])
        verify(repository).set(captor.capture())

        captor.getValue.get(ImportMissingAnswersJourneyPage) mustBe Some(true)
        captor.getValue.get(ImportSuppliersNamePage) mustBe None
        captor.getValue.get(ImportTotalVatClaimPage) mustBe None
      }
    }

    "must redirect complete answers to the temporary Journey Recovery destination on POST" in {
      val repository = mock[SessionRepository]
      val answers = completeAnswers
      val application = buildApplication(Some(answers), repository)

      running(application) {
        val request =
          FakeRequest(POST, routes.CheckYourImportDetailsController.onSubmit().url)

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe
          controllers.routes.JourneyRecoveryController.onPageLoad().url

        val captor = ArgumentCaptor.forClass(classOf[UserAnswers])
        verify(repository).set(captor.capture())

        captor.getValue.get(ImportTotalVatClaimPage) mustBe
          answers.get(ImportTotalVatClaimPage)
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
