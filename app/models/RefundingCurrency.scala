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

sealed trait RefundingCurrency {
  val symbol: String
}

object RefundingCurrency extends Enumerable.Implicits {

  case object Euro          extends WithName("euro") with RefundingCurrency { val symbol = "€" }
  case object EstonianKroon extends WithName("estonianKroon") with RefundingCurrency { val symbol = "kr" }
  case object BulgarianLev  extends WithName("bulgarianLev") with RefundingCurrency { val symbol = "лв" }

  val values: Seq[RefundingCurrency] = Seq(
    Euro,
    EstonianKroon,
    BulgarianLev
  )

  implicit val enumerable: Enumerable[RefundingCurrency] =
    Enumerable(values.map(v => v.toString -> v)*)
}
