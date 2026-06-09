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

package forms.mappings

import play.api.data.FormError
import play.api.data.format.Formatter
import play.api.i18n.Messages

import java.time.{LocalDate, Month}
import scala.util.{Failure, Success, Try}

private[mappings] class LocalDateFormatter(
  invalidKey: String,
  allRequiredKey: String,
  twoRequiredKey: String,
  requiredKey: String,
  args: Seq[String] = Seq.empty,
  usePerFieldKeys: Boolean = false
)(implicit messages: Messages)
    extends Formatter[LocalDate]
    with Formatters {

  private val fieldKeys: List[String] = List("day", "month", "year")

  private def toDate(key: String, day: Int, month: Int, year: Int): Either[Seq[FormError], LocalDate] = {
    import java.time.YearMonth

    val dayInvalidKey = if (usePerFieldKeys) s"$invalidKey.day" else invalidKey
    val monthInvalidKey = if (usePerFieldKeys) s"$invalidKey.month" else invalidKey
    val yearInvalidKey = if (usePerFieldKeys) s"$invalidKey.year" else invalidKey

    Try(YearMonth.of(year, month)) match {
      case Success(ym) =>
        // If the day is outside absolute 1-31 range, it's a day-specific error
        if (day < 1 || day > 31) Left(Seq(FormError(key, dayInvalidKey, args)))
        else if (day > ym.lengthOfMonth) {
          // Numbers individually look plausible (e.g., 30 Feb) but the date doesn't exist
          // Treat this as a day-specific error so the view can anchor to the day input.
          val dayArgs = if (invalidKey.startsWith("invoiceDate")) List(messages("date.error.day")) ++ args else args
          Left(Seq(FormError(key, dayInvalidKey, dayArgs)))
        } else
          Try(LocalDate.of(year, month, day)) match {
            case Success(date) => Right(date)
            case Failure(_)    => Left(Seq(FormError(key, invalidKey, args)))
          }
      case Failure(_) => Left(Seq(FormError(key, invalidKey, args)))
    }
  }

  private def formatDate(key: String, data: Map[String, String]): Either[Seq[FormError], LocalDate] = {

    val dayInvalidKey = if (usePerFieldKeys) s"$invalidKey.day" else invalidKey
    val monthInvalidKey = if (usePerFieldKeys) s"$invalidKey.month" else invalidKey
    val yearInvalidKey = if (usePerFieldKeys) s"$invalidKey.year" else invalidKey

    val dayFormatter = new Formatter[Int] with Formatters {
      private val baseFormatter = stringFormatter(dayInvalidKey, args)
      private val oneOrTwo = "^\\d{1,2}$"

      override def bind(key: String, data: Map[String, String]) =
        baseFormatter
          .bind(key, data)
          .map(_.replace(",", ""))
          .flatMap { s =>
            if (!s.matches(oneOrTwo)) Left(Seq(FormError(key, dayInvalidKey, args)))
            else
              scala.util.control.Exception.nonFatalCatch
                .either(s.toInt)
                .left
                .map(_ => Seq(FormError(key, dayInvalidKey, args)))
          }

      override def unbind(key: String, value: Int) = Map(key -> value.toString)
    }

    val monthFormatter = new MonthFormatter(monthInvalidKey, args)

    val yearFormatter = new Formatter[Int] with Formatters {
      private val baseFormatter = stringFormatter(yearInvalidKey, args)

      override def bind(key: String, data: Map[String, String]) =
        baseFormatter
          .bind(key, data)
          .map(_.replace(",", "").trim)
          .flatMap { s =>
            val yearRegexp = "^\\d{4}$"
            val digitsOnly = s
            if (!digitsOnly.matches(yearRegexp)) Left(Seq(FormError(key, yearInvalidKey, args)))
            else
              scala.util.control.Exception.nonFatalCatch
                .either(digitsOnly.toInt)
                .left
                .map(_ => Seq(FormError(key, yearInvalidKey, args)))
                .flatMap(parsed => Right(parsed))
          }

      override def unbind(key: String, value: Int) = Map(key -> value.toString)
    }

    val dayResult = dayFormatter.bind(s"$key.day", data)
    val monthResult = monthFormatter.bind(s"$key.month", data)
    val yearResult = yearFormatter.bind(s"$key.year", data)

    // Build an ordered list of invalid fields (day, month, year)
    val bindingInvalids = List(
      dayResult.left.toOption.map(_ => "day"),
      monthResult.left.toOption.map(_ => "month"),
      yearResult.left.toOption.map(_ => "year")
    ).flatten

    // Detect semantic day invalid (e.g., 32 March) even when year parsing failed
    // Only consider a semantic day-invalid when year parsing has failed (so we can't check full date validity).
    val semanticDayInvalid: Boolean = (dayResult, monthResult, yearResult) match {
      case (Right(d), Right(m), Left(_)) =>
        // Attempt to infer a numeric year from the raw input when year binding failed (e.g. "2026asc").
        // If we can parse a year, use it to compute the true month length for February; otherwise fall back to conservative 29.
        val maxForMonth: Int = m match {
          case 2 =>
            val rawYearOpt = data.get(s"$key.year")
            val parsedYearOpt = rawYearOpt.flatMap { s =>
              """\d+""".r.findFirstIn(s).flatMap(str => Try(str.toInt).toOption)
            }

            parsedYearOpt match {
              case Some(parsedYear) =>
                import java.time.YearMonth
                Try(YearMonth.of(parsedYear, m).lengthOfMonth()).getOrElse(29)
              case None => 29
            }
          case 4 | 6 | 9 | 11 => 30
          case _              => 31
        }
        d < 1 || d > maxForMonth
      case _ => false
    }

    // Also mark day invalid when it is numerically outside 1..31 even if month failed to bind
    val absoluteDayInvalid: Boolean = dayResult match {
      case Right(d) => d < 1 || d > 31
      case _        => false
    }

    val invalidFieldKeys: List[String] = {
      val base = bindingInvalids
      val withSemantic = if ((semanticDayInvalid || absoluteDayInvalid) && !base.contains("day")) base :+ "day" else base
      // Preserve order day, month, year
      List("day", "month", "year").filter(withSemantic.contains)
    }

    if (invalidFieldKeys.nonEmpty) {
      val invalidFields = invalidFieldKeys.map {
        case "day"   => messages("date.error.day")
        case "month" => messages("date.error.month")
        case "year"  => messages("date.error.year")
      }

      invalidFieldKeys.size match {
        case 1 =>
          invalidFieldKeys match {
            case List("day") =>
              if (usePerFieldKeys) Left(List(FormError(key, dayInvalidKey, List(messages("date.error.day")) ++ args)))
              else Left(List(FormError(key, dayInvalidKey, args)))
            case List("month") =>
              if (usePerFieldKeys) Left(List(FormError(key, monthInvalidKey, List(messages("date.error.month")) ++ args)))
              else Left(List(FormError(key, monthInvalidKey, args)))
            case List("year") =>
              if (usePerFieldKeys) Left(List(FormError(key, yearInvalidKey, List(messages("date.error.year")) ++ args)))
              else Left(List(FormError(key, yearInvalidKey, args)))
            case _ => Left(List(FormError(key, invalidKey, args)))
          }
        case 2 =>
          if (usePerFieldKeys) {
            val multiKey = s"$invalidKey.two"
            val rendered = messages(multiKey, invalidFields*)
            Left(List(FormError(key, rendered, invalidFields ++ args)))
          } else {
            // Preserve legacy behavior for generic invalid keys: no args, generic message
            Left(List(FormError(key, invalidKey, List.empty)))
          }
        case _ => Left(List(FormError(key, invalidKey, args)))
      }
    } else {
      for {
        day   <- dayResult
        month <- monthResult
        year  <- yearResult
        date  <- toDate(key, day, month, year)
      } yield date
    }
  }

  override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], LocalDate] = {

    val fields = fieldKeys.map { field =>
      field -> data.get(s"$key.$field").filter(_.nonEmpty)
    }

    lazy val missingFields = fields
      .filter(_._2.isEmpty)
      .map(_._1)
      .toList
      .map(field => messages(s"date.error.$field"))

    fields.count(_._2.isDefined) match {
      case 3 =>
        formatDate(key, data).left.map {
          _.map { fe =>
            // Preserve any args produced by formatDate (these contain which fields are invalid)
            if (fe.args.nonEmpty) fe.copy(key = key)
            else fe.copy(key                  = key, args = args)
          }
        }
      case 2 =>
        Left(List(FormError(key, requiredKey, missingFields ++ args)))
      case 1 =>
        Left(List(FormError(key, twoRequiredKey, missingFields ++ args)))
      case _ =>
        Left(List(FormError(key, allRequiredKey, args)))
    }
  }

  override def unbind(key: String, value: LocalDate): Map[String, String] =
    Map(
      s"$key.day"   -> value.getDayOfMonth.toString,
      s"$key.month" -> value.getMonthValue.toString,
      s"$key.year"  -> value.getYear.toString
    )
}

private class MonthFormatter(invalidKey: String, args: Seq[String] = Seq.empty) extends Formatter[Int] with Formatters {

  private val baseFormatter = stringFormatter(invalidKey, args)

  override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], Int] = {

    val months = Month.values.toList

    baseFormatter
      .bind(key, data)
      .flatMap { str =>
        val numericOneOrTwo = "^\\d{1,2}$".r
        if (numericOneOrTwo.matches(str)) {
          // accept 1..12 or 01..12
          val v = str.toInt
          if (v >= 1 && v <= 12) Right(v)
          else Left(List(FormError(key, invalidKey, args)))
        } else {
          // accept full month names (e.g. February) or 3-letter abbreviations (e.g. Feb), case-insensitive
          months
            .find(m => m.toString.equalsIgnoreCase(str) || m.toString.take(3).equalsIgnoreCase(str))
            .map(x => Right(x.getValue))
            .getOrElse(Left(List(FormError(key, invalidKey, args))))
        }
      }
  }

  override def unbind(key: String, value: Int): Map[String, String] =
    Map(key -> value.toString)
}
