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

import controllers.routes
import models.{CheckMode, UserAnswers}
import pages.*
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist.*
import viewmodels.implicits.*

object CheckYourClaimsSummary {

  def rowCountryLabel(answers: UserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(RefundingCountryPage).map { answer =>

      SummaryListRowViewModel(
        key   = "checkYourClaimDetails.refundingCountry.label",
        value = ValueViewModel(""),
        actions = Seq(
          ActionItemViewModel("site.change", routes.RefundingCountryController.onPageLoad(CheckMode).url)
            .withVisuallyHiddenText(messages("checkYourClaimDetails.refundingCountry.change.hidden"))
        )
      )
    }

  def rowCountry(answers: UserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(RefundingCountryPage).map { answer =>

      SummaryListRowViewModel(
        key     = "checkYourClaimDetails.refundingCountry.subLabel",
        value   = ValueViewModel(HtmlFormat.raw(answer).toString),
        actions = Seq.empty
      )
    }

  def rowLanguageLabel(answers: UserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(RefundingLanguagePage).map { answer =>

      SummaryListRowViewModel(
        key   = "checkYourClaimDetails.refundingLanguage.label",
        value = ValueViewModel(""),
        actions = Seq(
          ActionItemViewModel("site.change", routes.RefundingLanguageController.onPageLoad(CheckMode).url)
            .withVisuallyHiddenText(messages("checkYourClaimDetails.refundingLanguage.change.hidden"))
        )
      )
    }

  def rowLanguage(answers: UserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(RefundingLanguagePage).map { answer =>

      SummaryListRowViewModel(
        key     = "checkYourClaimDetails.refundingLanguage.subLabel",
        value   = ValueViewModel(answer.toString),
        actions = Seq.empty
      )
    }

  def rowRefundPeriodLabel(answers: UserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(RefundPeriodPage).map { answer =>

      SummaryListRowViewModel(
        key   = "checkYourClaimDetails.refundingPeriod.label",
        value = ValueViewModel(HtmlFormat.raw("").toString),
        actions = Seq(
          ActionItemViewModel("site.change", routes.RefundPeriodController.onPageLoad(CheckMode).url)
            .withVisuallyHiddenText(messages("checkYourClaimDetails.refundingPeriod.change.hidden"))
        )
      )
    }

  def rowRefundStart(answers: UserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(RefundPeriodPage).map { answer =>

      SummaryListRowViewModel(
        key     = "checkYourClaimDetails.refundingPeriodStart.subLabel",
        value   = ValueViewModel(HtmlFormat.raw(answer.startDate.toString).toString),
        actions = Seq.empty
      )
    }

  def rowRefundEnd(answers: UserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(RefundPeriodPage).map { answer =>

      SummaryListRowViewModel(
        key     = "checkYourClaimDetails.refundingPeriodEnd.subLabel",
        value   = ValueViewModel(HtmlFormat.raw(answer.endDate.toString).toString),
        actions = Seq.empty
      )
    }

  def rowContactLabel(answers: UserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(ContactDetailsPage).map { answer =>

      SummaryListRowViewModel(
        key   = "checkYourClaimDetails.contactDetails.label",
        value = ValueViewModel(HtmlFormat.raw("").toString),
        actions = Seq(
          ActionItemViewModel("site.change", routes.ContactDetailsController.onPageLoad(CheckMode).url)
            .withVisuallyHiddenText(messages("checkYourClaimDetails.contactDetails.change.hidden"))
        )
      )
    }

  def rowContactEmail(answers: UserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(ContactDetailsPage).map { answer =>

      SummaryListRowViewModel(
        key     = "checkYourClaimDetails.contactEmail.subLabel",
        value   = ValueViewModel(HtmlFormat.raw(answer.email).toString),
        actions = Seq.empty
      )
    }

  def rowContactTelephone(answers: UserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(ContactDetailsPage).map { answer =>

      SummaryListRowViewModel(
        key     = "checkYourClaimDetails.contactTelephone.subLabel",
        value   = ValueViewModel(HtmlFormat.raw(answer.telephone.getOrElse("")).toString),
        actions = Seq.empty
      )
    }

  def rowBusinessActivityLabel(answers: UserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
    val action = if (answers.get(BusinessActivityCodeThreePage).nonEmpty) {
      routes.BusinessActivityThreeController.onPageLoad()
    } else if (answers.get(BusinessActivityCodeTwoPage).nonEmpty) {
      routes.BusinessActivityTwoController.onPageLoad(CheckMode)
    } else {
      routes.BusinessActivityController.onPageLoad(CheckMode)
    }

    Some(
      SummaryListRowViewModel(
        key = KeyViewModel(
          s"""<span class="govuk-!-width-one-half">${messages("checkYourClaimDetails.businessActivity.label")}</span>"""
        ),
        value = ValueViewModel(HtmlFormat.raw("").toString),
        actions = Seq(
          ActionItemViewModel("site.change", action.url).withVisuallyHiddenText(messages("checkYourClaimDetails.businessActivity.change.hidden"))
        )
      )
    )

  def rowBusinessActivity(answers: UserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(BusinessActivityCodePage).map { answer =>

      SummaryListRowViewModel(
        key     = "checkYourClaimDetails.businessActivity.subLabel",
        value   = ValueViewModel(HtmlFormat.raw(answer).toString),
        actions = Seq.empty
      )
    }

  def rowBusinessActivity2(answers: UserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(BusinessActivityCodeTwoPage).map { answer =>

      SummaryListRowViewModel(
        key     = "checkYourClaimDetails.businessActivity2.subLabel",
        value   = ValueViewModel(HtmlFormat.raw(answer).toString),
        actions = Seq.empty
      )
    }

  def rowBusinessActivity3(answers: UserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(BusinessActivityCodeThreePage).map { answer =>

      SummaryListRowViewModel(
        key     = "checkYourClaimDetails.businessActivity3.subLabel",
        value   = ValueViewModel(HtmlFormat.raw(answer).toString),
        actions = Seq.empty
      )
    }

}
