package forms

import forms.behaviours.StringFieldBehaviours
import play.api.data.FormError

class BankDetailsFormProviderSpec extends StringFieldBehaviours {

  val form = new BankDetailsFormProvider()()

  ".NameOnTheAccount" - {

    val fieldName = "NameOnTheAccount"
    val requiredKey = "bankDetails.error.NameOnTheAccount.required"
    val lengthKey = "bankDetails.error.NameOnTheAccount.length"
    val maxLength = 100

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      stringsWithMaxLength(maxLength)
    )

    behave like fieldWithMaxLength(
      form,
      fieldName,
      maxLength = maxLength,
      lengthError = FormError(fieldName, lengthKey, Seq(maxLength))
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )
  }

  ".BIC" - {

    val fieldName = "BIC"
    val requiredKey = "bankDetails.error.BIC.required"
    val lengthKey = "bankDetails.error.BIC.length"
    val maxLength = 100

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      stringsWithMaxLength(maxLength)
    )

    behave like fieldWithMaxLength(
      form,
      fieldName,
      maxLength = maxLength,
      lengthError = FormError(fieldName, lengthKey, Seq(maxLength))
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )
  }
}
