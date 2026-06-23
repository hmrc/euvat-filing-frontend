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

import forms.behaviours.{CurrencyFieldBehaviours, StringFieldBehaviours}
import play.api.data.FormError

class TotalVatPaidFormProviderSpec extends CurrencyFieldBehaviours with StringFieldBehaviours {

  val requiredKey = "totalVatPaid.error.required"
  val invalidNumeric = "totalVatPaid.error.invalidNumeric"
  val nonNumeric = "totalVatPaid.error.nonNumeric"
  val aboveMaximum = "totalVatPaid.error.aboveMaximum"
  val max = BigDecimal("999999999.99")
  val maxLength = 15

  val form = new TotalVatPaidFormProvider()()

  behave like currencyField(form, "value", FormError("value", nonNumeric), FormError("value", invalidNumeric))

  val fmt = (amt: BigDecimal) => f"$amt%,1.2f".replace(".00", "")

  behave like currencyFieldWithMaximum(form, "value", max, FormError("value", aboveMaximum, Seq(fmt(-max), fmt(max))))

  behave like mandatoryField(form, "value", FormError("value", requiredKey))

  "must bind negative numbers when allowed" in {
    val result = form.bind(Map("value" -> "-123.45")).apply("value")
    result.errors mustBe empty
    result.value.value mustBe "-123.45"
  }

  "must return max length error when input is too long" in {
    val long = "1234567890123456" // 16 chars, max is 15
    val result = form.bind(Map("value" -> long)).apply("value")
    result.errors mustEqual Seq(FormError("value", "totalVatPaid.error.maxLength"))
  }

  "must return grouping error for space-separated thousands when grouping enforced" in {
    val result = form.bind(Map("value" -> "1 234.56")).apply("value")
    result.errors mustEqual Seq(FormError("value", "totalVatPaid.error.invalidNumeric"))
  }

  "must bind a set of valid currency edge cases" in {
    val good = Seq("123", "1,234", "1,234.56", "0.99", "-12.34", "999999999.99")

    good.foreach { v =>
      val result = form.bind(Map("value" -> v)).apply("value")
      withClue(s"value: $v") {
        result.errors mustBe empty
        result.value.value mustBe v
      }
    }
  }

}
