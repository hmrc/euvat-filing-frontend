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
import forms.imports.TotalVatPaidImportFormProvider
import play.api.data.FormError

class TotalVatPaidImportFormProviderSpec extends CurrencyFieldBehaviours with StringFieldBehaviours {

  val requiredKey = "totalVatPaidImport.error.required"
  val invalidNumeric = "totalVatPaidImport.error.invalidNumeric"
  val nonNumeric = "totalVatPaidImport.error.nonNumeric"
  val aboveMaximum = "totalVatPaidImport.error.aboveMaximum"
  val max = BigDecimal("999999999.99")

  val form = new TotalVatPaidImportFormProvider()()

  behave like currencyField(form, "value", FormError("value", nonNumeric), FormError("value", invalidNumeric))

  behave like currencyFieldWithMaximum(form, "value", max, FormError("value", aboveMaximum))

  behave like mandatoryField(form, "value", FormError("value", requiredKey))

  "must bind negative numbers when allowed" in {
    val result = form.bind(Map("value" -> "-123.45")).apply("value")
    result.errors mustBe empty
    result.value.value mustBe "-123.45"
  }

  "must return grouping error for space-separated thousands when grouping enforced" in {
    val result = form.bind(Map("value" -> "1 234.56")).apply("value")
    result.errors mustEqual Seq(FormError("value", "totalVatPaidImport.error.invalidNumeric"))
  }

  "must bind a set of valid currency edge cases" in {
    val good = Seq("123", "1,234", "1,234.56", "0.99", "-12.34", "999999999.99")

    good.foreach { v =>
      val result = form.bind(Map("value" -> v)).apply("value")
      withClue(s"value: $v") {
        result.errors mustBe empty
        result.value mustBe defined
      }
    }
  }

  "must not bind values above maximum" in {
    val over = "1000000000000"
    val result = form.bind(Map("value" -> over)).apply("value")
    result.errors must contain only FormError("value", aboveMaximum)
  }

  "must bind large valid currency formats including 12-digit amounts" in {
    val good = Seq("123", "1,234", "1,234.56", "0.99", "-12.34", "999,999,999.99", "123,456,123.99")

    good.foreach { v =>
      val bound = form.bind(Map("value" -> v)).apply("value")
      bound.errors mustBe empty
      bound.value mustBe defined
    }
  }

}
