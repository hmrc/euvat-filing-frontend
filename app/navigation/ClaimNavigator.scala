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

import controllers.claim.routes as claimRoutes
import models.{CheckMode, Mode, NormalMode, UserAnswers}
import pages.{BusinessActivityPage, BusinessActivityTwoPage, CheckYourStateDetailsPage, CountryChangedPage}
import play.api.mvc.Call
import utils.{ConfigLanguageMapping, ConfigPurchaseMapping, CountryCode}

import javax.inject.{Inject, Singleton}
@Singleton
class ClaimNavigator @Inject() (configLanguageMapping: ConfigLanguageMapping, configPurchaseMapping: ConfigPurchaseMapping) {

  def navigateFromRefundingCountryPage(mode: Mode, userAnswers: UserAnswers): Call = {
    CountryCode.findCountryCode(userAnswers) match {
      case Some(code) if configLanguageMapping.languagesFor(code).size <= 1 =>
        mode match {
          case NormalMode => claimRoutes.RefundPeriodController.onPageLoad(NormalMode)
          case CheckMode  => claimRoutes.CheckYourClaimDetailsController.onPageLoad()
        }
      case _ => claimRoutes.RefundingLanguageController.onPageLoad(mode)
    }
  }

  def navigateFromRefundingLanguagePage(mode: Mode)(userAnswers: UserAnswers): Call = {
    mode match {
      case NormalMode => claimRoutes.RefundPeriodController.onPageLoad(NormalMode)
      case _ =>
        if (userAnswers.get(CountryChangedPage).contains(true)) {
          claimRoutes.RefundPeriodController.onPageLoad(CheckMode)
        } else {
          claimRoutes.CheckYourClaimDetailsController.onPageLoad()
        }
    }
  }

  def navigateFromBusinessActivityPage(mode: Mode)(userAnswers: UserAnswers): Call =
    userAnswers.get(BusinessActivityPage) match {
      case Some(true) => claimRoutes.BusinessActivityCodeTwoController.onPageLoad(mode)
      case _          => claimRoutes.CheckYourClaimDetailsController.onPageLoad()
    }

  def navigateFromBusinessActivity2Page(mode: Mode)(userAnswers: UserAnswers): Call =
    userAnswers.get(BusinessActivityTwoPage) match {
      case Some(true) => claimRoutes.BusinessActivityCodeThreeController.onPageLoad(mode)
      case _          => claimRoutes.CheckYourClaimDetailsController.onPageLoad()
    }

  def navigateFromCheckYourStateDetailsPage(mode: Mode)(userAnswers: UserAnswers): Call =
    userAnswers.get(CheckYourStateDetailsPage) match {
      case Some(true) => controllers.routes.JourneyRecoveryController.onPageLoad() // TODO: replace when F8 delete application is in place
      case _          => claimRoutes.CheckYourClaimDetailsController.onPageLoad()
    }

}
