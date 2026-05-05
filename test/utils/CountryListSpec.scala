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

import base.SpecBase
import play.api.Configuration

class CountryListSpec extends SpecBase {

  "CountryList.fromConfig" - {

    "parse entries with name and code separated by |" in {
      val config = Configuration("eu.member-states" -> Seq("Germany|DE", "France|FR"))
      CountryList.fromConfig(config) mustEqual Seq(("Germany", "DE"), ("France", "FR"))
    }

    "parse name-only entries as name with empty code" in {
      val config = Configuration("eu.member-states" -> Seq("Spain"))
      CountryList.fromConfig(config) mustEqual Seq(("Spain", ""))
    }

    "treat malformed entries (extra pipes) as the raw string with empty code" in {
      val bad = "a|b|c"
      val config = Configuration("eu.member-states" -> Seq(bad))
      CountryList.fromConfig(config) mustEqual Seq((bad, ""))
    }

    "return empty sequence when the config key is missing" in {
      val config = Configuration.empty
      CountryList.fromConfig(config) mustEqual Seq.empty
    }
  }

}
