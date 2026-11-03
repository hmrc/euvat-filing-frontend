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

import javax.inject.Inject
import java.util.Locale

class BankDetailsFormProvider @Inject() extends Mappings {

  private def trim(value: String): String =
    value.trim

  private def normaliseBic(value: String): String =
    value.trim.toUpperCase(Locale.ROOT)

  private def normaliseIban(value: String): String =
    value
      .replaceAll("\\s+", "")
      .toUpperCase(Locale.ROOT)

  private def normaliseCurrencyCode(value: String): String =
    value.trim.toUpperCase(Locale.ROOT)

  def apply(): Form[BankDetails] =
    Form(
      mapping(
        "NameOnTheAccount" ->
          text("bankDetails.internationalBankAccountDetails.error.NameOnTheAccount.required")
            .transform(trim, identity)
            .verifying(
              maxLength(
                70,
                "bankDetails.internationalBankAccountDetails.error.NameOnTheAccount.length"
              )
            )
            .verifying(
              regexp(
                nameOnAccountRegex,
                "bankDetails.internationalBankAccountDetails.error.NameOnTheAccount.invalid"
              )
            ),
        "BIC" ->
          text("bankDetails.internationalBankAccountDetails.error.BIC.required")
            .transform(normaliseBic, identity)
            .verifying(
              maxLength(
                11,
                "bankDetails.internationalBankAccountDetails.error.BIC.length"
              )
            )
            .verifying(
              regexp(
                bicRegex,
                "bankDetails.internationalBankAccountDetails.error.BIC.invalid"
              )
            ),
        "accountNumber" ->
          text("bankDetails.internationalBankAccountDetails.error.accountNumber.required")
            .transform(normaliseIban, identity)
            .verifying(
              maxLength(
                34,
                "bankDetails.internationalBankAccountDetails.error.accountNumber.length"
              )
            )
            .verifying(
              regexp(
                ibanRegex,
                "bankDetails.internationalBankAccountDetails.error.accountNumber.invalid"
              )
            ),
        "CurrencyOfAccount" ->
          text("bankDetails.internationalBankAccountDetails.error.CurrencyOfAccount.required")
            .transform(normaliseCurrencyCode, identity)
            .verifying(
              regexp(
                currencyCodeRegex,
                "bankDetails.internationalBankAccountDetails.error.CurrencyOfAccount.invalid"
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
