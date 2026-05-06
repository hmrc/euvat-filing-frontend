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
import models.PurchaseType
import play.api.data.FormError

class PurchaseTypeFormProviderSpec extends FieldBehaviours {

  private val form = new PurchaseTypeFormProvider().apply()

  private val fieldName   = "value"
  private val errorKey    = "purchaseType.error.required"

  ".value" - {

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, errorKey)
    )

    "bind every defined PurchaseType value" in {
      PurchaseType.values.foreach { value =>
        val result = form.bind(Map(fieldName -> value.toString))
        result.errors mustBe empty
        result.value.value mustEqual value
      }
    }

    "fail to bind a value not in the enum with the required-error key" in {
      val result = form.bind(Map(fieldName -> "notARealOption")).apply(fieldName)
      result.errors mustEqual Seq(FormError(fieldName, errorKey))
    }
  }
}
