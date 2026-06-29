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

class SupplierVatRegistrationNumberFormProviderSpec extends StringFieldBehaviours with FieldBehaviours {

  private val formProvider = new SupplierVatRegistrationNumberFormProvider()
  private val form = formProvider()

  ".value" - {

    val fieldName = "value"

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, "supplierVatRegistrationNumber.error.required")
    )

    behave like fieldWithMaxLength(
      form,
      fieldName,
      maxLength   = 12,
      lengthError = FormError(fieldName, "supplierVatRegistrationNumber.error.length", Seq(12))
    )

    "bind valid data" in {
      val validValues = Seq("FR123456789", "DE123", "123456789012", "A+*")
      validValues.foreach { v =>
        val result = form.bind(Map(fieldName -> v)).apply(fieldName)
        result.value.value mustBe v
        result.errors mustBe empty
      }
    }
  }

}
