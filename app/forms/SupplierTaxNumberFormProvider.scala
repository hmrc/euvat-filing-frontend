package forms

import javax.inject.Inject

import forms.mappings.Mappings
import play.api.data.Form
import models.SupplierTaxNumber

class SupplierTaxNumberFormProvider @Inject() extends Mappings {

  def apply(): Form[SupplierTaxNumber] =
    Form(
      "value" -> enumerable[SupplierTaxNumber]("supplierTaxNumber.error.required")
    )
}
