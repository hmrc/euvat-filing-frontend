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

import base.SpecBase
import play.api.i18n.Messages
import play.api.test.Helpers._
import java.time.YearMonth

class YearMonthFormatterSpec extends SpecBase {

  "YearMonthFormatter" - {

    "must bind numeric month and year" in {
      val application = applicationBuilder().build()

      running(application) {
        implicit val msgs: Messages = messages(application)
        val formatter = new YearMonthFormatter(
          invalidKey = "invalid",
          allRequiredKey = "all",
          twoRequiredKey = "two",
          requiredKey = "req"
        )

        val data = Map("start.month" -> "03", "start.year" -> "2010")

        val res = formatter.bind("start", data)
        res.isRight mustBe true
        res.toOption.get mustEqual YearMonth.of(2010, 3)
      }
    }

    "must bind month by short or full name" in {
      val application = applicationBuilder().build()

      running(application) {
        implicit val msgs: Messages = messages(application)
        val formatter = new YearMonthFormatter("invalid", "all", "two", "req")

        val short = Map("m.month" -> "Mar", "m.year" -> "2010")
        val full = Map("m.month" -> "March", "m.year" -> "2010")

        formatter.bind("m", short).toOption.get mustEqual YearMonth.of(2010, 3)
        formatter.bind("m", full).toOption.get mustEqual YearMonth.of(2010, 3)
      }
    }

    "must return two-required when only one field present" in {
      val application = applicationBuilder().build()

      running(application) {
        implicit val msgs: Messages = messages(application)
        val formatter = new YearMonthFormatter("invalid", "all", "two", "req")

        val data = Map("x.month" -> "03")

        val res = formatter.bind("x", data)
        res.isLeft mustBe true
        res.swap.toOption.get.head.message mustEqual "two.year"
      }
    }

    "must return two.month error when only year is present" in {
      val application = applicationBuilder().build()

      running(application) {
        implicit val msgs: Messages = messages(application)
        val formatter = new YearMonthFormatter("invalid", "all", "two", "req")

        val data = Map("x.year" -> "2010")

        val res = formatter.bind("x", data)
        res.isLeft mustBe true
        res.swap.toOption.get.head.message mustEqual "two.month"
      }
    }

    "must return all-required error when both fields are missing" in {
      val application = applicationBuilder().build()

      running(application) {
        implicit val msgs: Messages = messages(application)
        val formatter = new YearMonthFormatter("invalid", "all", "two", "req")

        val data = Map.empty[String, String]

        val res = formatter.bind("x", data)
        res.isLeft mustBe true
        res.swap.toOption.get.head.message mustEqual "all"
      }
    }

    "must return invalid when non-numeric year provided" in {
      val application = applicationBuilder().build()

      running(application) {
        implicit val msgs: Messages = messages(application)
        val formatter = new YearMonthFormatter("invalid", "all", "two", "req")

        val data = Map("k.month" -> "03", "k.year" -> "20.10")

        val res = formatter.bind("k", data)
        res.isLeft mustBe true
        res.swap.toOption.get.head.message mustEqual "invalid"
      }
    }

    "must return invalid when month is out of range" in {
      val application = applicationBuilder().build()

      running(application) {
        implicit val msgs: Messages = messages(application)

        val formatter = new YearMonthFormatter("invalid", "all", "two", "req")

        val data = Map("z.month" -> "13", "z.year" -> "2010")

        val res = formatter.bind("z", data)
        res.isLeft mustBe true
        res.swap.toOption.get.head.message mustEqual "invalid"
      }
    }
  }
}
