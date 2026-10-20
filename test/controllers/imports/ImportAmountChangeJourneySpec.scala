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
import models.{CheckMode, NormalMode, UserAnswers}
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.verify
import pages.*
import pages.imports.*
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository

class ImportAmountChangeJourneySpec extends SpecBase {

  private def baseAnswers: UserAnswers =
    emptyUserAnswers
      .set(TotalAmountWithoutVatPage, BigDecimal("100"))
      .flatMap(_.set(TotalVatPaidImportPage, BigDecimal("20")))
      .flatMap(_.set(ImportTotalVatClaimPage, BigDecimal("10")))
      .success.value

  private def submit(
                      answers: UserAnswers,
                      action: Call,
                      value: String,
                      expected: Call
                    )(checkSaved: UserAnswers => Unit): Unit = {
    val application = applicationBuilder(userAnswers = Some(answers))
      .overrides(
        bind[SessionRepository].toInstance(mockSessionRepository)
      )
      .build()

    running(application) {
      val request = FakeRequest(POST, action.url)
        .withFormUrlEncodedBody("value" -> value)

      val result = route(application, request).value

      status(result) mustBe SEE_OTHER
      redirectLocation(result).value mustBe expected.url

      val captor = ArgumentCaptor.forClass(classOf[UserAnswers])
      verify(mockSessionRepository).set(captor.capture())
      checkSaved(captor.getValue)
    }
  }

  "Import amount change journey" - {

    "must return unchanged amount to CYA and reset a stale change flag" in {
      val answers = baseAnswers
        .set(ImportAmountWithoutVatChangedPage, true)
        .success.value

      submit(
        answers,
        routes.TotalAmountWithoutVatController.onSubmit(CheckMode),
        "100",
        routes.CheckYourImportDetailsController.onPageLoad()
      ) { saved =>
        saved.get(ImportAmountWithoutVatChangedPage) mustBe Some(false)
        saved.get(TotalVatPaidImportPage) mustBe Some(BigDecimal("20"))
        saved.get(ImportTotalVatClaimPage) mustBe Some(BigDecimal("10"))
      }
    }

    "must route changed amount to VAT paid and clear dependent amounts" in {
      submit(
        baseAnswers,
        routes.TotalAmountWithoutVatController.onSubmit(CheckMode),
        "200",
        routes.TotalVatPaidImportController.onPageLoad(CheckMode)
      ) { saved =>
        saved.get(TotalAmountWithoutVatPage) mustBe Some(BigDecimal("200"))
        saved.get(ImportAmountWithoutVatChangedPage) mustBe Some(true)
        saved.get(TotalVatPaidImportPage) mustBe None
        saved.get(ImportTotalVatClaimPage) mustBe None
      }
    }

    "must return unchanged VAT paid to CYA and preserve VAT claim" in {
      val answers = baseAnswers
        .set(ImportVatPaidChangedPage, true)
        .success.value

      submit(
        answers,
        routes.TotalVatPaidImportController.onSubmit(CheckMode),
        "20",
        routes.CheckYourImportDetailsController.onPageLoad()
      ) { saved =>
        saved.get(ImportVatPaidChangedPage) mustBe Some(false)
        saved.get(ImportTotalVatClaimPage) mustBe Some(BigDecimal("10"))
      }
    }

    "must route changed VAT paid below the amount to VAT claim" in {
      submit(
        baseAnswers,
        routes.TotalVatPaidImportController.onSubmit(CheckMode),
        "30",
        routes.TotalVatClaimController.onPageLoad(CheckMode)
      ) { saved =>
        saved.get(ImportVatPaidChangedPage) mustBe Some(true)
        saved.get(ImportTotalVatClaimPage) mustBe None
      }
    }

    Seq("100", "101").foreach { paid =>
      s"must warn when changed VAT paid is $paid against an amount of 100" in {
        submit(
          baseAnswers,
          routes.TotalVatPaidImportController.onSubmit(CheckMode),
          paid,
          routes.ImportAmountWarningController.onVatPaidPageLoad(CheckMode)
        ) { saved =>
          saved.get(ImportVatPaidChangedPage) mustBe Some(true)
          saved.get(TotalVatPaidImportPage) mustBe Some(BigDecimal(paid))
        }
      }
    }

    "must return unchanged VAT paid directly to CYA even at the warning threshold" in {
      val answers = baseAnswers
        .set(TotalVatPaidImportPage, BigDecimal("100"))
        .success.value

      submit(
        answers,
        routes.TotalVatPaidImportController.onSubmit(CheckMode),
        "100",
        routes.CheckYourImportDetailsController.onPageLoad()
      ) { saved =>
        saved.get(ImportVatPaidChangedPage) mustBe Some(false)
      }
    }

    "must return unchanged VAT claim directly to CYA and reset its flag" in {
      val answers = baseAnswers
        .set(ImportVatClaimChangedPage, true)
        .success.value

      submit(
        answers,
        routes.TotalVatClaimController.onSubmit(CheckMode),
        "10",
        routes.CheckYourImportDetailsController.onPageLoad()
      ) { saved =>
        saved.get(ImportVatClaimChangedPage) mustBe Some(false)
      }
    }

    Seq("15", "20").foreach { claim =>
      s"must return changed VAT claim $claim to CYA when VAT paid is 20" in {
        submit(
          baseAnswers,
          routes.TotalVatClaimController.onSubmit(CheckMode),
          claim,
          routes.CheckYourImportDetailsController.onPageLoad()
        ) { saved =>
          saved.get(ImportVatClaimChangedPage) mustBe Some(true)
        }
      }
    }

    "must warn when changed VAT claim exceeds VAT paid" in {
      submit(
        baseAnswers,
        routes.TotalVatClaimController.onSubmit(CheckMode),
        "21",
        routes.ImportAmountWarningController.onVatClaimPageLoad(CheckMode)
      ) { saved =>
        saved.get(ImportVatClaimChangedPage) mustBe Some(true)
      }
    }

    "must return an unchanged excessive VAT claim directly to CYA" in {
      val answers = baseAnswers
        .set(ImportTotalVatClaimPage, BigDecimal("21"))
        .success.value

      submit(
        answers,
        routes.TotalVatClaimController.onSubmit(CheckMode),
        "21",
        routes.CheckYourImportDetailsController.onPageLoad()
      ) { saved =>
        saved.get(ImportVatClaimChangedPage) mustBe Some(false)
      }
    }

    "must continue the initial journey for unchanged amount in NormalMode" in {
      submit(
        baseAnswers,
        routes.TotalAmountWithoutVatController.onSubmit(NormalMode),
        "100",
        routes.TotalVatPaidImportController.onPageLoad(NormalMode)
      ) { saved =>
        saved.get(ImportAmountWithoutVatChangedPage) mustBe Some(false)
      }
    }
  }
}