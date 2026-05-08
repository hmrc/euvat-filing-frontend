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

package services

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.Configuration
import com.typesafe.config.ConfigFactory

class ConfigLanguageMappingSpec extends AnyWordSpec with Matchers {

  "ConfigLanguageMapping" should {
    "read mapping from configuration" in {
      val confString = """
        |language.mapping = {
        |  AT = ["german", "english"]
        |  ZZ = ["foo"]
        |}
      """.stripMargin

      val cfg = Configuration(ConfigFactory.parseString(confString))
      val svc = new ConfigLanguageMapping(cfg)

      svc.languagesFor("AT") shouldBe Seq("German", "English")
      svc.languagesFor("ZZ") shouldBe Seq("Foo")
      svc.languagesFor("UNKNOWN") shouldBe Seq("English")

      // Also verify the application's `conf/application.conf` mappings
      val appCfg = Configuration(ConfigFactory.load())
      val appSvc = new ConfigLanguageMapping(appCfg)
      val lm = appCfg.get[Configuration]("language.mapping")

      lm.entrySet.map(_._1).foreach { key =>
        val expected = lm.get[Seq[String]](key).map(_.capitalize)
        appSvc.languagesFor(key) shouldBe expected
      }
    }
  }
}
