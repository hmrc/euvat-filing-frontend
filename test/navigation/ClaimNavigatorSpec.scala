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
import com.typesafe.config.ConfigFactory
import controllers.claim.routes as claimRoutes
import controllers.routes
import models.*
import pages.*
import play.api.Configuration
import utils.ConfigLanguageMapping

class ClaimNavigatorSpec extends SpecBase {

  val navigator = new ClaimNavigator(
    new ConfigLanguageMapping(
      Configuration(
        ConfigFactory.parseString("""
          language.mapping = {
            AT = ["german", "english"]
            BE = ["english", "german", "french", "dutch"]
            CZ = ["czech"]
          }
        """)
      )
    )
  )
  val userAnswers: UserAnswers = emptyUserAnswers

  "Navigator" - {

    "in Normal mode" - {
      "must go from RefundingCountryPage to RefundPeriodController" in {
        val updatedAnswers = userAnswers.set(RefundingCountryPage, "HR").success.value
        navigator.navigateFromRefundingCountryPage(NormalMode, updatedAnswers) mustBe
          claimRoutes.RefundPeriodController.onPageLoad(NormalMode)
      }

      "must go from RefundingCountryPage to RefundingLanguageController" in {
        navigator.navigateFromRefundingCountryPage(NormalMode, userAnswers) mustBe
          claimRoutes.RefundingLanguageController.onPageLoad(NormalMode)
      }

      "must go from RefundingLanguagePage to RefundPeriodController" in {
        navigator.navigateFromRefundingLanguagePage(NormalMode)(userAnswers) mustBe
          claimRoutes.RefundPeriodController.onPageLoad(NormalMode)
      }

      "must go from BusinessActivityPage to BusinessActivityCodeTwoController if yes selected" in {
        val ua = userAnswers.set(BusinessActivityPage, true).success.value
        navigator.navigateFromBusinessActivityPage(NormalMode)(ua) mustBe
          claimRoutes.BusinessActivityCodeTwoController.onPageLoad(NormalMode)
      }

      "must go from BusinessActivityPage to CheckYourClaimDetailsPage if no selected" in {
        val ua = userAnswers.set(BusinessActivityPage, false).success.value
        navigator.navigateFromBusinessActivityPage(NormalMode)(ua) mustBe
          claimRoutes.CheckYourClaimDetailsController.onPageLoad()
      }

      "must go from BusinessActivityTwoPage to BusinessActivityCodeThreeController if yes selected" in {
        val ua = userAnswers.set(BusinessActivityTwoPage, true).success.value
        navigator.navigateFromBusinessActivity2Page(NormalMode)(ua) mustBe
          claimRoutes.BusinessActivityCodeThreeController.onPageLoad(NormalMode)
      }

      "must go from BusinessActivityTwoPage to CheckYourClaimDetailsPage if no selected" in {
        val ua = userAnswers.set(BusinessActivityTwoPage, false).success.value
        navigator.navigateFromBusinessActivity2Page(NormalMode)(ua) mustBe
          claimRoutes.CheckYourClaimDetailsController.onPageLoad()
      }

      "must go from CheckYourStateDetailsPage to CheckYourClaimDetailsController if no selected" in {
        val ua = userAnswers.set(CheckYourStateDetailsPage, false).success.value
        navigator.navigateFromCheckYourStateDetailsPage(NormalMode)(ua) mustBe
          claimRoutes.CheckYourClaimDetailsController.onPageLoad()
      }

      "must go from CheckYourStateDetailsPage to JourneyRecoveryController if yes selected" in {
        val ua = userAnswers.set(CheckYourStateDetailsPage, true).success.value
        navigator.navigateFromCheckYourStateDetailsPage(NormalMode)(ua) mustBe
          controllers.routes.JourneyRecoveryController.onPageLoad()
      }
    }

    "in Check mode" - {
      "must go from RefundingCountryPage to RefundingLanguageController" in {
        navigator.navigateFromRefundingCountryPage(CheckMode, userAnswers) mustBe
          claimRoutes.RefundingLanguageController.onPageLoad(CheckMode)
      }

      "must go from RefundingCountryPage to CheckYourClaimDetailsController" in {
        val ua = userAnswers.set(RefundingCountryPage, "CZ").success.value
        navigator.navigateFromRefundingCountryPage(CheckMode, ua) mustBe
          claimRoutes.CheckYourClaimDetailsController.onPageLoad()
      }

      "must go from RefundingLanguagePage to CheckYourClaimDetailsController" in {
        val ua = userAnswers.set(RefundingCountryPage, "AT").success.value
        navigator.navigateFromRefundingLanguagePage(CheckMode)(ua) mustBe
          claimRoutes.CheckYourClaimDetailsController.onPageLoad()
      }

      "must go from RefundingLanguagePage to RefundPeriodController if CountryChangedPage is true" in {
        val ua = userAnswers
          .set(RefundingCountryPage, "AT")
          .success
          .value
          .set(CountryChangedPage, true)
          .success
          .value
        navigator.navigateFromRefundingLanguagePage(CheckMode)(ua) mustBe
          claimRoutes.RefundPeriodController.onPageLoad(CheckMode)
      }

      "must go from BusinessActivityPage to BusinessActivityCodeTwoController if yes selected" in {
        val ua = userAnswers.set(BusinessActivityPage, true).success.value
        navigator.navigateFromBusinessActivityPage(CheckMode)(ua) mustBe
          claimRoutes.BusinessActivityCodeTwoController.onPageLoad(CheckMode)
      }

      "must go from BusinessActivityPage to CheckYourClaimDetailsPage if no selected" in {
        val ua = userAnswers.set(BusinessActivityPage, false).success.value
        navigator.navigateFromBusinessActivityPage(CheckMode)(ua) mustBe
          claimRoutes.CheckYourClaimDetailsController.onPageLoad()
      }

      "must go from BusinessActivityTwoPage to BusinessActivityCodeThreeController if yes selected" in {
        val ua = userAnswers.set(BusinessActivityTwoPage, true).success.value
        navigator.navigateFromBusinessActivity2Page(CheckMode)(ua) mustBe
          claimRoutes.BusinessActivityCodeThreeController.onPageLoad(CheckMode)
      }

      "must go from BusinessActivityTwoPage to CheckYourClaimDetailsPage if no selected" in {
        val ua = userAnswers.set(BusinessActivityTwoPage, false).success.value
        navigator.navigateFromBusinessActivity2Page(CheckMode)(ua) mustBe
          claimRoutes.CheckYourClaimDetailsController.onPageLoad()
      }

      "must go from CheckYourStateDetailsPage to JourneyRecoveryController if yes selected" in {
        val ua = userAnswers.set(CheckYourStateDetailsPage, true).success.value
        navigator.navigateFromCheckYourStateDetailsPage(CheckMode)(ua) mustBe
          routes.JourneyRecoveryController.onPageLoad()
      }

      "must go from CheckYourStateDetailsPage to CheckYourClaimDetailsController if no selected" in {
        val ua = userAnswers.set(CheckYourStateDetailsPage, false).success.value
        navigator.navigateFromCheckYourStateDetailsPage(CheckMode)(ua) mustBe
          claimRoutes.CheckYourClaimDetailsController.onPageLoad()
      }
    }
  }

}
