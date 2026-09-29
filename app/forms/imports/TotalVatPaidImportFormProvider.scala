package forms

import forms.mappings.Mappings
import javax.inject.Inject
import play.api.data.Form

class TotalVatPaidImportFormProvider @Inject() extends Mappings {

  def apply(): Form[Int] =
    Form(
      "value" -> int(
        "totalVatPaidImport.error.required",
        "totalVatPaidImport.error.wholeNumber",
        "totalVatPaidImport.error.nonNumeric")
          .verifying(inRange(-999,999,999.99, 999,999,999.99, "totalVatPaidImport.error.outOfRange"))
    )
}
