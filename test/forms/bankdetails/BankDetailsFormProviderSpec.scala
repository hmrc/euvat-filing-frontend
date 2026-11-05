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

import models.BankDetails
import org.scalatest.OptionValues.convertOptionToValuable
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers

class BankDetailsFormProviderSpec extends AnyFreeSpec with Matchers {

  private val form =
    new BankDetailsFormProvider()()

  private val validNameOnAccount =
    "John Smith"

  private val validBic =
    "NWBKGB2L"

  private val validAccountNumber =
    "GB29NWBK60161331926819"

  private val validCurrency =
    "GBP"

  private val validData =
    Map(
      "NameOnTheAccount"  -> validNameOnAccount,
      "BIC"               -> validBic,
      "accountNumber"     -> validAccountNumber,
      "CurrencyOfAccount" -> validCurrency
    )

  "BankDetailsFormProvider" - {

    "must bind valid bank details" in {

      val result =
        form.bind(validData)

      result.errors mustBe empty

      result.value mustBe Some(
        BankDetails(
          NameOnTheAccount  = validNameOnAccount,
          BIC               = validBic,
          accountNumber     = validAccountNumber,
          CurrencyOfAccount = validCurrency
        )
      )
    }

    ".NameOnTheAccount" - {

      val fieldName =
        "NameOnTheAccount"

      val requiredKey =
        "bankDetails.internationalBankAccountDetails.error.NameOnTheAccount.required"

      val lengthKey =
        "bankDetails.internationalBankAccountDetails.error.NameOnTheAccount.length"

      val invalidKey =
        "bankDetails.internationalBankAccountDetails.error.NameOnTheAccount.invalid"

      "must bind a valid name" in {

        val result =
          form.bind(
            validData.updated(
              fieldName,
              "John Smith"
            )
          )

        result.errors mustBe empty

        result.value.map(_.NameOnTheAccount) mustBe
          Some("John Smith")
      }

      "must bind a name containing allowed characters" in {

        val value =
          "O'Connor-Smith (UK) Ltd."

        val result =
          form.bind(
            validData.updated(
              fieldName,
              value
            )
          )

        result.errors mustBe empty

        result.value.map(_.NameOnTheAccount) mustBe
          Some(value)
      }

      "must preserve leading and trailing whitespace" in {

        val value =
          "  John Smith  "

        val result =
          form.bind(
            validData.updated(
              fieldName,
              value
            )
          )

        result.errors mustBe empty

        result.value.map(_.NameOnTheAccount) mustBe
          Some(value)
      }


      "must bind a value with exactly 70 characters" in {

        val value =
          "A" * 70

        val result =
          form.bind(
            validData.updated(
              fieldName,
              value
            )
          )

        result.errors mustBe empty

        result.value.map(_.NameOnTheAccount) mustBe
          Some(value)
      }

      "must return required error when empty" in {

        val result =
          form.bind(
            validData.updated(
              fieldName,
              ""
            )
          )

        result.error(fieldName).map(_.message) mustBe
          Some(requiredKey)
      }

      "must return required error when only whitespace is supplied" in {

        val result =
          form.bind(
            validData.updated(
              fieldName,
              "   "
            )
          )

        result.error(fieldName).map(_.message) mustBe
          Some(requiredKey)
      }

      "must return length error when longer than 70 characters" in {

        val result =
          form.bind(
            validData.updated(
              fieldName,
              "A" * 71
            )
          )

        val error =
          result.error(fieldName).value

        error.message mustBe lengthKey
        error.args must contain(70)
      }

      "must return invalid error when invalid characters are supplied" in {

        val result =
          form.bind(
            validData.updated(
              fieldName,
              "John Smith @ Company"
            )
          )

        result.error(fieldName).map(_.message) mustBe
          Some(invalidKey)
      }

      "must reject ampersand because it is not allowed by the regex" in {

        val result =
          form.bind(
            validData.updated(
              fieldName,
              "John & Sons"
            )
          )

        result.error(fieldName).map(_.message) mustBe
          Some(invalidKey)
      }
    }

    ".BIC" - {

      val fieldName =
        "BIC"

      val requiredKey =
        "bankDetails.internationalBankAccountDetails.error.BIC.required"

      val lengthKey =
        "bankDetails.internationalBankAccountDetails.error.BIC.length"

      val invalidKey =
        "bankDetails.internationalBankAccountDetails.error.BIC.invalid"

      "must bind a valid 8 character BIC" in {

        val result =
          form.bind(
            validData.updated(
              fieldName,
              "NWBKGB2L"
            )
          )

        result.errors mustBe empty

        result.value.map(_.BIC) mustBe
          Some("NWBKGB2L")
      }

      "must bind a valid 11 character BIC" in {

        val result =
          form.bind(
            validData.updated(
              fieldName,
              "NWBKGB2LXXX"
            )
          )

        result.errors mustBe empty

        result.value.map(_.BIC) mustBe
          Some("NWBKGB2LXXX")
      }

      "must trim leading and trailing whitespace" in {

        val result =
          form.bind(
            validData.updated(
              fieldName,
              "  NWBKGB2L  "
            )
          )

        result.errors mustBe empty

        result.value.map(_.BIC) mustBe
          Some("NWBKGB2L")
      }

      "must return invalid error for lowercase BIC" in {

        val result =
          form.bind(
            validData.updated(
              fieldName,
              "nwbkgb2l"
            )
          )

        result.error(fieldName).map(_.message) mustBe
          Some(invalidKey)
      }

      "must return required error when empty" in {

        val result =
          form.bind(
            validData.updated(
              fieldName,
              ""
            )
          )

        result.error(fieldName).map(_.message) mustBe
          Some(requiredKey)
      }

      "must return required error when only whitespace is supplied" in {

        val result =
          form.bind(
            validData.updated(
              fieldName,
              "   "
            )
          )

        result.error(fieldName).map(_.message) mustBe
          Some(requiredKey)
      }

      "must return length error when longer than 11 characters" in {

        val result =
          form.bind(
            validData.updated(
              fieldName,
              "NWBKGB2LXXXX"
            )
          )

        val errors =
          result.errors(fieldName)

        errors.map(_.message) must contain(lengthKey)

        errors
          .find(_.message == lengthKey)
          .value
          .args must contain(11)
      }

      "must return invalid error for an invalid BIC" in {

        val result =
          form.bind(
            validData.updated(
              fieldName,
              "INVALID"
            )
          )

        result.error(fieldName).map(_.message) mustBe
          Some(invalidKey)
      }
    }

    ".accountNumber" - {

      val fieldName =
        "accountNumber"

      val requiredKey =
        "bankDetails.internationalBankAccountDetails.error.accountNumber.required"

      val lengthKey =
        "bankDetails.internationalBankAccountDetails.error.accountNumber.length"

      val invalidKey =
        "bankDetails.internationalBankAccountDetails.error.accountNumber.invalid"

      "must bind a valid IBAN" in {

        val result =
          form.bind(
            validData.updated(
              fieldName,
              "GB29NWBK60161331926819"
            )
          )

        result.errors mustBe empty

        result.value.map(_.accountNumber) mustBe
          Some("GB29NWBK60161331926819")
      }

      "must trim leading and trailing whitespace" in {

        val result =
          form.bind(
            validData.updated(
              fieldName,
              "  GB29NWBK60161331926819  "
            )
          )

        result.errors mustBe empty

        result.value.map(_.accountNumber) mustBe
          Some("GB29NWBK60161331926819")
      }

      "must return invalid error for lowercase IBAN" in {

        val result =
          form.bind(
            validData.updated(
              fieldName,
              "gb29nwbk60161331926819"
            )
          )

        result.error(fieldName).map(_.message) mustBe
          Some(invalidKey)
      }

      "must return required error when empty" in {

        val result =
          form.bind(
            validData.updated(
              fieldName,
              ""
            )
          )

        result.error(fieldName).map(_.message) mustBe
          Some(requiredKey)
      }

      "must return required error when only whitespace is supplied" in {

        val result =
          form.bind(
            validData.updated(
              fieldName,
              "   "
            )
          )

        result.error(fieldName).map(_.message) mustBe
          Some(requiredKey)
      }

      "must return length error when longer than 34 characters" in {

        val value =
          "GB00" + ("A" * 31)

        val result =
          form.bind(
            validData.updated(
              fieldName,
              value
            )
          )

        val errors =
          result.errors(fieldName)

        errors.map(_.message) must contain(lengthKey)

        errors
          .find(_.message == lengthKey)
          .value
          .args must contain(34)
      }

      "must return invalid error for an invalid IBAN" in {

        val result =
          form.bind(
            validData.updated(
              fieldName,
              "INVALID"
            )
          )

        result.error(fieldName).map(_.message) mustBe
          Some(invalidKey)
      }

      "must return invalid error when IBAN country prefix is invalid" in {

        val result =
          form.bind(
            validData.updated(
              fieldName,
              "1234NWBK60161331926819"
            )
          )

        result.error(fieldName).map(_.message) mustBe
          Some(invalidKey)
      }
    }

    ".CurrencyOfAccount" - {

      val fieldName =
        "CurrencyOfAccount"

      val requiredKey =
        "bankDetails.internationalBankAccountDetails.error.CurrencyOfAccount.required"

      val invalidKey =
        "bankDetails.internationalBankAccountDetails.error.CurrencyOfAccount.invalid"

      "must bind a valid three letter currency code" in {

        val result =
          form.bind(
            validData.updated(
              fieldName,
              "GBP"
            )
          )

        result.errors mustBe empty

        result.value.map(_.CurrencyOfAccount) mustBe
          Some("GBP")
      }

      "must trim and convert lowercase currency code to uppercase" in {

        val result =
          form.bind(
            validData.updated(
              fieldName,
              "  gbp  "
            )
          )

        result.errors mustBe empty

        result.value.map(_.CurrencyOfAccount) mustBe
          Some("GBP")
      }

      "must return required error when empty" in {

        val result =
          form.bind(
            validData.updated(
              fieldName,
              ""
            )
          )

        result.error(fieldName).map(_.message) mustBe
          Some(requiredKey)
      }

      "must return required error when only whitespace is supplied" in {

        val result =
          form.bind(
            validData.updated(
              fieldName,
              "   "
            )
          )

        result.error(fieldName).map(_.message) mustBe
          Some(requiredKey)
      }

      "must return invalid error for a two character currency code" in {

        val result =
          form.bind(
            validData.updated(
              fieldName,
              "GB"
            )
          )

        result.error(fieldName).map(_.message) mustBe
          Some(invalidKey)
      }

      "must return invalid error for a four character currency code" in {

        val result =
          form.bind(
            validData.updated(
              fieldName,
              "GBPP"
            )
          )

        result.error(fieldName).map(_.message) mustBe
          Some(invalidKey)
      }

      "must return invalid error when currency code contains numbers" in {

        val result =
          form.bind(
            validData.updated(
              fieldName,
              "GB1"
            )
          )

        result.error(fieldName).map(_.message) mustBe
          Some(invalidKey)
      }

      "must return invalid error when currency code contains special characters" in {

        val result =
          form.bind(
            validData.updated(
              fieldName,
              "GB!"
            )
          )

        result.error(fieldName).map(_.message) mustBe
          Some(invalidKey)
      }
    }

    "must unbind BankDetails into the correct form fields" in {

      val bankDetails =
        BankDetails(
          NameOnTheAccount  = "John Smith",
          BIC               = "NWBKGB2L",
          accountNumber     = "GB29NWBK60161331926819",
          CurrencyOfAccount = "GBP"
        )

      val filledForm =
        form.fill(bankDetails)

      filledForm("NameOnTheAccount").value mustBe
        Some("John Smith")

      filledForm("BIC").value mustBe
        Some("NWBKGB2L")

      filledForm("accountNumber").value mustBe
        Some("GB29NWBK60161331926819")

      filledForm("CurrencyOfAccount").value mustBe
        Some("GBP")
    }
  }
}
