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

package controllers

import base.SpecBase
import play.api.test.FakeRequest
import play.api.test.Helpers._

class LanguageSwitchControllerSpec extends SpecBase {

  "LanguageSwitchController" - {

    "must return the configured fallback URL and language map" in {
      val application = applicationBuilder().build()

      running(application) {
        val controller = application.injector.instanceOf[controllers.LanguageSwitchController]

        controller.fallbackURL mustEqual controllers.routes.IndexController.onPageLoad().url

        val langs = controller.languageMap
        langs.keySet must contain("en")
        langs.keySet must contain("cy")
      }
    }
  }
}
