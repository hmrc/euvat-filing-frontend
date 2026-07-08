package models

import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.radios.RadioItem

sealed trait SupplierTaxNumber

object SupplierTaxNumber extends Enumerable.Implicits {

  case object Vatregistrationnumber extends WithName("vatRegistrationNumber") with SupplierTaxNumber
  case object Axidentifiernumber extends WithName("axIdentifierNumber") with SupplierTaxNumber

  val values: Seq[SupplierTaxNumber] = Seq(
    Vatregistrationnumber, Axidentifiernumber
  )

  def options(implicit messages: Messages): Seq[RadioItem] = values.zipWithIndex.map {
    case (value, index) =>
      RadioItem(
        content = Text(messages(s"supplierTaxNumber.${value.toString}")),
        value   = Some(value.toString),
        id      = Some(s"value_$index")
      )
  }

  implicit val enumerable: Enumerable[SupplierTaxNumber] =
    Enumerable(values.map(v => v.toString -> v): _*)
}
