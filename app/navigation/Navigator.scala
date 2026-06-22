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
import utils.{ConfigCurrencyMapping, ConfigLanguageMapping}

@Singleton
class Navigator @Inject() (configCurrencyMapping: ConfigCurrencyMapping, configLanguageMapping: ConfigLanguageMapping) {

  def nextPage(page: Page, mode: Mode, userAnswers: UserAnswers): Call = mode match {
    case NormalMode => normalRoutes(page)(userAnswers)
    case CheckMode  => checkRoutes(page)(userAnswers)
  }

  private val normalRoutes: Page => UserAnswers => Call = {
    case RefundingCountryPage             => userAnswers =>
      val maybeCountryCode = userAnswers.get(pages.RefundingCountryPage).orElse {
        userAnswers.get(pages.RefundingCountryNamePage).map { stored =>
          stored.split(",", 2).headOption.getOrElse(stored)
        }
      }

      maybeCountryCode match {
        case Some(code) if configLanguageMapping.languagesFor(code).size <= 1 =>
          if (configCurrencyMapping.requiresCurrencySelection(code)) routes.RefundingCurrencyController.onPageLoad(NormalMode)
          else routes.RefundPeriodController.onPageLoad(NormalMode)
        case _ => routes.RefundingLanguageController.onPageLoad(NormalMode)
      }
    case RefundingLanguagePage            => userAnswers => navigateFromRefundingLanguagePage(NormalMode)(userAnswers)
    case RefundingCurrencyPage            => _ => routes.RefundPeriodController.onPageLoad(NormalMode)
    case RefundPeriodPage                 => _ => routes.ContactDetailsController.onPageLoad(NormalMode)
    case ContactDetailsPage               => _ => routes.BusinessActivityController.onPageLoad(NormalMode)
    case BusinessActivityPage             => userAnswer => navigateFromBusinessActivityPage(NormalMode)(userAnswer)
    case BusinessActivityTwoPage          => userAnswer => navigateFromBusinessActivity2Page(NormalMode)(userAnswer)
    case BusinessActivityCodeThreePage    => _ => routes.BusinessActivityThreeController.onPageLoad()
    case InvoiceTypePage                  => userAnswer => navigateFromInvoiceTypePage(NormalMode)(userAnswer)
    case InvoiceNumberPage                => _ => routes.InvoiceDateController.onPageLoad(NormalMode)
    case InvoiceDatePage                  => _ => routes.SuppliersNameController.onPageLoad(NormalMode)
    case SuppliersNamePage                => _ => routes.SupplierAddressController.onPageLoad(NormalMode)
    case SupplierAddressPage              => _ => routes.SimplifiedInvoiceVatRegCheckController.onPageLoad(NormalMode)
    case SimplifiedInvoiceVatRegCheckPage => userAnswer => navigateFromSimplifiedInvoiceVatRegCheckPage(NormalMode)(userAnswer)
    case SupplierVatRegistrationNumberPage => _ => routes.TotalPurchaseAmountBeforeVatController.onPageLoad(NormalMode)
    case TotalPurchaseAmountBeforeVatPage => _ => routes.PurchaseTypeController.onPageLoad(NormalMode)
    case PurchaseTypePage                 => _ => routes.JourneyRecoveryController.onPageLoad()
    case _                                => _ => routes.IndexController.onPageLoad()
  }

  private val checkRoutes: Page => UserAnswers => Call = {
    case RefundingCountryPage             => userAnswers =>
      val maybeCountryCode = userAnswers.get(pages.RefundingCountryPage).orElse {
        userAnswers.get(pages.RefundingCountryNamePage).map { stored =>
          stored.split(",", 2).headOption.getOrElse(stored)
        }
      }

      maybeCountryCode match {
        case Some(code) if configLanguageMapping.languagesFor(code).size <= 1 =>
          if (configCurrencyMapping.requiresCurrencySelection(code)) routes.RefundingCurrencyController.onPageLoad(CheckMode)
          else routes.CheckYourClaimDetailsController.onPageLoad()
        case _ => routes.RefundingLanguageController.onPageLoad(CheckMode)
      }
    case RefundingLanguagePage            => userAnswers => navigateFromRefundingLanguagePage(CheckMode)(userAnswers)
    case RefundingCurrencyPage            => _ => routes.CheckYourClaimDetailsController.onPageLoad()
    case RefundPeriodPage                 => _ => routes.CheckYourClaimDetailsController.onPageLoad()
    case ContactDetailsPage               => _ => routes.CheckYourClaimDetailsController.onPageLoad()
    case BusinessActivityPage             => userAnswer => navigateFromBusinessActivityPage(CheckMode)(userAnswer)
    case BusinessActivityTwoPage          => userAnswer => navigateFromBusinessActivity2Page(CheckMode)(userAnswer)
    case BusinessActivityCodeThreePage    => _ => routes.BusinessActivityThreeController.onPageLoad()
    case InvoiceTypePage                  => userAnswer => navigateFromInvoiceTypePage(CheckMode)(userAnswer)
    case InvoiceNumberPage                => _ => routes.InvoiceDateController.onPageLoad(CheckMode)
    case InvoiceDatePage                  => _ => routes.SuppliersNameController.onPageLoad(CheckMode)
    case SuppliersNamePage                => _ => routes.SupplierAddressController.onPageLoad(CheckMode)
    case SupplierAddressPage              => _ => routes.SimplifiedInvoiceVatRegCheckController.onPageLoad(CheckMode)
    case SimplifiedInvoiceVatRegCheckPage => userAnswer => navigateFromSimplifiedInvoiceVatRegCheckPage(CheckMode)(userAnswer)
    case SupplierVatRegistrationNumberPage => _ => routes.TotalPurchaseAmountBeforeVatController.onPageLoad(CheckMode)
    case TotalPurchaseAmountBeforeVatPage => _ => routes.CheckYourClaimDetailsController.onPageLoad()
    case PurchaseTypePage                 => _ => routes.IndexController.onPageLoad()
    case _                                => _ => routes.IndexController.onPageLoad()
  }

