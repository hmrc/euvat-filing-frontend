package forms

import forms.behaviours.IntFieldBehaviours
import play.api.data.FormError

class TotalVatPaidImportFormProviderSpec extends IntFieldBehaviours {

  val form = new TotalVatPaidImportFormProvider()()

  ".value" - {

    val fieldName = "value"

    val minimum = -999,999,999.99
    val maximum = 999,999,999.99

    val validDataGenerator = intsInRangeWithCommas(minimum, maximum)

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      validDataGenerator
    )

    behave like intField(
      form,
      fieldName,
      nonNumericError  = FormError(fieldName, "totalVatPaidImport.error.nonNumeric"),
      wholeNumberError = FormError(fieldName, "totalVatPaidImport.error.wholeNumber")
    )

    behave like intFieldWithRange(
      form,
      fieldName,
      minimum       = minimum,
      maximum       = maximum,
      expectedError = FormError(fieldName, "totalVatPaidImport.error.outOfRange", Seq(minimum, maximum))
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, "totalVatPaidImport.error.required")
    )
  }
}
