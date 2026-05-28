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

import forms.behaviours.FieldBehaviours
import models.SupplierAddress
import play.api.data.FormError
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers.*

class SupplierAddressFormProviderSpec extends FieldBehaviours {

  private val application  = GuiceApplicationBuilder().build()
  private val messagesApi = application.injector.instanceOf[play.api.i18n.MessagesApi]
  private implicit val msgs: play.api.i18n.Messages = play.api.i18n.MessagesImpl(play.api.i18n.Lang("en"), messagesApi)

  private val formProvider = application.injector.instanceOf[SupplierAddressFormProvider]
  private val form         = formProvider()

  private val validData = Map(
    "addressLine1" -> "1 High Street",
    "addressLine2" -> "Apartment 3",
    "addressLine3" -> "London"
  )

  private val modelFromValidData = SupplierAddress(
    line1 = "1 High Street",
    line2 = Some("Apartment 3"),
    line3 = Some("London")
  )

  ".addressLine1" - {

    val fieldName = "addressLine1"

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, "supplierAddress.error.line1.required")
    )

    "bind a valid value" in {
      val result = form.bind(validData).apply(fieldName)
      result.value.value mustBe "1 High Street"
      result.errors mustBe empty
    }

    "reject a value longer than 35 characters" in {
      val tooLong = "a" * 36
      val result = form.bind(validData.updated(fieldName, tooLong)).apply(fieldName)
      result.errors must contain only FormError(
        fieldName,
        "supplierAddress.error.line1.maxLength",
        Seq(msgs("supplierAddress.line1.label"), msgs("supplierAddress.error.maxLength"))
      )
    }

    "bind a value of exactly 35 characters" in {
      val maxLength = "a" * 35
      val result = form.bind(validData.updated(fieldName, maxLength)).apply(fieldName)
      result.value.value mustBe maxLength
      result.errors mustBe empty
    }
  }

  ".addressLine2" - {

    val fieldName = "addressLine2"

    "bind to None when absent" in {
      val result = form.bind(validData - fieldName).value.value
      result.line2 mustBe None
    }

    "bind to None when blank" in {
      val result = form.bind(validData.updated(fieldName, "")).value.value
      result.line2 mustBe None
    }

    "reject a value longer than 35 characters" in {
      val tooLong = "b" * 36
      val result = form.bind(validData.updated(fieldName, tooLong)).apply(fieldName)
      result.errors must contain only FormError(
        fieldName,
        "supplierAddress.error.line2.maxLength",
        Seq(msgs("supplierAddress.line2.label"), msgs("supplierAddress.error.maxLength"))
      )
    }
  }

  ".addressLine3" - {

    val fieldName = "addressLine3"

    "bind to None when absent" in {
      val result = form.bind(validData - fieldName).value.value
      result.line3 mustBe None
    }

    "bind to None when blank" in {
      val result = form.bind(validData.updated(fieldName, "")).value.value
      result.line3 mustBe None
    }

    "reject a value longer than 35 characters" in {
      val tooLong = "c" * 36
      val result = form.bind(validData.updated(fieldName, tooLong)).apply(fieldName)
      result.errors must contain only FormError(
        fieldName,
        "supplierAddress.error.line3.maxLength",
        Seq(msgs("supplierAddress.line3.label"), msgs("supplierAddress.error.maxLength"))
      )
    }
  }

  "SupplierAddressFormProvider" - {

    "apply SupplierAddress correctly (bind → model round-trip)" in {
      val bound = form.bind(validData).value.value
      bound mustBe modelFromValidData
    }

    "unapply SupplierAddress correctly (fill → form round-trip)" in {
      val filled = form.fill(modelFromValidData)
      filled("addressLine1").value.value mustBe "1 High Street"
      filled("addressLine2").value.value mustBe "Apartment 3"
      filled("addressLine3").value.value mustBe "London"
    }

    "unapply SupplierAddress with missing optionals" in {
      val model = SupplierAddress(line1 = "1 High Street", line2 = None, line3 = None)
      val filled = form.fill(model)
      filled("addressLine1").value.value mustBe "1 High Street"
      filled("addressLine2").value mustBe None
      filled("addressLine3").value mustBe None
    }
  }
}
