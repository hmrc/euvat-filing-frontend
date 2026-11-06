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

package forms.bankdetails

import forms.Validation.{bicRegex, currencyCodeRegex, ibanRegex, nameOnAccountRegex}
import forms.mappings.Mappings
import models.BankDetails
import play.api.data.Form
import play.api.data.Forms.*
import play.api.data.validation.{Constraint, Invalid, Valid}

import java.util.Locale
import javax.inject.Inject

class BankDetailsFormProvider @Inject() extends Mappings {

  private def normaliseBic(value: String): String =
    value.trim

  private def normaliseIban(value: String): String =
    value.trim

  private def normaliseCurrencyCode(value: String): String =
    value.trim
      .toUpperCase(Locale.ROOT)

  private def requiredMaxLengthAndPattern(
    requiredKey: String,
    lengthKey: String,
    invalidKey: String,
    maximumLength: Int,
    pattern: String
  ): Constraint[String] =
    Constraint { value =>
      if (value.trim.isEmpty) {
        Invalid(requiredKey)
      } else if (value.length > maximumLength) {
        Invalid(lengthKey, maximumLength)
      } else if (!value.matches(pattern)) {
        Invalid(invalidKey)
      } else {
        Valid
      }
    }

  private def requiredAndPattern(
    requiredKey: String,
    invalidKey: String,
    pattern: String
  ): Constraint[String] =
    Constraint { value =>
      if (value.isEmpty) {
        Invalid(requiredKey)
      } else if (!value.matches(pattern)) {
        Invalid(invalidKey)
      } else {
        Valid
      }
    }

  def apply(): Form[BankDetails] =
    Form(
      mapping(
        "NameOnTheAccount" ->
          play.api.data.Forms.text
            .verifying(
              requiredMaxLengthAndPattern(
                requiredKey   = "bankDetails.internationalBankAccountDetails.error.NameOnTheAccount.required",
                lengthKey     = "bankDetails.internationalBankAccountDetails.error.NameOnTheAccount.length",
                invalidKey    = "bankDetails.internationalBankAccountDetails.error.NameOnTheAccount.invalid",
                maximumLength = 70,
                pattern       = nameOnAccountRegex
              )
            ),
        "BIC" ->
          play.api.data.Forms.text
            .transform(normaliseBic, identity)
            .verifying(
              requiredMaxLengthAndPattern(
                requiredKey   = "bankDetails.internationalBankAccountDetails.error.BIC.required",
                lengthKey     = "bankDetails.internationalBankAccountDetails.error.BIC.length",
                invalidKey    = "bankDetails.internationalBankAccountDetails.error.BIC.invalid",
                maximumLength = 11,
                pattern       = bicRegex
              )
            ),
        "accountNumber" ->
          play.api.data.Forms.text
            .transform(normaliseIban, identity)
            .verifying(
              requiredMaxLengthAndPattern(
                requiredKey   = "bankDetails.internationalBankAccountDetails.error.accountNumber.required",
                lengthKey     = "bankDetails.internationalBankAccountDetails.error.accountNumber.length",
                invalidKey    = "bankDetails.internationalBankAccountDetails.error.accountNumber.invalid",
                maximumLength = 34,
                pattern       = ibanRegex
              )
            ),
        "CurrencyOfAccount" ->
          play.api.data.Forms.text
            .transform(normaliseCurrencyCode, identity)
            .verifying(
              requiredAndPattern(
                requiredKey = "bankDetails.internationalBankAccountDetails.error.CurrencyOfAccount.required",
                invalidKey  = "bankDetails.internationalBankAccountDetails.error.CurrencyOfAccount.invalid",
                pattern     = currencyCodeRegex
              )
            )
      )(
        (
          nameOnTheAccount,
          bic,
          accountNumber,
          currencyOfAccount
        ) =>
          BankDetails(
            NameOnTheAccount  = nameOnTheAccount,
            BIC               = bic,
            accountNumber     = accountNumber,
            CurrencyOfAccount = currencyOfAccount
          )
      )(bankDetails =>
        Some(
          (
            bankDetails.NameOnTheAccount,
            bankDetails.BIC,
            bankDetails.accountNumber,
            bankDetails.CurrencyOfAccount
          )
        )
      )
    )
}
