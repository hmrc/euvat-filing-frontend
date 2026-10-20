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
import models.{CheckMode, NormalMode}
import org.jsoup.Jsoup
import pages.{ImportTotalVatClaimPage, TotalAmountWithoutVatPage, TotalVatPaidImportPage}
import play.api.test.FakeRequest
import play.api.test.Helpers.*

class ImportAmountWarningControllerSpec extends SpecBase {

  private def warningAnswers = emptyUserAnswers
    .set(TotalAmountWithoutVatPage, BigDecimal("100"))
    .flatMap(_.set(TotalVatPaidImportPage, BigDecimal("100")))
    .flatMap(_.set(ImportTotalVatClaimPage, BigDecimal("101")))
    .success.value

  "ImportAmountWarningController" - {

    Seq(NormalMode, CheckMode).foreach { mode =>

      s"VAT paid warning in $mode" - {

        "must render at equality with the import POST action" in {
          val application =
            applicationBuilder(userAnswers = Some(warningAnswers)).build()

          running(application) {
            val result = route(
              application,
              FakeRequest(
                GET,
                routes.ImportAmountWarningController
                  .onVatPaidPageLoad(mode).url
              )
            ).value

            status(result) mustBe OK

            Jsoup.parse(contentAsString(result))
              .select("form")
              .attr("action") mustBe
              routes.ImportAmountWarningController.onVatPaidSubmit(mode).url
          }
        }

        "must render when VAT paid exceeds the amount" in {
          val answers = warningAnswers
            .set(TotalVatPaidImportPage, BigDecimal("101"))
            .success.value

          val application =
            applicationBuilder(userAnswers = Some(answers)).build()

          running(application) {
            val result = route(
              application,
              FakeRequest(
                GET,
                routes.ImportAmountWarningController
                  .onVatPaidPageLoad(mode).url
              )
            ).value

            status(result) mustBe OK
          }
        }

        "must continue to import VAT claim without repeating the warning" in {
          val application =
            applicationBuilder(userAnswers = Some(warningAnswers)).build()

          running(application) {
            val result = route(
              application,
              FakeRequest(
                POST,
                routes.ImportAmountWarningController.onVatPaidSubmit(mode).url
              )
            ).value

            status(result) mustBe SEE_OTHER
            redirectLocation(result).value mustBe
              routes.TotalVatClaimController.onPageLoad(mode).url
          }
        }

        "must return to VAT paid when no warning is applicable" in {
          val answers = warningAnswers
            .set(TotalVatPaidImportPage, BigDecimal("99"))
            .success.value

          val application =
            applicationBuilder(userAnswers = Some(answers)).build()

          running(application) {
            val result = route(
              application,
              FakeRequest(
                GET,
                routes.ImportAmountWarningController
                  .onVatPaidPageLoad(mode).url
              )
            ).value

            status(result) mustBe SEE_OTHER
            redirectLocation(result).value mustBe
              routes.TotalVatPaidImportController.onPageLoad(mode).url
          }
        }
      }

      s"VAT claim warning in $mode" - {

        "must render when VAT claim exceeds VAT paid with the import POST action" in {
          val application =
            applicationBuilder(userAnswers = Some(warningAnswers)).build()

          running(application) {
            val result = route(
              application,
              FakeRequest(
                GET,
                routes.ImportAmountWarningController
                  .onVatClaimPageLoad(mode).url
              )
            ).value

            status(result) mustBe OK

            Jsoup.parse(contentAsString(result))
              .select("form")
              .attr("action") mustBe
              routes.ImportAmountWarningController.onVatClaimSubmit(mode).url
          }
        }

        "must continue to Import CYA" in {
          val application =
            applicationBuilder(userAnswers = Some(warningAnswers)).build()

          running(application) {
            val result = route(
              application,
              FakeRequest(
                POST,
                routes.ImportAmountWarningController.onVatClaimSubmit(mode).url
              )
            ).value

            status(result) mustBe SEE_OTHER
            redirectLocation(result).value mustBe
              routes.CheckYourImportDetailsController.onPageLoad().url
          }
        }

        "must not display a warning when VAT claim equals VAT paid" in {
          val answers = warningAnswers
            .set(ImportTotalVatClaimPage, BigDecimal("100"))
            .success.value

          val application =
            applicationBuilder(userAnswers = Some(answers)).build()

          running(application) {
            val result = route(
              application,
              FakeRequest(
                GET,
                routes.ImportAmountWarningController
                  .onVatClaimPageLoad(mode).url
              )
            ).value

            status(result) mustBe SEE_OTHER
            redirectLocation(result).value mustBe
              routes.TotalVatClaimController.onPageLoad(mode).url
          }
        }

        "must recover when no session answers exist" in {
          val application =
            applicationBuilder(userAnswers = None).build()

          running(application) {
            val result = route(
              application,
              FakeRequest(
                GET,
                routes.ImportAmountWarningController
                  .onVatClaimPageLoad(mode).url
              )
            ).value

            status(result) mustBe SEE_OTHER
            redirectLocation(result).value mustBe
              controllers.routes.JourneyRecoveryController.onPageLoad().url
          }
        }
      }
    }
  }
}