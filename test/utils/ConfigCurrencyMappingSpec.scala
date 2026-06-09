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

import com.typesafe.config.ConfigFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.Configuration

class ConfigCurrencyMappingSpec extends AnyWordSpec with Matchers {

  "ConfigCurrencyMapping" should {

    "read mapping from configuration" in {
      val confString =
        """
          |currency.mapping = {
          |  BG = ["euro|EUR", "bulgarianLev|BGN"]
          |  AT = ["euro|EUR"]
          |}
        """.stripMargin

      val cfg = Configuration(ConfigFactory.parseString(confString))
      val svc = new ConfigCurrencyMapping(cfg)

      svc.currenciesFor("BG")      shouldBe Seq(("euro", "EUR"), ("bulgarianLev", "BGN"))
      svc.currenciesFor("AT")      shouldBe Seq(("euro", "EUR"))
      svc.currenciesFor("UNKNOWN") shouldBe Seq(("euro", "EUR"))
    }

    "return true for requiresCurrencySelection when country has two currencies" in {
      val confString =
        """
          |currency.mapping = {
          |  BG = ["euro|EUR", "bulgarianLev|BGN"]
          |  EE = ["euro|EUR", "estonianKroon|EEK"]
          |  AT = ["euro|EUR"]
          |}
        """.stripMargin

      val cfg = Configuration(ConfigFactory.parseString(confString))
      val svc = new ConfigCurrencyMapping(cfg)

      svc.requiresCurrencySelection("BG")      shouldBe true
      svc.requiresCurrencySelection("EE")      shouldBe true
      svc.requiresCurrencySelection("AT")      shouldBe false
      svc.requiresCurrencySelection("UNKNOWN") shouldBe false
    }

    "verify application.conf currency mappings load without error" in {
      val appCfg = Configuration(ConfigFactory.load())
      val appSvc = new ConfigCurrencyMapping(appCfg)
      val cm = appCfg.get[Configuration]("currency.mapping")

      cm.entrySet.map(_._1).foreach { key =>
        appSvc.currenciesFor(key) should not be empty
      }
    }
  }
}
