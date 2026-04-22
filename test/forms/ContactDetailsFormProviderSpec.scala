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
import models.ContactDetails
import play.api.data.FormError

class ContactDetailsFormProviderSpec extends StringFieldBehaviours with FieldBehaviours {

  private val formProvider = new ContactDetailsFormProvider()
  private val form         = formProvider()

  private val validData = Map(
    "contactEmail"     -> "test@example.com",
    "contactFirstName" -> "Jane",
    "contactLastName"  -> "Doe",
    "contactTelephone" -> "07700900000"
  )

  private val modelFromValidData = ContactDetails(
    email     = "test@example.com",
    firstName = Some("Jane"),
    lastName  = Some("Doe"),
    telephone = Some("07700900000")
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
      result.errors must contain only FormError(
        fieldName,
        "contactDetails.error.email.invalidFormat",
        Seq(formProvider.validateEmailAddress)
      )
    }

    behave like fieldWithMaxLength(
      form,
      fieldName,
      maxLength   = 100,
      lengthError = FormError(fieldName, "contactDetails.error.email.invalidFormat", Seq(100))
    )
  }

  ".contactFirstName" - {

    val fieldName = "contactFirstName"

    behave like fieldThatBindsValidData(form, fieldName, nonEmptyString)

    "bind to None when absent" in {
      val result = form.bind(validData - fieldName).value.value
      result.firstName mustBe None
    }

    "bind to None when blank" in {
      val result = form.bind(validData.updated(fieldName, "")).value.value
      result.firstName mustBe None
    }
  }

  ".contactLastName" - {

    val fieldName = "contactLastName"

    behave like fieldThatBindsValidData(form, fieldName, nonEmptyString)

    "bind to None when absent" in {
      val result = form.bind(validData - fieldName).value.value
      result.lastName mustBe None
    }

    "bind to None when blank" in {
      val result = form.bind(validData.updated(fieldName, "")).value.value
      result.lastName mustBe None
    }
  }

  ".contactTelephone" - {

    val fieldName = "contactTelephone"

    behave like fieldThatBindsValidData(form, fieldName, nonEmptyString)

    "bind to None when absent" in {
      val result = form.bind(validData - fieldName).value.value
      result.telephone mustBe None
    }

    "bind to None when blank" in {
      val result = form.bind(validData.updated(fieldName, "")).value.value
      result.telephone mustBe None
    }
  }

  "ContactDetailsFormProvider" - {

    "apply ContactDetails correctly (bind → model round-trip)" in {
      val bound = form.bind(validData).value.value
      bound mustBe modelFromValidData
    }

    "unapply ContactDetails correctly (fill → form round-trip)" in {
      val filled = form.fill(modelFromValidData)
      filled("contactEmail").value.value     mustBe "test@example.com"
      filled("contactFirstName").value.value mustBe "Jane"
      filled("contactLastName").value.value  mustBe "Doe"
      filled("contactTelephone").value.value mustBe "07700900000"
    }

    "unapply ContactDetails with missing optionals" in {
      val model  = ContactDetails(email = "a@b.co", firstName = None, lastName = None, telephone = None)
      val filled = form.fill(model)
      filled("contactEmail").value.value mustBe "a@b.co"
      filled("contactFirstName").value   mustBe None
      filled("contactLastName").value    mustBe None
      filled("contactTelephone").value   mustBe None
    }
  }

  "validateEmailAddress regex" - {

    "return valid" - {
      "for test@example.com" in {
        "test@example.com" must fullyMatch regex formProvider.validateEmailAddress
      }

      "for firstname.lastname@domain.com" in {
        "firstname.lastname@domain.com" must fullyMatch regex formProvider.validateEmailAddress
      }

      "for firstname-lastname@domain.co.uk" in {
        "firstname-lastname@domain.co.uk" must fullyMatch regex formProvider.validateEmailAddress
      }

      "for user_name@sub-domain.example.com" in {
        "user_name@sub-domain.example.com" must fullyMatch regex formProvider.validateEmailAddress
      }
    }

    "return invalid" - {
      "for abc@gmail (no TLD)" in {
        "abc@gmail" mustNot fullyMatch regex formProvider.validateEmailAddress
      }

      "for no-at-symbol.example.com" in {
        "no-at-symbol.example.com" mustNot fullyMatch regex formProvider.validateEmailAddress
      }

      "for spaces in local@domain.com" in {
        "spaces in local@domain.com" mustNot fullyMatch regex formProvider.validateEmailAddress
      }

      "for pipe-in-domain@example.com|gov.uk" in {
        "pipe-in-domain@example.com|gov.uk" mustNot fullyMatch regex formProvider.validateEmailAddress
      }

      "for brackets(in)local@domain.com" in {
        "brackets(in)local@domain.com" mustNot fullyMatch regex formProvider.validateEmailAddress
      }

      "for comma-in-domain@domain,gov.uk" in {
        "comma-in-domain@domain,gov.uk" mustNot fullyMatch regex formProvider.validateEmailAddress
      }

      "for single-char TLD user@domain.a" in {
        "user@domain.a" mustNot fullyMatch regex formProvider.validateEmailAddress
      }
    }
  }
}
