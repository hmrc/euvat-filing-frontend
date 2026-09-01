package forms

import forms.behaviours.OptionFieldBehaviours
import models.PurchaseOrImport
import play.api.data.FormError

class PurchaseOrImportFormProviderSpec extends OptionFieldBehaviours {

  val form = new PurchaseOrImportFormProvider()()

  ".value" - {

    val fieldName = "value"
    val requiredKey = "purchaseOrImport.error.required"

    behave like optionsField[PurchaseOrImport](
      form,
      fieldName,
      validValues  = PurchaseOrImport.values,
      invalidError = FormError(fieldName, "error.invalid")
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )
  }
}
