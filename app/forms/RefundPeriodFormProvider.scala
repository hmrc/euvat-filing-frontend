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

package forms

import forms.mappings.YearMonthFormatter

import javax.inject.Inject
import play.api.data.Form
import play.api.data.Forms.{mapping, of}
import play.api.data.validation.{Constraint, Invalid, Valid}
import play.api.i18n.Messages

import java.time.{LocalDate, YearMonth}

case class RefundPeriodData(start: YearMonth, end: YearMonth)

class RefundPeriodFormProvider @Inject()() {

  protected def today: LocalDate = LocalDate.now()

  private val septemberCutoffConstraint: Constraint[RefundPeriodData] =
    Constraint { data =>
      val (cutoff, errorKey) = if (today.isAfter(java.time.LocalDate.of(today.getYear, 9, 30)))
        (YearMonth.of(today.getYear, 1), "refundPeriod.error.periodStartDateafter30thSept")
      else
        (YearMonth.of(today.getYear - 1, 1), "refundPeriod.error.periodStartDate30thSeptOrEarlier")

      if (!data.start.isBefore(cutoff)) Valid
      else Invalid(errorKey)
    }

  def apply()(implicit messages: Messages): Form[RefundPeriodData] =
    Form(
      mapping(
        "start" -> of(new YearMonthFormatter(
          invalidKey = "refundPeriod.error.periodStartDateinvalidStartEndDateFormat",
          allRequiredKey = "refundPeriod.error.periodStartDatecompleteFieldname",
          twoRequiredKey = "refundPeriod.error.periodStartDateinvalidStartEndDateFormat",
          requiredKey = "refundPeriod.error.periodStartDatecompleteFieldname"
        )),
        "end" -> of(new YearMonthFormatter(
          invalidKey = "refundPeriod.error.periodEndDateinvalidStartEndDateFormat",
          allRequiredKey = "refundPeriod.error.periodEndDatecompleteFieldname",
          twoRequiredKey = "refundPeriod.error.periodEndDateinvalidStartEndDateFormat",
          requiredKey = "refundPeriod.error.periodEndDatecompleteFieldname"
        ))
      )((s,e) => RefundPeriodData(s,e)) (rd => Some(rd.start, rd.end))
        .verifying("refundPeriod.error.periodStartDatenotAfterEndDate", data => data.start.isBefore(data.end))
        .verifying("refundPeriod.error.periodEndDaterefundPeriodInSingleYear", data => data.start.getYear == data.end.getYear)
        .verifying("refundPeriod.error.periodStartDateperiodNotLessThan3Months", data => {
          if (!data.start.isBefore(data.end)) true
          else if (data.end.getMonthValue == 12) true
          else java.time.temporal.ChronoUnit.MONTHS.between(data.start, data.end) >= 2
        })
        .verifying("refundPeriod.error.periodEndDateInvalid", data => data.end.isBefore(YearMonth.now()) || data.end.equals(YearMonth.now()))
        .verifying(septemberCutoffConstraint)
    )
}
