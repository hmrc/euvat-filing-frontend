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

package viewmodels.checkAnswers

import base.SpecBase
import pages.BusinessActivityCodeTwoPage

class BusinessActivityCodeTwoSummarySpec extends SpecBase {

  "BusinessActivityCodeTwoSummary" - {

    "must return None when no value present" in new Setup {
      val answers = emptyUserAnswers
      BusinessActivityCodeTwoSummary.row(answers) mustBe None
    }

    "must return a summary row when value present" in new Setup {
      val answers = emptyUserAnswers.set(BusinessActivityCodeTwoPage, "25344").success.value
      val row = BusinessActivityCodeTwoSummary.row(answers).value
      row.toString must include("businessActivityCodeTwo.checkYourAnswersLabel")
      row.toString must include("25344")
    }
  }

  trait Setup {
    val app = applicationBuilder().build()
    implicit val messages: play.api.i18n.Messages = play.api.i18n.MessagesImpl(
      play.api.i18n.Lang.defaultLang,
      app.injector.instanceOf[play.api.i18n.MessagesApi]
    )
  }
}
