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
import models.{CheckMode, Fuel, ImportSupplierAddress, Other, UserAnswers}
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.verify
import pages.claim.RefundingCountryPage
import pages.imports.*
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import utils.ConfigPurchaseOrImportMapping
import java.time.LocalDate

class CheckYourImportDetailsJourneySpec extends SpecBase {
  private def complete: UserAnswers = emptyUserAnswers
    .set(RefundingCountryPage, "AT")
    .flatMap(_.set(ImportTypePage, Other))
    .flatMap(_.set(DescribeItemsOnImportDocPage, "Imported equipment"))
    .flatMap(_.set(SadReferenceCheckPage, true))
    .flatMap(_.set(SadReferenceNumberPage, "SAD123"))
    .flatMap(_.set(ImportDatePage, LocalDate.of(2026, 10, 1)))
    .flatMap(_.set(ImportSuppliersNamePage, "Example Supplier"))
    .flatMap(_.set(ImportSupplierAddressPage, ImportSupplierAddress("10 Street", None, None, "IN")))
    .flatMap(_.set(TotalAmountWithoutVatPage, BigDecimal(100)))
    .flatMap(_.set(TotalVatPaidImportPage, BigDecimal(20)))
    .flatMap(_.set(ImportTotalVatClaimPage, BigDecimal(10))).success.value

  private def submit(answers: UserAnswers, action: => Call, body: Seq[(String, String)], expected: => Call)(check: UserAnswers => Unit): Unit = {
    val app = applicationBuilder(userAnswers = Some(answers)).overrides(
      bind[SessionRepository].toInstance(mockSessionRepository),
      bind[ConfigPurchaseOrImportMapping].toInstance(new ConfigPurchaseOrImportMapping())
    ).build()
    running(app) {
      val result = route(app, FakeRequest(POST, action.url).withFormUrlEncodedBody(body*)).value
      status(result) mustBe SEE_OTHER
      redirectLocation(result).value mustBe expected.url
      val captor = ArgumentCaptor.forClass(classOf[UserAnswers])
      verify(mockSessionRepository).set(captor.capture())
      check(captor.getValue)
    }
  }

