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

package viewmodels.checkAnswers

import base.SpecBase
import play.api.test.Helpers.stubMessages
import play.api.i18n.Messages
import java.time.LocalDate
import pages.InvoiceDatePage

class InvoiceDateSummarySpec extends SpecBase {

  private implicit val messages: Messages = stubMessages()

  "InvoiceDateSummary" - {

    "must return a SummaryListRow when InvoiceDatePage is present" in {
      val date = LocalDate.of(2025, 4, 15)
      val userAnswers = emptyUserAnswers.set(InvoiceDatePage, date).success.value

      val row = InvoiceDateSummary.row(userAnswers).value

      row.key.content.asHtml.toString must include("invoiceDate.checkYourAnswersLabel")
      row.value.content.asHtml.toString must include("15 April 2025")
      row.actions.get.items.head.href mustEqual controllers.routes.InvoiceDateController.onPageLoad(models.CheckMode).url
    }

    "must return None when InvoiceDatePage is not present" in {
      InvoiceDateSummary.row(emptyUserAnswers) mustBe None
    }
  }
}