  private def navigateFromRefundingLanguagePage(mode: Mode)(userAnswers: UserAnswers): Call = {
    val maybeCountryCode = userAnswers.get(pages.RefundingCountryPage).orElse {
      userAnswers.get(pages.RefundingCountryNamePage).map { stored =>
        stored.split(",", 2).headOption.getOrElse(stored)
      }
    }
    maybeCountryCode match {
      case Some(countryCode) if configCurrencyMapping.requiresCurrencySelection(countryCode) =>
        mode match {
          case NormalMode => routes.RefundingCurrencyController.onPageLoad(mode)
          case CheckMode  =>
            if (userAnswers.get(pages.RefundingCurrencyPage).isDefined)
              routes.CheckYourClaimDetailsController.onPageLoad()
            else
              routes.RefundingCurrencyController.onPageLoad(mode)
        }
      case Some(_) => mode match {
        case NormalMode => routes.RefundPeriodController.onPageLoad(mode)
        case CheckMode  => routes.CheckYourClaimDetailsController.onPageLoad()
      }
      case None =>
        routes.JourneyRecoveryController.onPageLoad()
    }
  }

  private def navigateFromBusinessActivityPage(mode: Mode)(userAnswers: UserAnswers): Call =
    userAnswers.get(BusinessActivityPage) match {
      case Some(true)  => routes.BusinessActivityCodeTwoController.onPageLoad(mode)
      case Some(false) => routes.CheckYourClaimDetailsController.onPageLoad()
      case _           => routes.JourneyRecoveryController.onPageLoad()
    }

  private def navigateFromBusinessActivity2Page(mode: Mode)(userAnswers: UserAnswers): Call =
    userAnswers.get(BusinessActivityTwoPage) match {
      case Some(true)  => routes.BusinessActivityCodeThreeController.onPageLoad(mode)
      case Some(false) => routes.CheckYourClaimDetailsController.onPageLoad()
      case _           => routes.JourneyRecoveryController.onPageLoad()
    }

  private def navigateFromInvoiceTypePage(mode: Mode)(userAnswers: UserAnswers): Call =
    userAnswers.get(InvoiceTypePage) match {
      case Some(InvoiceType.StandardInvoice) => routes.InvoiceNumberController.onPageLoad(mode)
      case Some(InvoiceType.SimplifiedInvoice) =>
        routes.JourneyRecoveryController.onPageLoad() // TODO - link to navigate to simp.invoicevatregcheck page
      case _ => routes.JourneyRecoveryController.onPageLoad()
    }

  private def navigateFromSimplifiedInvoiceVatRegCheckPage(mode: Mode)(userAnswers: UserAnswers): Call =
    userAnswers.get(SimplifiedInvoiceVatRegCheckPage) match {
      case Some(true)  =>
        userAnswers.get(InvoiceTypePage) match {
          // Only show the supplier VAT registration number page when the invoice type is Simplified
          case Some(InvoiceType.SimplifiedInvoice) => routes.SupplierVatRegistrationNumberController.onPageLoad(mode)
          // For other invoice types, continue to the total purchase amount page
          case _ => routes.TotalPurchaseAmountBeforeVatController.onPageLoad(mode)
        }
      // If the check was answered 'no' skip the supplier VAT registration number page and go to total purchase amount
      case Some(false) => routes.TotalPurchaseAmountBeforeVatController.onPageLoad(mode)
      case _           => routes.JourneyRecoveryController.onPageLoad()
    }

}