  "Import CYA journey" - {
    "must send complete answers to the temporary destination" in {
      submit(complete, routes.CheckYourImportDetailsController.onSubmit(), Seq.empty,
        controllers.routes.JourneyRecoveryController.onPageLoad()) { saved =>
        saved.get(ImportTotalVatClaimPage) mustBe Some(BigDecimal(10))
      }
    }
    "must start missing-answer recovery at the earliest missing page" in {
      val answers = complete.remove(ImportSuppliersNamePage).flatMap(_.remove(ImportTotalVatClaimPage)).success.value
      submit(answers, routes.CheckYourImportDetailsController.onSubmit(), Seq.empty,
        routes.ImportSuppliersNameController.onPageLoad(CheckMode)) { saved =>
        saved.get(ImportMissingAnswersJourneyPage) mustBe Some(true)
      }
    }
    "must skip populated pages during recovery" in {
      val answers = complete.remove(ImportSuppliersNamePage).flatMap(_.remove(ImportTotalVatClaimPage))
        .flatMap(_.set(ImportMissingAnswersJourneyPage, true)).success.value
      submit(answers, routes.ImportSuppliersNameController.onSubmit(CheckMode), Seq("value" -> "New Supplier"),
        routes.TotalVatClaimController.onPageLoad(CheckMode)) { saved =>
        saved.get(ImportSuppliersNamePage) mustBe Some("New Supplier")
      }
    }
    "must return an unchanged amount to CYA and overwrite its stale flag" in {
      val answers = complete.set(ImportAmountWithoutVatChangedPage, true).success.value
      submit(answers, routes.TotalAmountWithoutVatController.onSubmit(CheckMode), Seq("value" -> "100"),
        routes.CheckYourImportDetailsController.onPageLoad()) { saved =>
        saved.get(ImportAmountWithoutVatChangedPage) mustBe Some(false)
        saved.get(TotalVatPaidImportPage) mustBe Some(BigDecimal(20))
      }
    }
    "must clear dependent amounts and continue after changing amount without VAT" in {
      submit(complete, routes.TotalAmountWithoutVatController.onSubmit(CheckMode), Seq("value" -> "200"),
        routes.TotalVatPaidImportController.onPageLoad(CheckMode)) { saved =>
        saved.get(ImportAmountWithoutVatChangedPage) mustBe Some(true)
        saved.get(TotalVatPaidImportPage) mustBe None
        saved.get(ImportTotalVatClaimPage) mustBe None
      }
    }
    "must continue changed VAT paid to claim" in {
      submit(complete, routes.TotalVatPaidImportController.onSubmit(CheckMode), Seq("value" -> "100"),
        routes.TotalVatClaimController.onPageLoad(CheckMode)) { saved =>
        saved.get(ImportVatPaidChangedPage) mustBe Some(true)
        saved.get(ImportTotalVatClaimPage) mustBe None
      }
    }
    "must return unchanged VAT paid to CYA" in {
      submit(complete, routes.TotalVatPaidImportController.onSubmit(CheckMode), Seq("value" -> "20"),
        routes.CheckYourImportDetailsController.onPageLoad()) { saved =>
        saved.get(ImportVatPaidChangedPage) mustBe Some(false)
      }
    }
    "must return a changed VAT claim to CYA" in {
      submit(complete, routes.TotalVatClaimController.onSubmit(CheckMode), Seq("value" -> "30"),
        routes.CheckYourImportDetailsController.onPageLoad()) { saved =>
        saved.get(ImportTotalVatClaimPage) mustBe Some(BigDecimal(30))
      }
    }
    "must route an unchanged import type to CYA and preserve its description" in {
      submit(complete, routes.ImportTypeController.onSubmit(CheckMode), Seq("value" -> Other.toString),
        routes.CheckYourImportDetailsController.onPageLoad()) { saved =>
        saved.get(ImportTypeChangedPage) mustBe Some(false)
        saved.get(DescribeItemsOnImportDocPage) mustBe Some("Imported equipment")
      }
    }
    "must clear old category answers when import type changes" in {
      submit(complete, routes.ImportTypeController.onSubmit(CheckMode), Seq("value" -> Fuel.toString),
        routes.CheckYourImportDetailsController.onPageLoad()) { saved =>
        saved.get(ImportTypeChangedPage) mustBe Some(true)
        saved.get(DescribeItemsOnImportDocPage) mustBe None
      }
    }
    "must route unchanged SAD to CYA and preserve its reference" in {
      submit(complete, routes.SadReferenceCheckController.onSubmit(CheckMode), Seq("value" -> "true"),
        routes.CheckYourImportDetailsController.onPageLoad()) { saved =>
        saved.get(ImportSadChangedPage) mustBe Some(false)
        saved.get(SadReferenceNumberPage) mustBe Some("SAD123")
      }
    }
    "must clear the obsolete reference when SAD changes to No" in {
      submit(complete, routes.SadReferenceCheckController.onSubmit(CheckMode), Seq("value" -> "false"),
        routes.ImportDetailsInfoController.onPageLoad(CheckMode)) { saved =>
        saved.get(ImportSadChangedPage) mustBe Some(true)
        saved.get(SadReferenceNumberPage) mustBe None
      }
    }
    "must return changed SAD reference directly to CYA" in {
      submit(complete, routes.SadReferenceNumberController.onSubmit(CheckMode), Seq("value" -> "SAD456"),
        routes.CheckYourImportDetailsController.onPageLoad()) { saved =>
        saved.get(SadReferenceNumberPage) mustBe Some("SAD456")
      }
    }
    "must reset recovery when CYA is revisited" in {
      val answers = complete.set(ImportMissingAnswersJourneyPage, true).success.value
      val app = applicationBuilder(userAnswers = Some(answers)).overrides(
        bind[SessionRepository].toInstance(mockSessionRepository)
      ).build()
      running(app) {
        val result = route(app, FakeRequest(GET, routes.CheckYourImportDetailsController.onPageLoad().url)).value
        status(result) mustBe OK
        val captor = ArgumentCaptor.forClass(classOf[UserAnswers])
        verify(mockSessionRepository).set(captor.capture())
        captor.getValue.get(ImportMissingAnswersJourneyPage) mustBe None
        contentAsString(result) must include("Check your answers")
      }
    }
  }
}
