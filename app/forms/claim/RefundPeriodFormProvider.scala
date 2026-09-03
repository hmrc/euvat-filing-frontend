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

package forms.claim

import forms.claim.RefundPeriodData
import forms.mappings.{Mappings, YearMonthFormatter}
import play.api.data.Form
import play.api.data.Forms.{mapping, of}
import play.api.data.validation.{Constraint, Invalid, Valid}
import play.api.i18n.Messages

import java.time.temporal.ChronoUnit
import java.time.{LocalDate, YearMonth}
import javax.inject.Inject

class RefundPeriodFormProvider @Inject() extends Mappings {
  protected def today: LocalDate = LocalDate.now()
  private def currentYearMonth: YearMonth = YearMonth.of(today.getYear, today.getMonthValue)

  private def fieldsForError(message: String): Set[String] = message match {
    case "refundPeriod.start.error.required" | "refundPeriod.start.error.after30Sept" | "refundPeriod.start.error.30SeptOrEarlier" |
        "refundPeriod.start.error.beforeVatRegDate.remainingQuarter" | "refundPeriod.start.error.beforeVatRegDate.firstQuarter" =>
      Set("start.month", "start.year")
    case "refundPeriod.error.startDateNotAfterEndDate" => Set("start.month", "start.year", "end.month", "end.year")
    case "refundPeriod.end.error.required" | "refundPeriod.end.error.afterVatDeRegDate" =>
      Set("end.month", "end.year")
    case "refundPeriod.error.startAndEndInSameYear"    => Set("start.year", "end.year")
    case "refundPeriod.error.periodNotLessThan3Months" => Set("start.month", "end.month")
    case _                                             => Set.empty
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

  def withMappedErrors(form: Form[RefundPeriodData], suppressCutoff: Boolean = false): (Form[RefundPeriodData], Set[String]) = {
    val errorMappings = Map(
      "refundPeriod.error.startDateNotAfterEndDate"                -> "start",
      "refundPeriod.error.startAndEndInSameYear"                   -> "start",
      "refundPeriod.error.periodNotLessThan3Months"                -> "start",
      "refundPeriod.start.error.after30Sept"                       -> "start",
      "refundPeriod.start.error.30SeptOrEarlier"                   -> "start",
      "refundPeriod.start.error.beforeVatRegDate.firstQuarter"     -> "start",
      "refundPeriod.start.error.beforeVatRegDate.remainingQuarter" -> "start",
      "refundPeriod.end.error.afterVatDeRegDate"                   -> "end"
    )

    val earliestPrefix = "refundPeriod.error.beforeEarliest"
    val latestPrefix = "refundPeriod.error.afterLatest"
    val hasFieldLevelErrors = form.errors.exists(e => e.key.contains("."))
    val hasPeriodLengthError = form.errors.exists(_.message == "refundPeriod.error.periodNotLessThan3Months")
    val filteredErrors = if (hasPeriodLengthError) {
      form.errors.filterNot(e => e.message.startsWith(earliestPrefix) || e.message.startsWith(latestPrefix))
    } else {
      form.errors
    }

    val filteredAfterCutoff = if (suppressCutoff) {
      val cutoffKeys = Set("refundPeriod.start.error.after30Sept", "refundPeriod.start.error.30SeptOrEarlier")
      filteredErrors.filterNot(e => cutoffKeys.contains(e.message))
    } else if (hasPeriodLengthError) {
      val cutoffKeys = Set("refundPeriod.start.error.after30Sept", "refundPeriod.start.error.30SeptOrEarlier")
      filteredErrors.filterNot(e => cutoffKeys.contains(e.message))
    } else filteredErrors

    val hasStartYearMismatch = filteredAfterCutoff.exists(_.message == "refundPeriod.error.startAndEndInSameYear")
    val hasLatestOrEarliest = filteredAfterCutoff.exists(e => e.message.startsWith(latestPrefix) || e.message.startsWith(earliestPrefix))
    val filteredAfterCutoff2 =
      if (suppressCutoff && hasLatestOrEarliest) filteredAfterCutoff.filterNot(_.message == "refundPeriod.error.startAndEndInSameYear")
      else filteredAfterCutoff

    val hasEarliestBusiness = filteredAfterCutoff2.exists(e => e.message.startsWith(earliestPrefix))
    val filteredErrors2 = if (!hasFieldLevelErrors && hasEarliestBusiness) {
      filteredAfterCutoff2.filter(e => e.message.startsWith(earliestPrefix))
    } else filteredAfterCutoff2

    import play.api.data.FormError

    val remappedErrors: Seq[FormError] = filteredErrors2.flatMap { error =>
      val msg = error.message
      if (msg.startsWith(earliestPrefix)) {
        msg match {
          case s if s.endsWith(".both")  => Seq(error.copy(key = "start"), error.copy(key = "end"))
          case s if s.endsWith(".start") => Seq(error.copy(key = "start"))
          case s if s.endsWith(".end")   => Seq(error.copy(key = "end"))
          case _                         => Seq(error.copy(key = "start"))
        }
      } else if (msg.startsWith(latestPrefix)) {
        msg match {
          case s if s.endsWith(".both")  => Seq(error.copy(key = "start"), error.copy(key = "end"))
          case s if s.endsWith(".start") => Seq(error.copy(key = "start"))
          case s if s.endsWith(".end")   => Seq(error.copy(key = "end"))
          case _                         => Seq(error.copy(key = "end"))
        }
      } else {
        errorMappings.get(error.message) match {
          case Some(fieldKey) => Seq(error.copy(key = fieldKey))
          case None           => Seq(error)
        }
      }
    }

    val remappedForm = form.copy(errors = remappedErrors)
    val highlighted = highlightedFields(remappedForm)

    (remappedForm, highlighted)
  }

  def apply()(implicit messages: Messages): Form[RefundPeriodData] = apply(None, None, skipSeptemberCutoff = false)

  def apply(earliest: Option[YearMonth], latest: Option[YearMonth], skipSeptemberCutoff: Boolean = false)(implicit
    messages: Messages
  ): Form[RefundPeriodData] = {
    import play.api.data.Mapping

    val baseMapping: Mapping[RefundPeriodData] = mapping(
      "start" -> of(yearMonthFormatter("start")),
      "end"   -> of(yearMonthFormatter("end"))
    )((s, e) => RefundPeriodData(s, e))(rd => Some(rd.start, rd.end))

    val mapped: Mapping[RefundPeriodData] = baseMapping
      .verifying(earliestConstraint(earliest))
      .verifying(latestConstraint(latest))
      .verifying(
        "refundPeriod.error.startDateNotAfterEndDate",
        data => {
          if (!data.start.isBefore(currentYearMonth) || !data.end.isBefore(currentYearMonth)) { true }
          else { !data.start.isAfter(data.end) }
        }
      )
      .verifying("refundPeriod.error.startAndEndInSameYear", datesInSameYear(earliest))
      .verifying("refundPeriod.error.periodNotLessThan3Months", periodLessThan3Months)

    Form(mapped)
  }

  private def datesInSameYear(earliestOpt: Option[YearMonth]): RefundPeriodData => Boolean = { data =>
    if (!data.start.isBefore(currentYearMonth) && !data.end.isBefore(currentYearMonth)) {
      true
    } else if (!data.start.isBefore(data.end)) {
      true
    } else {
      earliestOpt match {
        case Some(_) =>
          data.start.getYear == data.end.getYear
        case None =>
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
      if (!data.start.isBefore(currentYearMonth) || !data.end.isBefore(currentYearMonth)) { true }
      else if (data.start.isAfter(data.end)) { true }
      else if (data.end.getMonthValue == 12) { true }
      else {
        ChronoUnit.MONTHS.between(data.start, data.end) >= 2
      }
    }
  }

  private def earliestConstraint(earliestOpt: Option[YearMonth]): Constraint[RefundPeriodData] =
    Constraint { data =>
      earliestOpt match {
        case None => Valid
        case Some(min) =>
          val startBefore = data.start.isBefore(min)
          val endBefore = data.end.isBefore(min)
          if (!startBefore && !endBefore) Valid
          else {
            val human = min.atDay(1).format(java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy"))
            (startBefore, endBefore) match {
              case (true, true)  => Invalid("refundPeriod.error.beforeEarliest.both", human)
              case (true, false) => Invalid("refundPeriod.error.beforeEarliest.start", human)
              case (false, true) => Invalid("refundPeriod.error.beforeEarliest.end", human)
              case _             => Valid
            }
          }
      }
    }

  private def latestConstraint(latestOpt: Option[YearMonth]): Constraint[RefundPeriodData] =
    Constraint { data =>
      latestOpt match {
        case None => Valid
        case Some(max) =>
          val startAfter = data.start.isAfter(max)
          val endAfter = data.end.isAfter(max)
          if (!startAfter && !endAfter) Valid
          else {
            val human = max.atDay(1).format(java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy"))
            (startAfter, endAfter) match {
              case (true, true)  => Invalid("refundPeriod.error.afterLatest.both", human)
              case (true, false) => Invalid("refundPeriod.error.afterLatest.start", human)
              case (false, true) => Invalid("refundPeriod.error.afterLatest.end", human)
              case _             => Valid
            }
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

case class RefundPeriodData(start: YearMonth, end: YearMonth)
