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
import play.api.i18n.{Lang, Messages}
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import utils.DateTimeFormats.{dateTimeFormat, shortMonthYearFormat}
import viewmodels.govuk.summarylist.*
import viewmodels.implicits.*

object CheckYourClaimDetailsSummary {

  def rowCountry(answers: UserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(RefundingCountryNamePage).map { countryName =>
      SummaryListRowViewModel(
        key   = "checkYourClaimDetails.refundingCountry.subLabel",
        value = ValueViewModel(HtmlFormat.raw(countryName).toString),
        actions = Seq(
          ActionItemViewModel("site.change", routes.RefundingCountryController.onPageLoad(CheckMode).url)
            .withVisuallyHiddenText(messages("checkYourClaimDetails.refundingCountry.change.hidden"))
            .withAttribute("aria-label" -> s"${messages("site.change")} ${messages("checkYourClaimDetails.refundingCountry.change.hidden")}")
        )
      )
    }

  def rowLanguage(answers: UserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(RefundingLanguagePage).map { answer =>
      SummaryListRowViewModel(
        key   = "checkYourClaimDetails.refundingLanguage.subLabel",
        value = ValueViewModel(messages(s"refundingLanguage.${answer.toString}")),
        actions = Seq(
          ActionItemViewModel("site.change", routes.RefundingLanguageController.onPageLoad(CheckMode).url)
            .withVisuallyHiddenText(messages("checkYourClaimDetails.refundingLanguage.change.hidden"))
            .withAttribute("aria-label" -> s"${messages("site.change")} ${messages("checkYourClaimDetails.refundingLanguage.change.hidden")}")
        )
      )
    }

  def rowCurrency(displayName: Option[String])(implicit messages: Messages): Option[SummaryListRow] =
    displayName.map { name =>
      SummaryListRowViewModel(
        key   = "checkYourClaimDetails.refundingCurrency.subLabel",
        value = ValueViewModel(name),
        actions = Seq(
          ActionItemViewModel("site.change", routes.RefundingCurrencyController.onPageLoad(CheckMode).url)
            .withVisuallyHiddenText(messages("checkYourClaimDetails.refundingCurrency.change.hidden"))
            .withAttribute("aria-label" -> s"${messages("site.change")} ${messages("checkYourClaimDetails.refundingCurrency.change.hidden")}")
        )
      )
    }

  def rowRefundStart(answers: UserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(RefundPeriodPage).map { answer =>
      implicit val lang: Lang = messages.lang
      SummaryListRowViewModel(
        key   = "checkYourClaimDetails.refundingPeriodStart.subLabel",
        value = ValueViewModel(answer.startDate.format(shortMonthYearFormat())),
        actions = Seq(
          ActionItemViewModel("site.change", routes.RefundPeriodController.onPageLoad(CheckMode).url + "#start.month")
            .withVisuallyHiddenText(messages("checkYourClaimDetails.refundingStartDate.change.hidden"))
            .withAttribute("aria-label" -> s"${messages("site.change")} ${messages("checkYourClaimDetails.refundingStartDate.change.hidden")}")
        )
      )
    }

  def rowRefundEnd(answers: UserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(RefundPeriodPage).map { answer =>
      implicit val lang: Lang = messages.lang
      SummaryListRowViewModel(
        key   = "checkYourClaimDetails.refundingPeriodEnd.subLabel",
        value = ValueViewModel(answer.endDate.format(shortMonthYearFormat())),
        actions = Seq(
          ActionItemViewModel("site.change", routes.RefundPeriodController.onPageLoad(CheckMode).url + "#end.month")
            .withVisuallyHiddenText(messages("checkYourClaimDetails.refundingEndDate.change.hidden"))
            .withAttribute("aria-label" -> s"${messages("site.change")} ${messages("checkYourClaimDetails.refundingEndDate.change.hidden")}")
        )
      )
    }

  def rowContactEmail(answers: UserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(ContactDetailsPage).map { answer =>
      SummaryListRowViewModel(
        key   = "checkYourClaimDetails.contactEmail.subLabel",
        value = ValueViewModel(HtmlFormat.raw(answer.email).toString),
        actions = Seq(
          ActionItemViewModel("site.change", routes.ContactDetailsController.onPageLoad(CheckMode).url + "#contactEmail")
            .withVisuallyHiddenText(messages("checkYourClaimDetails.Email.change.hidden"))
            .withAttribute("aria-label" -> s"${messages("site.change")} ${messages("checkYourClaimDetails.Email.change.hidden")}")
        )
      )
    }

  def rowContactPhone(answers: UserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(ContactDetailsPage).map { answer =>
      SummaryListRowViewModel(
        key   = "checkYourClaimDetails.contactPhone.subLabel",
        value = ValueViewModel(HtmlFormat.escape(answer.telephone.getOrElse("Not provided")).toString),
        actions = Seq(
          ActionItemViewModel("site.change", routes.ContactDetailsController.onPageLoad(CheckMode).url + "#contactTelephone")
            .withVisuallyHiddenText(messages("checkYourClaimDetails.Phone.change.hidden"))
            .withAttribute("aria-label" -> s"${messages("site.change")} ${messages("checkYourClaimDetails.Phone.change.hidden")}")
        )
      )
    }

  def rowBusinessActivity(answers: UserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(BusinessActivityCodePage).map { answer =>

      val viewUrl = (answers.get(BusinessActivityCodeTwoPage), answers.get(BusinessActivityCodeThreePage)) match {
        case (_, Some(_)) => routes.BusinessActivityThreeController.onPageLoad().url
        case (Some(_), _) => routes.BusinessActivityTwoController.onPageLoad(CheckMode).url
        case _            => routes.BusinessActivityController.onPageLoad(CheckMode).url
      }

      SummaryListRowViewModel(
        key   = "checkYourClaimDetails.businessActivity.subLabel",
        value = ValueViewModel(HtmlFormat.raw(answer).toString),
        actions = Seq(
          ActionItemViewModel("site.view", viewUrl)
            .withVisuallyHiddenText(messages("checkYourClaimDetails.businessActivity1.view.hidden"))
            .withAttribute("aria-label" -> s"${messages("site.view")} ${messages("checkYourClaimDetails.businessActivity1.view.hidden")}")
        )
      )
    }

  def rowBusinessActivity2(answers: UserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(BusinessActivityCodeTwoPage).map { answer =>
      SummaryListRowViewModel(
        key   = "checkYourClaimDetails.businessActivity2.subLabel",
        value = ValueViewModel(HtmlFormat.raw(answer).toString),
        actions = Seq(
          ActionItemViewModel("site.change", routes.BusinessActivityCodeTwoController.onPageLoad(CheckMode).url)
            .withVisuallyHiddenText(messages("checkYourClaimDetails.businessActivity2.change.hidden"))
            .withAttribute("aria-label" -> s"${messages("site.change")} ${messages("checkYourClaimDetails.businessActivity2.change.hidden")}")
        )
      )
    }

  def rowBusinessActivity3(answers: UserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(BusinessActivityCodeThreePage).map { answer =>
      SummaryListRowViewModel(
        key   = "checkYourClaimDetails.businessActivity3.subLabel",
        value = ValueViewModel(HtmlFormat.raw(answer).toString),
        actions = Seq(
          ActionItemViewModel("site.change", routes.BusinessActivityCodeThreeController.onPageLoad(CheckMode).url)
            .withVisuallyHiddenText(messages("checkYourClaimDetails.businessActivity3.change.hidden"))
            .withAttribute("aria-label" -> s"${messages("site.change")} ${messages("checkYourClaimDetails.businessActivity3.change.hidden")}")
        )
      )
    }

}
