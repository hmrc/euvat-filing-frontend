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
import play.api.data.Form
import play.api.data.Forms.{mapping, of}
import play.api.data.validation.{Constraint, Invalid, Valid}
import play.api.i18n.Messages

import java.time.temporal.ChronoUnit
import java.time.{LocalDate, YearMonth}
import javax.inject.Inject

case class RefundPeriodData(start: YearMonth, end: YearMonth)

class RefundPeriodFormProvider @Inject() () {

  protected def today: LocalDate = LocalDate.now()

  private val septemberCutoffConstraint: Constraint[RefundPeriodData] =
    Constraint { data =>
      if (!data.start.isBefore(YearMonth.now()) || !data.end.isBefore(YearMonth.now())) { Valid }
      else {
        val (cutoff, errorKey) =
          if (today.isAfter(LocalDate.of(today.getYear, 9, 30))) {
            (YearMonth.of(today.getYear, 1), "refundPeriod.start.error.after30Sept")
          } else {
            (YearMonth.of(today.getYear - 1, 1), "refundPeriod.start.error.30SeptOrEarlier")
          }

        if (!data.start.isBefore(cutoff)) { Valid }
        else { Invalid(errorKey, cutoff.getYear.toString) }
      }
    }

  private def fieldsForError(message: String): Set[String] = message match {
    case "refundPeriod.start.error.required" | "refundPeriod.start.error.after30Sept" | "refundPeriod.start.error.30SeptOrEarlier" =>
      Set("start.month", "start.year")
    case "refundPeriod.error.startDateNotAfterEndDate"                       => Set("start.month", "start.year", "end.month", "end.year")
    case "refundPeriod.end.error.required" | "refundPeriod.end.error.inPast" => Set("end.month", "end.year")
    case "refundPeriod.error.startAndEndInSameYear"                          => Set("start.year", "end.year")
    case "refundPeriod.error.periodNotLessThan3Months"                       => Set("start.month", "end.month")
    case _                                                                   => Set.empty
  }

  private def highlightedFields(form: Form[RefundPeriodData]): Set[String] = {
    val errorMessages = form.errors.map(_.message).toSet
    val fieldErrors = Set(
      form.error("start.month").map(_ => "start.month"),
      form.error("start.year").map(_ => "start.year"),
      form.error("end.month").map(_ => "end.month"),
      form.error("end.year").map(_ => "end.year")
    ).flatten

    val businessRuleFields = errorMessages.flatMap(fieldsForError)

    fieldErrors ++ businessRuleFields
  }

  def withMappedErrors(form: Form[RefundPeriodData]): (Form[RefundPeriodData], Set[String]) = {
    val errorMappings = Map(
      "refundPeriod.error.startDateNotAfterEndDate" -> "start",
      "refundPeriod.error.startAndEndInSameYear"    -> "start",
      "refundPeriod.error.periodNotLessThan3Months" -> "start",
      "refundPeriod.start.error.inPast"             -> "start",
      "refundPeriod.end.error.inPast"               -> "end",
      "refundPeriod.error.startAndEndInPast"        -> "start",
      "refundPeriod.start.error.after30Sept"        -> "start",
      "refundPeriod.start.error.30SeptOrEarlier"    -> "start"
    )

    val highlighted = highlightedFields(form)

    val remappedErrors = form.errors.map { error =>
      errorMappings.get(error.message) match {
        case Some(fieldKey) => error.copy(key = fieldKey)
        case None           => error
      }
    }

    (form.copy(errors = remappedErrors), highlighted)
  }

  def apply()(implicit messages: Messages): Form[RefundPeriodData] =
    Form(
      mapping(
        "start" -> of(yearMonthFormatter("start")),
        "end"   -> of(yearMonthFormatter("end"))
      )((s, e) => RefundPeriodData(s, e))(rd => Some(rd.start, rd.end))
        .verifying(
          "refundPeriod.error.startDateNotAfterEndDate",
          data => {
            if (!data.start.isBefore(YearMonth.now()) || !data.end.isBefore(YearMonth.now())) { true }
            else { !data.start.isAfter(data.end) }
          }
        )
        .verifying("refundPeriod.error.startAndEndInSameYear", datesInSameYear)
        .verifying("refundPeriod.error.periodNotLessThan3Months", periodLessThan3Months) // TODO - warning messages
        .verifying("refundPeriod.end.error.inPast", data => data.end.isBefore(YearMonth.now())) // TODO - warning messages
        .verifying(septemberCutoffConstraint) // TODO - warning messages
    )

  private val datesInSameYear: RefundPeriodData => Boolean = { data =>
    {
      if (!data.start.isBefore(YearMonth.now()) || !data.end.isBefore(YearMonth.now())) {
        true
      } else if (!data.start.isBefore(data.end)) {
        true
      } else {
        val cutoff =
          if (today.isAfter(LocalDate.of(today.getYear, 9, 30))) {
            YearMonth.of(today.getYear, 1)
          } else {
            YearMonth.of(today.getYear - 1, 1)
          }
        if (data.start.isBefore(cutoff)) {
          true
        } else {
          data.start.getYear == data.end.getYear
        }
      }
    }
  }

  private val periodLessThan3Months: RefundPeriodData => Boolean = { data =>
    {
      if (!data.start.isBefore(YearMonth.now()) || !data.end.isBefore(YearMonth.now())) { true }
      else if (data.start.isAfter(data.end)) { true }
      else if (data.end.getMonthValue == 12) { true }
      else {
        val cutoff =
          if (today.isAfter(LocalDate.of(today.getYear, 9, 30))) { YearMonth.of(today.getYear, 1) }
          else { YearMonth.of(today.getYear - 1, 1) }
        if (data.start.isBefore(cutoff)) { true }
        else { ChronoUnit.MONTHS.between(data.start, data.end) >= 2 }
      }
    }
  }

  private def yearMonthFormatter(prefix: String)(implicit messages: Messages): YearMonthFormatter = {
    new YearMonthFormatter(
      invalidKey     = s"refundPeriod.$prefix.error.invalidDateFormat",
      allRequiredKey = s"refundPeriod.$prefix.error.required",
      twoRequiredKey = s"refundPeriod.$prefix.error.required",
      requiredKey    = s"refundPeriod.$prefix.error.required"
    )
  }
}
