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

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.data.FormError
import play.api.i18n.Messages
import play.api.test.Helpers.stubMessages

import java.time.LocalDate

class LocalDateFormatterSpec extends AnyFreeSpec with Matchers {

  private implicit val messages: Messages = stubMessages()

  private val formatter = new LocalDateFormatter(
    invalidKey      = "invoiceDate.error.invalid",
    allRequiredKey  = "invoiceDate.error.required.all",
    twoRequiredKey  = "invoiceDate.error.required.two",
    requiredKey     = "invoiceDate.error.required",
    usePerFieldKeys = true
  )

  "Month parsing" - {
    "binds numeric months with leading zeros and two-digit day" in {
      val result = formatter.bind("value", Map("value.day" -> "01", "value.month" -> "02", "value.year" -> "2025"))
      result.isRight mustBe true
      result.toOption.get.getMonthValue mustEqual 2
    }

    "binds three-letter mixed-case month with two-digit day" in {
      val result = formatter.bind("value", Map("value.day" -> "03", "value.month" -> "fEb", "value.year" -> "2025"))
      result.isRight mustBe true
      result.toOption.get.getMonthValue mustEqual 2
    }

    "fails for full month mixed-case (only 3-letter allowed)" in {
      val result = formatter.bind("value", Map("value.day" -> "04", "value.month" -> "fEbRuArY", "value.year" -> "2025"))
      result mustBe Left(List(FormError("value", "invoiceDate.error.invalid.month", List(messages("date.error.month")))))
    }

    "fails for invalid month text" in {
      val result = formatter.bind("value", Map("value.day" -> "01", "value.month" -> "Foo", "value.year" -> "2025"))
      result mustBe Left(List(FormError("value", "invoiceDate.error.invalid.month", List(messages("date.error.month")))))
    }

    "flags day and month for large numeric garbage input" in {
      val rendered = messages("invoiceDate.error.invalid.two", messages("date.error.day"), messages("date.error.month"))
      val result = formatter.bind("value", Map("value.day" -> "123", "value.month" -> "123", "value.year" -> "1234"))
      result mustBe Left(List(FormError("value", rendered, List(messages("date.error.day"), messages("date.error.month")))))
    }

    "flags day and month for textual invalid day and month" in {
      val rendered = messages("invoiceDate.error.invalid.two", messages("date.error.day"), messages("date.error.month"))
      val result = formatter.bind("value", Map("value.day" -> "abc", "value.month" -> "def", "value.year" -> "2025"))
      result mustBe Left(List(FormError("value", rendered, List(messages("date.error.day"), messages("date.error.month")))))
    }

    "marks year invalid when it contains non-numeric characters even if digits would allow 29 Feb" in {
      val result = formatter.bind("value", Map("value.day" -> "29", "value.month" -> "02", "value.year" -> "2024abc"))
      result mustBe Left(List(FormError("value", "invoiceDate.error.invalid.year", List(messages("date.error.year")))))
    }

    "marks day and year invalid when inferred year is non-leap" in {
      val rendered = messages("invoiceDate.error.invalid.two", messages("date.error.day"), messages("date.error.year"))
      val result = formatter.bind("value", Map("value.day" -> "29", "value.month" -> "02", "value.year" -> "2025abc"))
      result mustBe Left(List(FormError("value", rendered, List(messages("date.error.day"), messages("date.error.year")))))
    }

    "rejects messy non-4-digit year input like '231 123 123' as invalid year" in {
      val result = formatter.bind("value", Map("value.day" -> "01", "value.month" -> "01", "value.year" -> "231 123 123"))
      result mustBe Left(List(FormError("value", "invoiceDate.error.invalid.year", List(messages("date.error.year")))))
    }

    "rejects 3-digit and 5-digit years as invalid" in {
      val result3 = formatter.bind("value", Map("value.day" -> "01", "value.month" -> "01", "value.year" -> "123"))
      result3 mustBe Left(List(FormError("value", "invoiceDate.error.invalid.year", List(messages("date.error.year")))))

      val result5 = formatter.bind("value", Map("value.day" -> "01", "value.month" -> "01", "value.year" -> "12345"))
      result5 mustBe Left(List(FormError("value", "invoiceDate.error.invalid.year", List(messages("date.error.year")))))
    }
  }
}
