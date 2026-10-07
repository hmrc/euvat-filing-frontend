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
import models.{PurchaseImport, PurchaseOrImportType, UserAnswers}
import play.api.i18n.Messages
import play.twirl.api.Html
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.{HtmlContent, Text}
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{SummaryListRow, Value}
import utils.{ControllerHelpers, Currency}
import viewmodels.govuk.summarylist.*
import viewmodels.implicits.*

object PurchaseImportListSummary {

  def rows(userAnswers: UserAnswers, list: List[PurchaseImport], config: Map[String, Seq[Currency]])(implicit
    messages: Messages,
    request: DataRequest[?]
  ): Seq[SummaryListRow] = {
    val (completeClaims, incompleteClaims) = list.partition(_.deductibleVatAmount > 0) // split between incomplete and complete items
    (incompleteClaims ++ completeClaims) // display incomplete first and the completed ones
      .map { item =>
        val itemNumber = item.itemNumber
        val vatClaim = item.deductibleVatAmount
        val category = PurchaseOrImportType.codeToType.getOrElse(item.goodsDescriptionCategory, "Unknown")

        val itemType = if (item.itemType == "P") { "Purchase" }
        else { "Import" }

        if (vatClaim == 0) {
          displaySummaryListRow(itemType, category)
        } else {
          val currencySymbol = ControllerHelpers.currencySymbolFromSession(userAnswers, config)
          displaySummaryListRow(itemType, category, currencySymbol, vatClaim)
        }
      }
  }

  private def displaySummaryListRow(itemType: String, category: String, currencySymbol: String = "", vatClaim: BigDecimal = 0)(implicit
    messages: Messages
  ): SummaryListRow = {
    SummaryListRowViewModel(
      key = itemType + " (" + category + ")",
      value = if (vatClaim == 0) {
        Value(content = HtmlContent(Html("<strong class='govuk-tag'> Incomplete</strong>")))
      } else {
        Value(content = Text(s"$currencySymbol" + vatClaim.toString() + " VAT claim"))
      },
      actions = if (vatClaim == 0) {
        Seq(
          ActionItemViewModel("site.add", "#").withVisuallyHiddenText(
            messages("purchaseImportSummary.incomplete.add.hidden", itemType, category)
          ),
          ActionItemViewModel("site.remove", "#").withVisuallyHiddenText(
            messages("purchaseImportSummary.incomplete.remove.hidden", itemType, category)
          )
        )
      } else {
        Seq(
          ActionItemViewModel("site.change", "#").withVisuallyHiddenText(
            messages("purchaseImportSummary.complete.change.hidden", itemType, category, currencySymbol + vatClaim)
          ),
          ActionItemViewModel("site.remove", "#").withVisuallyHiddenText(
            messages("purchaseImportSummary.complete.remove.hidden", itemType, category, currencySymbol + vatClaim)
          )
        )
      }
    )
  }
}
