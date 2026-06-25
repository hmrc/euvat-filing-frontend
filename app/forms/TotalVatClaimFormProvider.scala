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
import javax.inject.Inject
import play.api.data.Form
import play.api.data.validation.{Constraint, Invalid, Valid}

class TotalVatClaimFormProvider @Inject() extends Mappings {

  def apply(): Form[BigDecimal] =
    Form(
      "value" -> currency(
        "totalVatClaim.error.required",
        "totalVatClaim.error.invalidNumeric",
        "totalVatClaim.error.nonNumeric"
      )
        .verifying(
          Constraint[BigDecimal]("range") { amount =>
            val min = BigDecimal("-999999999.99")
            val max = BigDecimal("999999999.99")
            val minStr = f"$min%.2f"
            val maxStr = f"$min%.2f"
            if (amount < min) Invalid("totalVatClaim.error.belowMinimum", minStr, maxStr)
            else if (amount > max) Invalid("totalVatClaim.error.aboveMinimum", minStr, maxStr)
            else Valid
          }
        )
    )
}
