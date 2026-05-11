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

package navigation

import base.SpecBase
import controllers.routes
import pages.*
import models.*

class NavigatorSpec extends SpecBase {

  val navigator = new Navigator
  val userAnswers: UserAnswers = UserAnswers("id")

  "Navigator" - {

    "in Normal mode" - {

      "must go from a page that doesn't exist in the route map to Index" in {
        case object UnknownPage extends Page
        navigator.nextPage(UnknownPage, NormalMode, userAnswers) mustBe routes.IndexController.onPageLoad()
      }

      "must go from RefundingCountryPage to RefundingLanguageController" in {
        navigator.nextPage(pages.RefundingCountryPage, NormalMode, userAnswers) mustBe
          routes.RefundingLanguageController.onPageLoad(NormalMode)
      }

      "must go from RefundingLanguagePage to RefundPeriodController" in {
        navigator.nextPage(pages.RefundingLanguagePage, NormalMode, userAnswers) mustBe
          routes.RefundPeriodController.onPageLoad(NormalMode)
      }

      "must go from RefundPeriodPage to ContactDetailsController" in {
        navigator.nextPage(pages.RefundPeriodPage, NormalMode, userAnswers) mustBe
          routes.ContactDetailsController.onPageLoad(NormalMode)
      }

      "must go from ContactDetailsPage to BusinessActivityController" in {
        navigator.nextPage(ContactDetailsPage, NormalMode, userAnswers) mustBe
          routes.BusinessActivityController.onPageLoad(NormalMode)
      }

      "must go from BusinessActivityPage to JourneyRecoveryController" in {
        navigator.nextPage(BusinessActivityPage, NormalMode, userAnswers) mustBe
          routes.JourneyRecoveryController.onPageLoad()
      }

      "must go from PurchaseTypePage to JourneyRecoveryController" in {
        navigator.nextPage(PurchaseTypePage, NormalMode, userAnswers) mustBe
          routes.JourneyRecoveryController.onPageLoad()
      }
    }

    "in Check mode" - {

      "must go from a page that doesn't exist in the edit route map to CheckYourAnswers" in {
        case object UnknownPage extends Page
        navigator.nextPage(UnknownPage, CheckMode, userAnswers) mustBe routes.CheckYourAnswersController.onPageLoad()
      }
    }

  }
}
