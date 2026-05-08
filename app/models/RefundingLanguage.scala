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

sealed trait RefundingLanguage

object RefundingLanguage extends Enumerable.Implicits {

  case object English    extends WithName("english") with RefundingLanguage
  case object German     extends WithName("german") with RefundingLanguage
  case object French     extends WithName("french") with RefundingLanguage
  case object Dutch      extends WithName("dutch") with RefundingLanguage
  case object Bulgarian  extends WithName("bulgarian") with RefundingLanguage
  case object Spanish    extends WithName("spanish") with RefundingLanguage
  case object Turkish    extends WithName("turkish") with RefundingLanguage
  case object Czech      extends WithName("czech") with RefundingLanguage
  case object Danish     extends WithName("danish") with RefundingLanguage
  case object Estonian   extends WithName("estonian") with RefundingLanguage
  case object Finnish    extends WithName("finnish") with RefundingLanguage
  case object Swedish    extends WithName("swedish") with RefundingLanguage
  case object Italian    extends WithName("italian") with RefundingLanguage
  case object Latvian    extends WithName("latvian") with RefundingLanguage
  case object Lithuanian extends WithName("lithuanian") with RefundingLanguage
  case object Maltese    extends WithName("maltese") with RefundingLanguage
  case object Polish     extends WithName("polish") with RefundingLanguage
  case object Portuguese extends WithName("portuguese") with RefundingLanguage
  case object Romanian   extends WithName("romanian") with RefundingLanguage
  case object Hungarian  extends WithName("hungarian") with RefundingLanguage
  case object Greek      extends WithName("greek") with RefundingLanguage
  case object Slovak     extends WithName("slovak") with RefundingLanguage
  case object Slovenian  extends WithName("slovenian") with RefundingLanguage
  case object Irish      extends WithName("irish") with RefundingLanguage

  val values: Seq[RefundingLanguage] = Seq(
    English,
    German,
    French,
    Dutch,
    Bulgarian,
    Spanish,
    Turkish,
    Czech,
    Danish,
    Estonian,
    Finnish,
    Swedish,
    Italian,
    Latvian,
    Lithuanian,
    Maltese,
    Polish,
    Portuguese,
    Romanian,
    Hungarian,
    Greek,
    Slovak,
    Slovenian,
    Irish
  )

  def options(implicit messages: Messages): Seq[RadioItem] = values.zipWithIndex.map { case (value, index) =>
    RadioItem(
      content = Text(messages(s"refundingLanguage.${value.toString}")),
      value   = Some(value.toString),
      id      = Some(s"value_$index")
    )
  }

  implicit val enumerable: Enumerable[RefundingLanguage] =
    Enumerable(values.map(v => v.toString -> v)*)
}
