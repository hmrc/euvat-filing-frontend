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
import forms.mappings.Mappings
import play.api.data.FormError
import config.CurrencyFormatter.currencyFormat

class TotalPurchaseAmountBeforeVatFormProviderSpec extends CurrencyFieldBehaviours with StringFieldBehaviours {

  val requiredKey = "totalPurchaseAmountBeforeVat.error.required"
  val invalidNumeric = "totalPurchaseAmountBeforeVat.error.invalidNumeric"
  val nonNumeric = "totalPurchaseAmountBeforeVat.error.nonNumeric"
  val aboveMaximum = "totalPurchaseAmountBeforeVat.error.aboveMaximum"
  val max = BigDecimal("999999999.99")
  val maxLength = 15

  val form = new TotalPurchaseAmountBeforeVatFormProvider()()

  behave like currencyField(form, "value", FormError("value", nonNumeric), FormError("value", invalidNumeric))

  behave like currencyFieldWithMaximum(form, "value", max, FormError("value", aboveMaximum, Seq(f"${-max}%,1.2f", f"${max}%,1.2f")))

  behave like mandatoryField(form, "value", FormError("value", requiredKey))


  "must bind valid currency formats" in {
    val good = Seq("123", "1,234", "1,234.56", "0.99", "-12.34", "999999999.99")

    good.foreach { v =>
      val bound = form.bind(Map("value" -> v)).apply("value")
      bound.errors mustBe empty
      bound.value mustBe defined
    }
  }

  "must not bind values longer than max length including commas" in {
    val long = "1,234,567,890,123" // >15 chars
    val result = form.bind(Map("value" -> long)).apply("value")
    result.errors must contain only FormError("value", "totalPurchaseAmountBeforeVat.error.maxLength")
  }

  "must not bind invalid grouping like 12,34" in {
    val result = form.bind(Map("value" -> "12,34")).apply("value")
    result.errors must contain only FormError("value", invalidNumeric)
  }

  "must not bind values below minimum" in {
    val under = "-1000000000"
    val result = form.bind(Map("value" -> under)).apply("value")
    result.errors must contain only FormError("value", aboveMaximum, Seq(f"${-max}%,1.2f", f"${max}%,1.2f"))
  }

}
