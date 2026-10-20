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
import config.FrontendAppConfig
import models.NormalMode
import org.jsoup.Jsoup
import pages.ImportSuppliersNamePage
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import utils.{ConfigPurchaseOrImportMapping, CurrencyConfig}
import viewmodels.checkAnswers.CheckYourImportDetailsSummary
import views.html.imports.CheckYourImportDetailsView

class CheckYourImportDetailsControllerSpec extends SpecBase {

  private def getUrl: String =
    routes.CheckYourImportDetailsController.onPageLoad().url

  "CheckYourImportDetailsController" - {

    "must return OK and render the summary using the stored answers" in {
      val answers = emptyUserAnswers
        .set(ImportSuppliersNamePage, "Example Supplier Ltd")
        .success
        .value

      val application =
        applicationBuilder(userAnswers = Some(answers)).build()

      running(application) {
        val request = FakeRequest(GET, getUrl)
        val result = route(application, request).value

        val view =
          application.injector.instanceOf[CheckYourImportDetailsView]

        val config =
          application.injector.instanceOf[ConfigPurchaseOrImportMapping]

        val currencyConfig =
          application.injector.instanceOf[CurrencyConfig]

        val appConfig =
          application.injector.instanceOf[FrontendAppConfig]

        val sections = CheckYourImportDetailsSummary.sections(
          answers,
          config,
          currencyConfig,
          appConfig.supplierCountries
        )(messages(application))

        status(result) mustBe OK

        contentAsString(result) mustBe view(
          sections,
          Call(POST, getUrl),
          routes.TotalVatClaimController.onPageLoad(NormalMode)
        )(request, messages(application)).toString
      }
    }

    "must render incomplete answers with Not provided and Add actions" in {
      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val result =
          route(application, FakeRequest(GET, getUrl)).value

        status(result) mustBe OK

        val document = Jsoup.parse(contentAsString(result))
        val rows = document.select(".govuk-summary-list__row")

        rows.isEmpty mustBe false

        (0 until rows.size()).foreach { index =>
          val row = rows.get(index)

          row.select(".govuk-summary-list__value").text() mustBe
            messages(application)("site.notProvided")

          row.select(
            ".govuk-summary-list__actions [aria-hidden=true]"
          ).text() mustBe messages(application)("site.add")
        }
      }
    }

    "must link back to the import VAT claim page" in {
      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val result =
          route(application, FakeRequest(GET, getUrl)).value

        val document = Jsoup.parse(contentAsString(result))

        document.select("a.govuk-back-link").attr("href") mustBe
          routes.TotalVatClaimController.onPageLoad(NormalMode).url
      }
    }

    "must redirect to Journey Recovery when no user answers exist" in {
      val application =
        applicationBuilder(userAnswers = None).build()

      running(application) {
        val result =
          route(application, FakeRequest(GET, getUrl)).value

        status(result) mustBe SEE_OTHER

        redirectLocation(result).value mustBe
          controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
