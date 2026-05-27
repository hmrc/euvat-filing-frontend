package forms

import javax.inject.Inject

import forms.mappings.Mappings
import play.api.data.Form
import models.InvoiceType

class InvoiceTypeFormProvider @Inject() extends Mappings {

  def apply(): Form[InvoiceType] =
    Form(
      "value" -> enumerable[InvoiceType]("invoiceType.error.required")
    )
}
