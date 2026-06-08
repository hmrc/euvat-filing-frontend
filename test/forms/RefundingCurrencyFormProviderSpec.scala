package forms

import forms.behaviours.OptionFieldBehaviours
import models.RefundingCurrency
import play.api.data.FormError

class RefundingCurrencyFormProviderSpec extends OptionFieldBehaviours {

  val form = new RefundingCurrencyFormProvider()()

  ".value" - {

    val fieldName = "value"
    val requiredKey = "refundingCurrency.error.required"

    behave like optionsField[RefundingCurrency](
      form,
      fieldName,
      validValues  = RefundingCurrency.values,
      invalidError = FormError(fieldName, "error.invalid")
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )
  }
}
