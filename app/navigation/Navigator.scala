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

import javax.inject.{Inject, Singleton}

import play.api.mvc.Call
import controllers.routes
import pages.*
import models.*

@Singleton
class Navigator @Inject() () {

  private val normalRoutes: Page => UserAnswers => Call = {
    case RefundingCountryPage          => _ => routes.RefundingLanguageController.onPageLoad(NormalMode)
    case RefundingLanguagePage         => _ => routes.RefundPeriodController.onPageLoad(NormalMode)
    case RefundPeriodPage              => _ => routes.ContactDetailsController.onPageLoad(NormalMode)
    case ContactDetailsPage            => _ => routes.BusinessActivityController.onPageLoad(NormalMode)
    case BusinessActivityPage          => userAnswer => navigateFromBusinessActivityPage(NormalMode)(userAnswer)
    case BusinessActivityCodeTwoPage   => _ => routes.BusinessActivityTwoController.onPageLoad(NormalMode)
    case BusinessActivityTwoPage       => userAnswer => navigateFromBusinessActivity2Page(NormalMode)(userAnswer)
    case BusinessActivityCodeThreePage => _ => routes.BusinessActivityThreeController.onPageLoad()
    case PurchaseTypePage              => _ => routes.SuppliersNameController.onPageLoad(NormalMode)
    case SuppliersNamePage             => _ => routes.SupplierAddressController.onPageLoad(NormalMode)
    case SupplierAddressPage           => _ => routes.JourneyRecoveryController.onPageLoad()
    case _                             => _ => routes.IndexController.onPageLoad()
  }

  private val checkRouteMap: Page => UserAnswers => Call = {
    case BusinessActivityPage          => userAnswer => navigateFromBusinessActivityPage(CheckMode)(userAnswer)
    case BusinessActivityCodeTwoPage   => _ => routes.BusinessActivityTwoController.onPageLoad(CheckMode)
    case BusinessActivityTwoPage       => userAnswer => navigateFromBusinessActivity2Page(CheckMode)(userAnswer)
    case BusinessActivityCodeThreePage => _ => routes.BusinessActivityThreeController.onPageLoad()
    case SupplierAddressPage           => _ => routes.CheckYourAnswersController.onPageLoad()
    case _                             => _ => routes.CheckYourAnswersController.onPageLoad()
  }

  private def navigateFromBusinessActivityPage(mode: Mode)(userAnswers: UserAnswers): Call =
    userAnswers.get(BusinessActivityPage) match {
      case Some(true)  => routes.BusinessActivityCodeTwoController.onPageLoad(mode)
      case Some(false) => routes.JourneyRecoveryController.onPageLoad() // TODO - update to check you claim details page
      case _           => routes.JourneyRecoveryController.onPageLoad()
    }

  private def navigateFromBusinessActivity2Page(mode: Mode)(userAnswers: UserAnswers): Call =
    userAnswers.get(BusinessActivityTwoPage) match {
      case Some(true)  => routes.BusinessActivityCodeThreeController.onPageLoad(mode)
      case Some(false) => routes.JourneyRecoveryController.onPageLoad() // TODO - update to check you claim details page
      case _           => routes.JourneyRecoveryController.onPageLoad()
    }

  def nextPage(page: Page, mode: Mode, userAnswers: UserAnswers): Call = mode match {
    case NormalMode =>
      normalRoutes(page)(userAnswers)
    case CheckMode =>
      checkRouteMap(page)(userAnswers)
  }
}
