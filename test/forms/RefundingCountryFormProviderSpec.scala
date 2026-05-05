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

import forms.behaviours.{FieldBehaviours, StringFieldBehaviours}
import play.api.data.FormError

class RefundingCountryFormProviderSpec extends StringFieldBehaviours with FieldBehaviours {

  private val formProvider = new RefundingCountryFormProvider()
  private val allowed = Set("DE", "Germany", "United Kingdom", "Some Country")
  private val form = formProvider(allowed)

  ".value" - {

    val fieldName = "value"

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, "refundingCountry.error.required")
    )

    // max length constraint removed per review

    "bind valid data" in {
      val validValues = Seq("DE", "Germany", "United Kingdom", "Some Country")
      validValues.foreach { v =>
        val result = form.bind(Map(fieldName -> v)).apply(fieldName)
        result.value.value mustBe v
        result.errors mustBe empty
      }
    }
  }

}
