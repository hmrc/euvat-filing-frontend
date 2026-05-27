package forms

import forms.behaviours.OptionFieldBehaviours
import models.InvoiceType
import play.api.data.FormError

class InvoiceTypeFormProviderSpec extends OptionFieldBehaviours {

  val form = new InvoiceTypeFormProvider()()

  ".value" - {

    val fieldName = "value"
    val requiredKey = "invoiceType.error.required"

    behave like optionsField[InvoiceType](
      form,
      fieldName,
      validValues  = InvoiceType.values,
      invalidError = FormError(fieldName, "error.invalid")
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )
  }
}
