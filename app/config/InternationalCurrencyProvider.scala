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

package config

import com.typesafe.config.Config
import models.Currency

import javax.inject.{Inject, Singleton}
import scala.jdk.CollectionConverters._

@Singleton
class InternationalCurrencyProvider @Inject()(
                                               config: Config
                                             ) {

  private val configPath = "international-bank-currencies"

  val currencies: Seq[Currency] =
    config
      .getConfigList(configPath)
      .asScala
      .map { currencyConfig =>
        Currency(
          code = currencyConfig.getString("code"),
          description = currencyConfig.getString("description")
        )
      }
      .toSeq

  private val validCodes: Set[String] =
    currencies.map(_.code).toSet

  def isValid(code: String): Boolean =
    validCodes.contains(code)
}