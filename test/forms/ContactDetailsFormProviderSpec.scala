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
  private val form = formProvider()

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

    "bind a valid first name" in {
      val result = form.bind(validData.updated(fieldName, "Jane")).apply(fieldName)
      result.value.value mustBe "Jane"
      result.errors mustBe empty
    }

    "bind a hyphenated first name" in {
      val result = form.bind(validData.updated(fieldName, "Mary-Jane")).apply(fieldName)
      result.value.value mustBe "Mary-Jane"
      result.errors mustBe empty
    }

    "bind a first name with an apostrophe" in {
      val result = form.bind(validData.updated(fieldName, "O'Neil")).apply(fieldName)
      result.value.value mustBe "O'Neil"
      result.errors mustBe empty
    }

    "bind to None when absent" in {
      val result = form.bind(validData - fieldName).value.value
      result.firstName mustBe None
    }

    "bind to None when blank" in {
      val result = form.bind(validData.updated(fieldName, "")).value.value
      result.firstName mustBe None
    }

    "reject a first name longer than 100 characters" in {
      val tooLong = "a" * 101
      val result = form.bind(validData.updated(fieldName, tooLong)).apply(fieldName)
      result.errors must contain only FormError(fieldName, "contactDetails.error.firstName.maxLength", Seq(100))
    }

    "reject a first name with disallowed characters" in {
      val result = form.bind(validData.updated(fieldName, "Jane@Doe")).apply(fieldName)
      result.errors must contain only FormError(
        fieldName,
        "contactDetails.error.firstName.format",
        Seq(formProvider.validateNameField)
      )
    }

    "reject a first name containing a comma or full stop (per spec regex)" in {
      val withPunctuation = "Jane, Sr."
      val result = form.bind(validData.updated(fieldName, withPunctuation)).apply(fieldName)
      result.errors must contain only FormError(
        fieldName,
        "contactDetails.error.firstName.format",
        Seq(formProvider.validateNameField)
      )
    }
  }

  ".contactLastName" - {

    val fieldName = "contactLastName"

    "bind a valid last name" in {
      val result = form.bind(validData.updated(fieldName, "Doe")).apply(fieldName)
      result.value.value mustBe "Doe"
      result.errors mustBe empty
    }

    "bind a hyphenated last name" in {
      val result = form.bind(validData.updated(fieldName, "Smith-Jones")).apply(fieldName)
      result.value.value mustBe "Smith-Jones"
      result.errors mustBe empty
    }

    "bind a last name with an apostrophe" in {
      val result = form.bind(validData.updated(fieldName, "O'Reilly")).apply(fieldName)
      result.value.value mustBe "O'Reilly"
      result.errors mustBe empty
    }

    "bind to None when absent" in {
      val result = form.bind(validData - fieldName).value.value
      result.lastName mustBe None
    }

    "bind to None when blank" in {
      val result = form.bind(validData.updated(fieldName, "")).value.value
      result.lastName mustBe None
    }

    "reject a last name longer than 100 characters" in {
      val tooLong = "z" * 101
      val result = form.bind(validData.updated(fieldName, tooLong)).apply(fieldName)
      result.errors must contain only FormError(fieldName, "contactDetails.error.lastName.maxLength", Seq(100))
    }

    "reject a last name with disallowed characters" in {
      val result = form.bind(validData.updated(fieldName, "<script>")).apply(fieldName)
      result.errors must contain only FormError(
        fieldName,
        "contactDetails.error.lastName.format",
        Seq(formProvider.validateNameField)
      )
    }
  }

  ".contactTelephone" - {

    val fieldName = "contactTelephone"

    "bind a valid UK telephone number" in {
      val result = form.bind(validData.updated(fieldName, "07700900000")).apply(fieldName)
      result.value.value mustBe "07700900000"
      result.errors mustBe empty
    }

    "bind a valid international telephone number with plus prefix" in {
      val result = form.bind(validData.updated(fieldName, "+448081570192")).apply(fieldName)
      result.value.value mustBe "+448081570192"
      result.errors mustBe empty
    }

    "bind a short telephone number per spec (1-20 digits accepted)" in {
      val result = form.bind(validData.updated(fieldName, "12345")).apply(fieldName)
      result.value.value mustBe "12345"
      result.errors mustBe empty
    }

    "bind to None when absent" in {
      val result = form.bind(validData - fieldName).value.value
      result.telephone mustBe None
    }

    "bind to None when blank" in {
      val result = form.bind(validData.updated(fieldName, "")).value.value
      result.telephone mustBe None
    }

    "reject a telephone number with letters" in {
      val result = form.bind(validData.updated(fieldName, "0770call_me")).apply(fieldName)
      result.errors must contain only FormError(
        fieldName,
        "contactDetails.error.telephone.format",
        Seq(formProvider.validateTelephoneNumber)
      )
    }

    "reject a telephone number with whitespace per spec" in {
      val result = form.bind(validData.updated(fieldName, "01632 960 001")).apply(fieldName)
      result.errors must contain only FormError(
        fieldName,
        "contactDetails.error.telephone.format",
        Seq(formProvider.validateTelephoneNumber)
      )
    }

    "reject a telephone number longer than 20 digits" in {
      val tooLong = "1" * 21
      val result = form.bind(validData.updated(fieldName, tooLong)).apply(fieldName)
      result.errors must contain only FormError(
        fieldName,
        "contactDetails.error.telephone.format",
        Seq(formProvider.validateTelephoneNumber)
      )
    }
  }

  "ContactDetailsFormProvider" - {

    "apply ContactDetails correctly (bind → model round-trip)" in {
      val bound = form.bind(validData).value.value
      bound mustBe modelFromValidData
    }

    "unapply ContactDetails correctly (fill → form round-trip)" in {
      val filled = form.fill(modelFromValidData)
      filled("contactEmail").value.value mustBe "test@example.com"
      filled("contactFirstName").value.value mustBe "Jane"
      filled("contactLastName").value.value mustBe "Doe"
      filled("contactTelephone").value.value mustBe "07700900000"
    }

    "unapply ContactDetails with missing optionals" in {
      val model = ContactDetails(email = "a@b.co", firstName = None, lastName = None, telephone = None)
      val filled = form.fill(model)
      filled("contactEmail").value.value mustBe "a@b.co"
      filled("contactFirstName").value mustBe None
      filled("contactLastName").value mustBe None
      filled("contactTelephone").value mustBe None
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

      "for hyphen-starting domain user@-domain.com" in {
        "user@-domain.com" mustNot fullyMatch regex formProvider.validateEmailAddress
      }

      "for underscore-starting domain user@_domain.com" in {
        "user@_domain.com" mustNot fullyMatch regex formProvider.validateEmailAddress
      }

      "for dot-starting domain user@.domain.com" in {
        "user@.domain.com" mustNot fullyMatch regex formProvider.validateEmailAddress
      }
    }
  }
}
