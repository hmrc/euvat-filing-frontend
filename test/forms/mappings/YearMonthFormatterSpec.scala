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
import play.api.test.Helpers.*
import java.time.YearMonth

class YearMonthFormatterSpec extends SpecBase {

  "YearMonthFormatter" - {

    "Binding valid data" - {

      "must bind numeric month and year" in {
        val application = applicationBuilder().build()
        running(application) {
          implicit val msgs: Messages = messages(application)
          val formatter = new YearMonthFormatter("invalid", "all", "two", "req")
          val data = Map("start.month" -> "03", "start.year" -> "2010")
          val res = formatter.bind("start", data)
          res.isRight `mustBe` true
          res.toOption.get mustEqual YearMonth.of(2010, 3)
        }
      }

      "must bind month by short name" in {
        val application = applicationBuilder().build()
        running(application) {
          implicit val msgs: Messages = messages(application)
          val formatter = new YearMonthFormatter("invalid", "all", "two", "req")
          val data = Map("m.month" -> "Mar", "m.year" -> "2010")
          formatter.bind("m", data).toOption.get mustEqual YearMonth.of(2010, 3)
        }
      }

      "must bind month by full name" in {
        val application = applicationBuilder().build()
        running(application) {
          implicit val msgs: Messages = messages(application)
          val formatter = new YearMonthFormatter("invalid", "all", "two", "req")
          val data = Map("m.month" -> "March", "m.year" -> "2010")
          formatter.bind("m", data).toOption.get mustEqual YearMonth.of(2010, 3)
        }
      }
    }

    "Missing field errors" - {

      "must return two.year error when only month is present" in {
        val application = applicationBuilder().build()
        running(application) {
          implicit val msgs: Messages = messages(application)
          val formatter = new YearMonthFormatter("invalid", "all", "two", "req")
          val data = Map("x.month" -> "03")
          val res = formatter.bind("x", data)
          res.isLeft `mustBe` true
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
          res.isLeft `mustBe` true
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
          res.isLeft `mustBe` true
          res.swap.toOption.get.head.message mustEqual "all"
        }
      }
    }

    "Invalid format errors" - {

      "must return invalid.year when only year is invalid" in {
        val application = applicationBuilder().build()
        running(application) {
          implicit val msgs: Messages = messages(application)
          val formatter = new YearMonthFormatter("invalid", "all", "two", "req")
          val data = Map("k.month" -> "03", "k.year" -> "20.10")
          val res = formatter.bind("k", data)
          res.isLeft `mustBe` true
          res.swap.toOption.get.head.message mustEqual "invalid.year"
          res.swap.toOption.get.head.key mustEqual "k.year"
        }
      }

      "must return invalid.month when only month is invalid" in {
        val application = applicationBuilder().build()
        running(application) {
          implicit val msgs: Messages = messages(application)
          val formatter = new YearMonthFormatter("invalid", "all", "two", "req")
          val data = Map("x.month" -> "abc", "x.year" -> "2010")
          val res = formatter.bind("x", data)
          res.isLeft `mustBe` true
          res.swap.toOption.get.head.message mustEqual "invalid.month"
          res.swap.toOption.get.head.key mustEqual "x.month"
        }
      }

      "must return invalid when both month and year are invalid" in {
        val application = applicationBuilder().build()
        running(application) {
          implicit val msgs: Messages = messages(application)
          val formatter = new YearMonthFormatter("invalid", "all", "two", "req")
          val data = Map("x.month" -> "abc", "x.year" -> "abc")
          val res = formatter.bind("x", data)
          res.isLeft `mustBe` true
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
          res.isLeft `mustBe` true
          res.swap.toOption.get.head.message mustEqual "invalid"
        }
      }
      "must return invalid.year when year is greater than 9999" in {
        val application = applicationBuilder().build()
        running(application) {
          implicit val msgs: Messages = messages(application)
          val formatter = new YearMonthFormatter("invalid", "all", "two", "req")
          val data = Map("k.month" -> "03", "k.year" -> "10000")
          val res = formatter.bind("k", data)
          res.isLeft mustBe true
          res.swap.toOption.get.head.message mustEqual "invalid.year"
          res.swap.toOption.get.head.key mustEqual "k.year"
        }
      }
    }
  }
}
