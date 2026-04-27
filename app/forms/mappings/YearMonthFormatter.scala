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

import play.api.data.format.Formatter
import play.api.data.FormError
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

  private def toYearMonth(key: String, month: Int, year: Int): Either[Seq[FormError], YearMonth] =
    Try(YearMonth.of(year, month)) match {
      case Success(ym) => Right(ym)
      case Failure(_)   => Left(Seq(FormError(key, invalidKey, args)))
    }

  private def formatYearMonth(key: String, data: Map[String, String]): Either[Seq[FormError], YearMonth] = {

    val int = intFormatter(
      requiredKey = invalidKey,
      wholeNumberKey = invalidKey,
      nonNumericKey = invalidKey,
      args
    )

    val month = new MonthFormatter(invalidKey, args)

    for {
      month <- month.bind(s"$key.month", data)
      year <- int.bind(s"$key.year", data)
      ym <- toYearMonth(key, month, year)
    } yield ym
  }

  override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], YearMonth] = {

    val fields = fieldKeys.map { field =>
      field -> data.get(s"$key.$field").filter(_.nonEmpty)
    }.toMap

    lazy val missingFields = fields
      .withFilter(_._2.isEmpty)
      .map(_._1)
      .toList
      .map(field => messages(s"date.error.$field"))

    fields.count(_._2.isDefined) match {
      case 2 =>
        formatYearMonth(key, data).left.map(_.map(_.copy(key = key, args = args)))
      case 1 =>
        Left(List(FormError(key, twoRequiredKey, missingFields ++ args)))
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
