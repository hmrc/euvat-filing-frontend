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

import models.requests.DataRequest
import models.{PurchaseImport, UserAnswers}
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{SummaryListRow, Value}
import utils.{ControllerHelpers, Currency}
import viewmodels.govuk.summarylist.*
import viewmodels.implicits.*

object PurchaseImportListSummary {

  def rows(userAnswers: UserAnswers, list: List[PurchaseImport], config: Map[String, Seq[Currency]])(implicit
    messages: Messages,
    request: DataRequest[?]
  ): Seq[SummaryListRow] = {
    list.map { item =>
      val itemNumber = item.itemNumber
      val vatClaim = item.deductibleVatAmount
      val itemType = if (item.itemType == "P") {
        "Purchase"
      } else {
        "Import"
      }

      val currencySymbol = ControllerHelpers.currencySymbolFromSession(userAnswers, config)
      SummaryListRowViewModel(
        key   = itemType,
        value = Value(content = Text(s"$currencySymbol" + item.deductibleVatAmount.toString() + " VAT claim")),
        actions = Seq(
          ActionItemViewModel("site.change", "#").withVisuallyHiddenText(
            messages("purchaseImportSummary.change.hidden", itemType, currencySymbol + vatClaim)
          ),
          ActionItemViewModel("site.remove", "#").withVisuallyHiddenText(
            messages("purchaseImportSummary.remove.hidden", itemType, currencySymbol + vatClaim)
          )
        )
      )
    }
  }

}
