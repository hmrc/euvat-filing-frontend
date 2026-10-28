package forms

import forms.bankdetails.AccountHolderFormProvider
import forms.behaviours.OptionFieldBehaviours
import models.AccountHolder
import play.api.data.FormError

class AccountHolderFormProviderSpec extends OptionFieldBehaviours {

  val form = new AccountHolderFormProvider()()

  ".value" - {

    val fieldName = "value"
    val requiredKey = "accountHolder.error.required"

    behave like optionsField[AccountHolder](
      form,
      fieldName,
      validValues  = AccountHolder.values,
      invalidError = FormError(fieldName, "error.invalid")
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )
  }
}
