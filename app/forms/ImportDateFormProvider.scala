package forms

import forms.mappings.Mappings
import play.api.data.Form
import play.api.i18n.Messages

import java.time.LocalDate
import javax.inject.Inject

class ImportDateFormProvider @Inject() extends Mappings {

  def apply()(implicit messages: Messages): Form[LocalDate] =
    Form(
      "value" -> localDate(
        invalidKey     = "importDate.error.invalid",
        allRequiredKey = "importDate.error.required.all",
        twoRequiredKey = "importDate.error.required.two",
        requiredKey    = "importDate.error.required"
      )
    )
}
