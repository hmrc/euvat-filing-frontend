package forms

import forms.behaviours.OptionFieldBehaviours
import models.SupplierTaxNumber
import play.api.data.FormError

class SupplierTaxNumberFormProviderSpec extends OptionFieldBehaviours {

  val form = new SupplierTaxNumberFormProvider()()

  ".value" - {

    val fieldName = "value"
    val requiredKey = "supplierTaxNumber.error.required"

    behave like optionsField[SupplierTaxNumber](
      form,
      fieldName,
      validValues  = SupplierTaxNumber.values,
      invalidError = FormError(fieldName, "error.invalid")
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )
  }
}
