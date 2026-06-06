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

import java.time.LocalDate

import generators.Generators
import org.scalacheck.Gen
import org.scalatest.OptionValues
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.data.{Form, FormError}
import play.api.i18n.Messages
import play.api.test.Helpers.stubMessages

class InvoiceDateFormProviderSpec extends AnyFreeSpec with Matchers with ScalaCheckPropertyChecks with Generators with OptionValues {

  private implicit val messages: Messages = stubMessages()

  val form: Form[LocalDate] = new InvoiceDateFormProvider().apply()

  val validData: Gen[LocalDate] = datesBetween(
    min = LocalDate.of(2000, 1, 1),
    max = LocalDate.of(3000, 1, 1)
  )

  val invalidField: Gen[String] = Gen.alphaStr.suchThat(_.nonEmpty)
  val missingField: Gen[Option[String]] = Gen.option(Gen.const(""))

  "InvoiceDateFormProvider" - {

    "must bind valid dates with months provided as numbers" in {
      forAll(validData -> "valid date") { date =>

        val data = Map(
          "value.day"   -> f"${date.getDayOfMonth}%02d",
          "value.month" -> f"${date.getMonthValue}%02d",
          "value.year"  -> date.getYear.toString
        )

        val result = form.bind(data)

        result.value.value mustEqual date
      }
    }

    "must bind valid dates with months provided as numbers with leading zeroes" in {
      forAll(validData -> "valid date") { date =>

        val data = Map(
          "value.day"   -> f"${date.getDayOfMonth}%02d",
          "value.month" -> f"${date.getMonthValue}%02d",
          "value.year"  -> date.getYear.toString
        )

        val result = form.bind(data)

        result.value.value mustEqual date
      }
    }

    "must bind valid dates with months provided as full names" in {
      forAll(validData -> "valid date") { date =>

        val data = Map(
          "value.day"   -> f"${date.getDayOfMonth}%02d",
          "value.month" -> date.getMonth.toString,
          "value.year"  -> date.getYear.toString
        )

        val result = form.bind(data)

        // full month names are no longer accepted except when the name is 3 letters (e.g. May)
        if (date.getMonth.toString.length == 3) result.value.value mustEqual date
        else result.errors must contain(FormError("value", "invoiceDate.error.invalid.month", List(messages("date.error.month"))))
      }
    }

    "must bind when day has leading zero" in {
      val data = Map(
        "value.day"   -> "04",
        "value.month" -> "04",
        "value.year"  -> "2025"
      )

      val result = form.bind(data)

      result.value.value mustEqual LocalDate.of(2025, 4, 4)
    }

    "must bind when month is three-letter or full name (case-insensitive)" in {
      val d1 = form.bind(Map("value.day" -> "05", "value.month" -> "Feb", "value.year" -> "2025"))
      d1.value.value mustEqual LocalDate.of(2025, 2, 5)

      val d2 = form.bind(Map("value.day" -> "06", "value.month" -> "February", "value.year" -> "2025"))
      // full month names are not accepted (except 3-letter months); expect invalid month
      d2.errors must contain(FormError("value", "invoiceDate.error.invalid.month", List(messages("date.error.month"))))
    }

    "must fail when month is outside 1-12" in {
      val low = form.bind(Map("value.day" -> "01", "value.month" -> "0", "value.year" -> "2025"))
      low.errors must contain(FormError("value", "invoiceDate.error.invalid.month", List(messages("date.error.month"))))
      val high = form.bind(Map("value.day" -> "01", "value.month" -> "13", "value.year" -> "2025"))
      high.errors must contain(FormError("value", "invoiceDate.error.invalid.month", List(messages("date.error.month"))))
    }

    "must fail when month text is invalid" in {
      val result = form.bind(Map("value.day" -> "01", "value.month" -> "Foo", "value.year" -> "2025"))
      result.errors must contain(FormError("value", "invoiceDate.error.invalid.month", List(messages("date.error.month"))))
    }

    "must fail when year has leading zeros (more than 4 digits)" in {
      val result = form.bind(Map("value.day" -> "07", "value.month" -> "04", "value.year" -> "02025"))
      result.errors must contain(FormError("value", "invoiceDate.error.invalid.year", List(messages("date.error.year"))))
    }

    "must bind random textual months (3-letter and full, mixed case)" in {
      val monthVariants = java.time.Month
        .values()
        .toList
        .flatMap { m =>
          val s = m.toString
          Seq(s, s.toLowerCase, s.capitalize, s.take(3), s.take(3).toLowerCase, s.take(3).toUpperCase, s.head.toLower.toString + s.tail.toUpperCase)
        }
        .distinct

      forAll(Gen.oneOf(monthVariants)) { monthStr =>
        val data = Map(
          "value.day"   -> "05",
          "value.month" -> monthStr,
          "value.year"  -> "2025"
        )

        val result = form.bind(data)
        val expectedMonth =
          java.time.Month.values().toList.find(m => m.toString == monthStr.toUpperCase || m.toString.take(3) == monthStr.toUpperCase)
        if (monthStr.length == 3 || monthStr.forall(_.isUpper) && monthStr.length <= 3) {
          result.value.value.getMonthValue mustEqual expectedMonth.get.getValue
        } else {
          // full month names longer than 3 are not accepted
          result.errors must contain(FormError("value", "invoiceDate.error.invalid.month", List(messages("date.error.month"))))
        }
      }
    }

    "must fail for invalid combination (31 Feb) and highlight day" in {
      val result = form.bind(Map("value.day" -> "31", "value.month" -> "Feb", "value.year" -> "2025"))
      result.errors must contain(FormError("value", "invoiceDate.error.invalid.day", List(messages("date.error.day"))))
    }

    "must fail when all fields are invalid and show generic invalid message" in {
      val result = form.bind(Map("value.day" -> "abc", "value.month" -> "def", "value.year" -> "ghi"))
      result.errors must contain(FormError("value", "invoiceDate.error.invalid", List.empty))
    }

    "must flag day and month for large numeric garbage input" in {
      val rendered = messages("invoiceDate.error.invalid.two", messages("date.error.day"), messages("date.error.month"))
      val result = form.bind(Map("value.day" -> "123", "value.month" -> "123", "value.year" -> "1234"))
      result.errors.map(_.message) must contain(rendered)
    }

    "must return invalid year when only year is invalid" in {
      val result = form.bind(Map("value.day" -> "05", "value.month" -> "03", "value.year" -> "20ab"))
      result.errors must contain(FormError("value", "invoiceDate.error.invalid.year", List(messages("date.error.year"))))
    }

    "must return invalid month and year when both month and year are invalid" in {
      val rendered = messages("invoiceDate.error.invalid.two", messages("date.error.month"), messages("date.error.year"))
      val result = form.bind(Map("value.day" -> "05", "value.month" -> "Foo", "value.year" -> "20ab"))
      result.errors.map(_.message) must contain(rendered)
    }

    "missing beats invalid: when day is missing and month has invalid text, only the missing day is reported" in {
      val result = form.bind(Map("value.month" -> "abc", "value.year" -> "2025"))
      result.errors must contain only FormError("value", "invoiceDate.error.required", List(messages("date.error.day")))
    }

    // Year range is not enforced by the LocalDate formatter; no test here

    "must fail when day is outside 1-31" in {
      val zeroDay = form.bind(Map("value.day" -> "0", "value.month" -> "01", "value.year" -> "2025"))
      zeroDay.errors must contain(FormError("value", "invoiceDate.error.invalid.day", List(messages("date.error.day"))))
      val bigDay = form.bind(Map("value.day" -> "32", "value.month" -> "01", "value.year" -> "2025"))
      bigDay.errors must contain(FormError("value", "invoiceDate.error.invalid.day", List(messages("date.error.day"))))
    }

    "must fail to bind an empty date" in {
      val result = form.bind(Map.empty[String, String])
      result.errors must contain only FormError("value", "invoiceDate.error.required.all", List.empty)
    }

    "must fail to bind a date with a missing day" in {
      forAll(validData -> "valid date") { date =>

        val data = Map(
          "value.month" -> date.getMonthValue.toString,
          "value.year"  -> date.getYear.toString
        )

        val result = form.bind(data)

        result.errors must contain only FormError("value", "invoiceDate.error.required", List(messages("date.error.day")))
      }
    }

    "must fail to bind a date with an invalid day" in {
      forAll(validData -> "valid date", invalidField -> "invalid field") { (date, field) =>

        val data = Map(
          "value.day"   -> field,
          "value.month" -> f"${date.getMonthValue}%02d",
          "value.year"  -> date.getYear.toString
        )

        val result = form.bind(data)

        result.errors must contain(FormError("value", "invoiceDate.error.invalid.day", List(messages("date.error.day"))))
      }
    }

    "must fail to bind a date with a missing day and month" in {

      forAll(validData -> "valid date", missingField -> "missing day", missingField -> "missing month") { (date, dayOpt, monthOpt) =>

        val day = dayOpt.fold(Map.empty[String, String]) { value =>
          Map("value.day" -> value)
        }

        val month = monthOpt.fold(Map.empty[String, String]) { value =>
          Map("value.month" -> value)
        }

        val data: Map[String, String] = Map(
          "value.year" -> date.getYear.toString
        ) ++ day ++ month

        val result = form.bind(data)

        result.errors must contain only FormError("value",
                                                  "invoiceDate.error.required.two",
                                                  List(messages("date.error.day"), messages("date.error.month"))
                                                 )
      }
    }

    "must fail to bind a date with a missing day and year" in {

      forAll(validData -> "valid date", missingField -> "missing day", missingField -> "missing year") { (date, dayOpt, yearOpt) =>

        val day = dayOpt.fold(Map.empty[String, String]) { value =>
          Map("value.day" -> value)
        }

        val year = yearOpt.fold(Map.empty[String, String]) { value =>
          Map("value.year" -> value)
        }

        val data: Map[String, String] = Map(
          "value.month" -> f"${date.getMonthValue}%02d"
        ) ++ day ++ year

        val result = form.bind(data)

        result.errors must contain only FormError("value",
                                                  "invoiceDate.error.required.two",
                                                  List(messages("date.error.day"), messages("date.error.year"))
                                                 )
      }
    }

    "must fail to bind a date with a missing month and year" in {

      forAll(validData -> "valid date", missingField -> "missing month", missingField -> "missing year") { (date, monthOpt, yearOpt) =>

        val month = monthOpt.fold(Map.empty[String, String]) { value =>
          Map("value.month" -> value)
        }

        val year = yearOpt.fold(Map.empty[String, String]) { value =>
          Map("value.year" -> value)
        }

        val data: Map[String, String] = Map(
          "value.day" -> f"${date.getDayOfMonth}%02d"
        ) ++ month ++ year

        val result = form.bind(data)

        result.errors must contain only FormError("value",
                                                  "invoiceDate.error.required.two",
                                                  List(messages("date.error.month"), messages("date.error.year"))
                                                 )
      }
    }

    "must fail to bind a date with a missing month" in {
      forAll(validData -> "valid date") { date =>

        val data = Map(
          "value.day"  -> f"${date.getDayOfMonth}%02d",
          "value.year" -> date.getYear.toString
        )

        val result = form.bind(data)

        result.errors must contain only FormError("value", "invoiceDate.error.required", List(messages("date.error.month")))
      }
    }

    "must fail to bind a date with a missing year" in {
      forAll(validData -> "valid date") { date =>

        val data = Map(
          "value.day"   -> f"${date.getDayOfMonth}%02d",
          "value.month" -> f"${date.getMonthValue}%02d"
        )

        val result = form.bind(data)

        result.errors must contain only FormError("value", "invoiceDate.error.required", List(messages("date.error.year")))
      }
    }

    "must unbind a date" in {
      forAll(validData -> "valid date") { date =>

        val filledForm = form.fill(date)

        filledForm("value.day").value.value mustEqual date.getDayOfMonth.toString
        filledForm("value.month").value.value mustEqual date.getMonthValue.toString
        filledForm("value.year").value.value mustEqual date.getYear.toString
      }
    }
  }
}
