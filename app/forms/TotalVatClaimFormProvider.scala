package forms

import forms.mappings.Mappings
import javax.inject.Inject
import play.api.data.Form

class TotalVatClaimFormProvider @Inject() extends Mappings {

  def apply(): Form[BigDecimal] =
    Form(
      "value" -> currency(
        "totalVatClaim.error.required",
        "totalVatClaim.error.invalidNumeric",
        "totalVatClaim.error.nonNumeric"
      )
      .verifying(maximumCurrency(999999999.99, "totalVatClaim.error.aboveMaximum"))
    )
}
