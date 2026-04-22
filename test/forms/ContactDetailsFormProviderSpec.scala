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

class ContactDetailsFormProviderSpec extends StringFieldBehaviours with FieldBehaviours {

  private val form = new ContactDetailsFormProvider()()

  private val validData = Map(
    "contactEmail"     -> "test@example.com",
    "contactFirstName" -> "Jane",
    "contactLastName"  -> "Doe",
    "contactTelephone" -> "07700900000"
  )

  ".contactEmail" - {

    val fieldName = "contactEmail"

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, "contactDetails.error.email.required")
    )

    "bind a valid email address" in {
      val result = form.bind(validData).apply(fieldName)
      result.value.value mustBe "test@example.com"
      result.errors mustBe empty
    }

    "reject an invalid email format" in {
      val result = form.bind(validData.updated(fieldName, "not-an-email")).apply(fieldName)
      result.errors must contain only FormError(fieldName, "contactDetails.error.email.invalidFormat")
    }

    behave like fieldWithMaxLength(
      form,
      fieldName,
      maxLength   = 100,
      lengthError = FormError(fieldName, "contactDetails.error.email.invalidFormat")
    )
  }

  ".contactFirstName" - {

    "bind when present" in {
      val result = form.bind(validData).apply("contactFirstName")
      result.errors mustBe empty
    }

    "bind when absent" in {
      val result = form.bind(validData - "contactFirstName")
      result.errors mustBe empty
    }
  }

  ".contactLastName" - {

    "bind when present" in {
      val result = form.bind(validData).apply("contactLastName")
      result.errors mustBe empty
    }

    "bind when absent" in {
      val result = form.bind(validData - "contactLastName")
      result.errors mustBe empty
    }
  }

  ".contactTelephone" - {

    "bind when present" in {
      val result = form.bind(validData).apply("contactTelephone")
      result.errors mustBe empty
    }

    "bind when absent" in {
      val result = form.bind(validData - "contactTelephone")
      result.errors mustBe empty
    }
  }
}
