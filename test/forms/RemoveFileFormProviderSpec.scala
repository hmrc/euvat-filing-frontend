package forms

import forms.behaviours.BooleanFieldBehaviours
import play.api.data.FormError

class RemoveFileFormProviderSpec extends BooleanFieldBehaviours {

  val requiredKey = "removeFile.error.required"
  val invalidKey = "error.boolean"

  val form = new RemoveFileFormProvider()()

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
