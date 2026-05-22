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
import pages.BusinessActivityCodeThreePage

class BusinessActivityCodeThreeSummarySpec extends SpecBase {

  "BusinessActivityCodeThreeSummary" - {

    "must return None when no value present" in new Setup {
      val answers = emptyUserAnswers
      BusinessActivityCodeThreeSummary.row(answers) mustBe None
    }

    "must show the row when the value is present" in new Setup {
      val answers = emptyUserAnswers.set(BusinessActivityCodeThreePage, "25344").success.value

      val row = BusinessActivityCodeThreeSummary.row(answers).value

      row.toString must include(messages("businessActivityCodeThree.checkYourAnswersLabel"))
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
