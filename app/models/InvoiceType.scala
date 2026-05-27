package models

import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.radios.RadioItem

sealed trait InvoiceType

object InvoiceType extends Enumerable.Implicits {

  case object StandardInvoice extends WithName("standard invoice") with InvoiceType
  case object SimplifiedInvoice extends WithName("simplified invoice") with InvoiceType

  val values: Seq[InvoiceType] = Seq(
    StandardInvoice, SimplifiedInvoice
  )

  def options(implicit messages: Messages): Seq[RadioItem] = values.zipWithIndex.map {
    case (value, index) =>
      RadioItem(
        content = Text(messages(s"invoiceType.${value.toString}")),
        value   = Some(value.toString),
        id      = Some(s"value_$index")
      )
  }

  implicit val enumerable: Enumerable[InvoiceType] =
    Enumerable(values.map(v => v.toString -> v): _*)
}
