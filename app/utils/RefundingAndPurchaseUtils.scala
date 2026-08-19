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

package utils

import javax.inject.{Inject, Singleton}
import models.UserAnswers
import pages.{RefundingCountryNamePage, RefundingCountryPage, RefundingCurrencyPage}

@Singleton
class RefundingAndPurchaseUtils @Inject() (configCurrencyMapping: ConfigCurrencyMapping) {

  def findCountryCode(userAnswers: UserAnswers): Option[String] =
    userAnswers
      .get(RefundingCountryPage)
      .orElse(
        userAnswers
          .get(RefundingCountryNamePage)
          .map(stored => stored.split(",", 2).headOption.getOrElse(stored))
      )

  private val defaultName = "Euro"
  private val defaultSymbol = "€"

  private def humanizeName(name: String): String =
    name
      .replaceAll("([a-z])([A-Z])", "$1 $2")
      .split("[ _-]+")
      .filter(_.nonEmpty)
      .map(s => s.head.toUpper.toString + s.tail)
      .mkString(" ")

  // (humanized currency name, symbol) for the country/currency stored in the user's answers
  def resolveCurrency(userAnswers: UserAnswers): (String, String) =
    findCountryCode(userAnswers) match {
      case None => (defaultName, defaultSymbol)
      case Some(countryCode) =>
        val currencies = configCurrencyMapping.currenciesFor(countryCode)
        val chosen = userAnswers.get(RefundingCurrencyPage) match {
          case Some(currencyCode) => currencies.find(_._2 == currencyCode).orElse(currencies.headOption)
          case None               => currencies.headOption
        }
        chosen match {
          case Some((name, _, symbol)) => (humanizeName(name), symbol)
          case None                    => (defaultName, defaultSymbol)
        }
    }

  // currency symbol extraction
  def resolveCurrencySymbol(userAnswers: UserAnswers): String =
    resolveCurrency(userAnswers)._2

  // currency selection
  def requiresCurrencySelection(countryCode: String): Boolean =
    configCurrencyMapping.requiresCurrencySelection(countryCode)

}
