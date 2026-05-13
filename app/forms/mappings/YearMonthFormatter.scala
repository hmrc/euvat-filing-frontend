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

import java.time.YearMonth
import scala.util.{Failure, Success, Try}

class YearMonthFormatter(
  invalidKey: String,
  allRequiredKey: String,
  twoRequiredKey: String,
  requiredKey: String,
  args: Seq[String] = Seq.empty
)(implicit messages: Messages)
    extends Formatter[YearMonth]
    with Formatters {

  private val fieldKeys: List[String] = List("month", "year")

  private def parseMonthName(input: String): Option[String] = {
    val normalised = input.trim.toLowerCase
    val months = Map(
      "january"   -> 1,
      "jan"       -> 1,
      "february"  -> 2,
      "feb"       -> 2,
      "march"     -> 3,
      "mar"       -> 3,
      "april"     -> 4,
      "apr"       -> 4,
      "may"       -> 5,
      "june"      -> 6,
      "jun"       -> 6,
      "july"      -> 7,
      "jul"       -> 7,
      "august"    -> 8,
      "aug"       -> 8,
      "september" -> 9,
      "sep"       -> 9,
      "october"   -> 10,
      "oct"       -> 10,
      "november"  -> 11,
      "nov"       -> 11,
      "december"  -> 12,
      "dec"       -> 12
    )
    months.get(normalised).map(_.toString)
  }

  private def normaliseData(key: String, data: Map[String, String]): Map[String, String] =
    data.map {
      case (k, v) if k == s"$key.month" =>
        k -> (if (v.matches("""\d{1,2}""")) v else parseMonthName(v).getOrElse(v))
      case other => other
    }

  private def toYearMonth(key: String, month: Int, year: Int): Either[Seq[FormError], YearMonth] =
    Try(YearMonth.of(year, month)) match {
      case Success(ym) => Right(ym)
      case Failure(_) => Left(Seq(FormError(key, invalidKey, args)))
    }

  private def formatYearMonth(key: String, data: Map[String, String]): Either[Seq[FormError], YearMonth] = {
    val int = intFormatter(
      requiredKey = invalidKey,
      wholeNumberKey = invalidKey,
      nonNumericKey = invalidKey,
      args
    )

    val monthResult = int.bind(s"$key.month", data)
    val rawYearResult = int.bind(s"$key.year", data)
    val yearResult = rawYearResult.flatMap { y =>
      if (!y.toString.matches("[0-9]{4}")) Left(Seq(FormError(s"$key.year", s"$invalidKey.year", args)))
      else Right(y)
    }

    (monthResult, yearResult) match {
      case (Right(month), Right(year)) =>
        toYearMonth(key, month, year)
      case (Left(_), Left(_)) =>
        Left(Seq(FormError(key, invalidKey, args)))
      case (Left(_), Right(_)) =>
        Left(Seq(FormError(s"$key.month", s"$invalidKey.month", args)))
      case (Right(_), Left(_)) =>
        Left(Seq(FormError(s"$key.year", s"$invalidKey.year", args)))
    }
  }

  override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], YearMonth] = {

    val fields = fieldKeys.map { field =>
      field -> data.get(s"$key.$field").filter(_.nonEmpty)
    }.toMap

    fields.count(_._2.isDefined) match {
      case 2 =>
        formatYearMonth(key, normaliseData(key, data))
      case 1 =>
        val missingField = fields.find(_._2.isEmpty).map(_._1).getOrElse("month")
        val errorKey = s"$twoRequiredKey.$missingField"
        Left(List(FormError(s"$key.$missingField", errorKey, args)))
      case _ =>
        Left(List(FormError(key, allRequiredKey, args)))
    }
  }

  override def unbind(key: String, value: YearMonth): Map[String, String] =
    Map(
      s"$key.month" -> value.getMonthValue.toString,
      s"$key.year"  -> value.getYear.toString
    )
}
