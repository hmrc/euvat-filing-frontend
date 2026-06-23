package forms

import forms.behaviours.BooleanFieldBehaviours
import play.api.data.FormError

class CheckYourStateDetailsFormProviderSpec extends BooleanFieldBehaviours {

  val requiredKey = "checkYourStateDetails.error.required"
  val invalidKey = "error.boolean"

  val form = new CheckYourStateDetailsFormProvider()()

  ".value" - {

    val fieldName = "value"

    behave like booleanField(
      form,
      fieldName,
      invalidError = FormError(fieldName, invalidKey)
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )
  }
}
