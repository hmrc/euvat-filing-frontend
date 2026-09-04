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

sealed trait ImportType

object ImportType extends Enumerable.Implicits {

  case object Fuel         extends WithName("fuel") with ImportType
  case object Transport    extends WithName("transport") with ImportType
  case object FoodAndDrink extends WithName("foodAndDrink") with ImportType
  case object Luxuries     extends WithName("luxuries") with ImportType
  case object Other        extends WithName("other") with ImportType

  val values: Seq[ImportType] = Seq(
    Fuel,
    Transport,
    FoodAndDrink,
    Luxuries,
    Other
  )

  val urlSlugForImportType: Map[ImportType, String] = Map(
    Fuel         -> "import-fuel-use",
    Transport    -> "import-transport-cost",
    FoodAndDrink -> "import-food-drink-restaurant-cost",
    Luxuries     -> "import-luxury-entertainment-hospitality-cost",
    Other        -> "import-type-other"
  )

  def fromKey(key: String): Option[ImportType] = values.find(_.toString == key)

  implicit val enumerable: Enumerable[ImportType] =
    Enumerable(values.map(v => v.toString -> v)*)
}
