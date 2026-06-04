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

import forms.mappings.Mappings
import models.SupplierAddress
import play.api.data.Form
import play.api.data.Forms.{mapping, optional}
import play.api.data.validation.{Constraint, Invalid, Valid}
import play.api.i18n.{Lang, MessagesApi}

import javax.inject.Inject

class SupplierAddressFormProvider @Inject() (messagesApi: MessagesApi) extends Mappings {

  val addressLineMaxLength: Int = 35

  private val defaultMessages = messagesApi.preferred(Seq(Lang("en")))

  private def fieldMaxLengthConstraint(labelKey: String, errorKey: String): Constraint[String] =
    Constraint { str =>
      if (str.length <= addressLineMaxLength) Valid
      else Invalid("supplierAddress.error.maxLength.withLabel", defaultMessages(labelKey), defaultMessages("supplierAddress.error.maxLength"))
    }

  def apply(): Form[SupplierAddress] =
    Form(
      mapping(
        "addressLine1" -> text("supplierAddress.error.line1.required")
          .verifying(fieldMaxLengthConstraint("supplierAddress.line1.label", "supplierAddress.error.line1.maxLength")),
        "addressLine2" -> optional(
          text().verifying(fieldMaxLengthConstraint("supplierAddress.line2.label.short", "supplierAddress.error.line2.maxLength"))
        ),
        "addressLine3" -> optional(
          text().verifying(fieldMaxLengthConstraint("supplierAddress.line3.label.short", "supplierAddress.error.line3.maxLength"))
        )
      )(SupplierAddress.apply)(o => Some(Tuple.fromProductTyped(o)))
    )
}
