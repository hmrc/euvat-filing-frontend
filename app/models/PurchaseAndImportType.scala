/*
 * Copyright 2026 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package models

import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.radios.RadioItem

sealed trait PurchaseAndImportType
case object Fuel         extends WithName("fuel") with PurchaseAndImportType
case object Transport    extends WithName("transport") with PurchaseAndImportType
case object FoodAndDrink extends WithName("foodAndDrink") with PurchaseAndImportType
case object Luxuries     extends WithName("luxuries") with PurchaseAndImportType
case object Other        extends WithName("other") with PurchaseAndImportType

object PurchaseAndImportType extends Enumerable.Implicits:

  val values: Seq[PurchaseAndImportType] = Seq(Fuel, Transport, FoodAndDrink, Luxuries, Other)

  val codes: Map[PurchaseAndImportType, String] = Map(
    Fuel         -> "1",
    Transport    -> "3",
    FoodAndDrink -> "7",
    Luxuries     -> "9",
    Other        -> "10"
  )

  val urlSlugForPurchaseType: Map[PurchaseAndImportType, String] = Map(
    Fuel         -> "fuel-use",
    Transport    -> "transport-cost",
    FoodAndDrink -> "food-drink-restaurant-cost",
    Luxuries     -> "luxury-entertainment-hospitality-cost",
    Other        -> "purchase-type-other"
  )

  val valueFromUrlSlug: Map[String, String] = urlSlugForPurchaseType.map((k, v) => (v, k.toString))

  def options(implicit messages: Messages): Seq[RadioItem] = values.zipWithIndex.map { case (value, index) =>
    RadioItem(
      content = Text(messages(s"purchaseType.${value.toString}")),
      value   = Some(value.toString),
      id      = Some(s"value_$index")
    )
  }

  implicit val enumerable: Enumerable[PurchaseAndImportType] =
    Enumerable(values.map(v => v.toString -> v)*)
