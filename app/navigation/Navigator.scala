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
import controllers.purchase.routes as purchaseRoutes
import models.*
import pages.*
import play.api.mvc.Call
import utils.{ConfigLanguageMapping, ConfigPurchaseMapping, CountryCode, CurrencyConfig}

import javax.inject.{Inject, Singleton}

@Singleton
class Navigator @Inject() (currencyConfig: CurrencyConfig,
                           configLanguageMapping: ConfigLanguageMapping,
                           configPurchaseMapping: ConfigPurchaseMapping
                          ) {

  def nextPage(page: Page, mode: Mode, userAnswers: UserAnswers): Call = mode match {
    case NormalMode => normalRoutes(page)(userAnswers)
    case CheckMode  => checkRoutes(page)(userAnswers)
  }

  private val normalRoutes: Page => UserAnswers => Call = {
    case RefundingCountryPage              => userAnswers => navigateFromRefundingCountryPage(NormalMode, userAnswers)
    case RefundingLanguagePage             => userAnswers => navigateFromRefundingLanguagePage(NormalMode)(userAnswers)
    case RefundPeriodPage                  => _ => claimRoutes.ContactDetailsController.onPageLoad(NormalMode)
    case ContactDetailsPage                => _ => claimRoutes.BusinessActivityController.onPageLoad(NormalMode)
    case BusinessActivityPage              => userAnswer => navigateFromBusinessActivityPage(NormalMode)(userAnswer)
    case BusinessActivityTwoPage           => userAnswer => navigateFromBusinessActivity2Page(NormalMode)(userAnswer)
    case BusinessActivityCodeThreePage     => _ => claimRoutes.BusinessActivityThreeController.onPageLoad()
    case CheckYourStateDetailsPage         => userAnswer => navigateFromCheckYourStateDetailsPage(NormalMode)(userAnswer)
    case PurchaseTypePage                  => userAnswer => navigateFromPurchaseTypePage(NormalMode)(userAnswer)
    case PurchaseSubCategoryPage           => userAnswers => navigateFromPurchaseSubCategoryPage(NormalMode, userAnswers)
    case DescribeItemsOnInvoicePage        => _ => purchaseRoutes.InvoiceTypeController.onPageLoad(NormalMode)
    case InvoiceTypePage                   => userAnswer => navigateFromInvoiceTypePage(NormalMode)(userAnswer)
    case InvoiceNumberPage                 => userAnswers => navigateFromInvoiceNumberPage(NormalMode)(userAnswers)
    case InvoiceDatePage                   => _ => purchaseRoutes.SuppliersNameController.onPageLoad(NormalMode)
    case SuppliersNamePage                 => _ => purchaseRoutes.SupplierAddressController.onPageLoad(NormalMode)
    case SupplierAddressPage               => userAnswers => navigateFromSupplierAddressPage(NormalMode)(userAnswers)
    case SupplierTaxNumberPage             => userAnswers => navigateFromSupplierTaxNumberPage(NormalMode)(userAnswers)
    case SimplifiedInvoiceVatRegCheckPage  => userAnswers => navigateFromSimplifiedInvoiceVatRegCheckPage(NormalMode)(userAnswers)
    case SupplierVatRegistrationNumberPage => userAnswers => navigateToCurrencyOrPurchaseAmount(NormalMode)(userAnswers)
    case SupplierTaxIdentifierNumberPage   => userAnswers => navigateFromSupplierTaxIdentifierNumberPage(NormalMode)(userAnswers)
    case RefundingCurrencyPage             => userAnswers => navigateFromRefundingCurrencyPage(NormalMode)(userAnswers)
    case TotalPurchaseAmountBeforeVatPage  => _ => purchaseRoutes.TotalVatPaidController.onPageLoad(NormalMode)
    case TotalVatPaidPage                  => _ => purchaseRoutes.TotalVatClaimController.onPageLoad(NormalMode)
    case TotalVatClaimPage                 => _ => purchaseRoutes.CheckYourPurchaseDetailsController.onPageLoad()
    case _                                 => _ => controllers.routes.IndexController.onPageLoad()
  }

  private val checkRoutes: Page => UserAnswers => Call = {
    case RefundingCountryPage              => userAnswers => navigateFromRefundingCountryPage(CheckMode, userAnswers)
    case RefundingLanguagePage             => userAnswers => navigateFromRefundingLanguagePage(CheckMode)(userAnswers)
    case RefundPeriodPage                  => _ => claimRoutes.CheckYourClaimDetailsController.onPageLoad()
    case ContactDetailsPage                => _ => claimRoutes.CheckYourClaimDetailsController.onPageLoad()
    case BusinessActivityPage              => userAnswer => navigateFromBusinessActivityPage(CheckMode)(userAnswer)
    case BusinessActivityTwoPage           => userAnswer => navigateFromBusinessActivity2Page(CheckMode)(userAnswer)
    case BusinessActivityCodeThreePage     => _ => claimRoutes.BusinessActivityThreeController.onPageLoad()
    case CheckYourStateDetailsPage         => userAnswers => navigateFromCheckYourStateDetailsPage(CheckMode)(userAnswers)
    case PurchaseTypePage                  => userAnswer => navigateFromPurchaseTypePage(CheckMode)(userAnswer)
    case PurchaseSubCategoryPage           => userAnswers => navigateFromPurchaseSubCategoryPage(CheckMode, userAnswers)
    case DescribeItemsOnInvoicePage        => _ => purchaseRoutes.CheckYourPurchaseDetailsController.onPageLoad()
    case InvoiceTypePage                   => userAnswer => navigateFromInvoiceTypePage(CheckMode)(userAnswer)
    case InvoiceNumberPage                 => userAnswers => navigateFromInvoiceNumberPage(CheckMode)(userAnswers)
    case InvoiceDatePage                   => _ => purchaseRoutes.SuppliersNameController.onPageLoad(CheckMode)
    case SuppliersNamePage                 => _ => purchaseRoutes.SupplierAddressController.onPageLoad(CheckMode)
    case SupplierAddressPage               => userAnswers => navigateFromSupplierAddressPage(CheckMode)(userAnswers)
    case SupplierTaxNumberPage             => userAnswers => navigateFromSupplierTaxNumberPage(CheckMode)(userAnswers)
    case SimplifiedInvoiceVatRegCheckPage  => userAnswers => navigateFromSimplifiedInvoiceVatRegCheckPage(CheckMode)(userAnswers)
    case SupplierVatRegistrationNumberPage => userAnswers => navigateToCurrencyOrPurchaseAmount(CheckMode)(userAnswers)
    case SupplierTaxIdentifierNumberPage   => userAnswers => navigateFromSupplierTaxIdentifierNumberPage(CheckMode)(userAnswers)
    case RefundingCurrencyPage             => userAnswers => navigateFromRefundingCurrencyPage(CheckMode)(userAnswers)
    case TotalPurchaseAmountBeforeVatPage  => _ => purchaseRoutes.TotalVatPaidController.onPageLoad(CheckMode)
    case TotalVatPaidPage                  => _ => purchaseRoutes.TotalVatClaimController.onPageLoad(CheckMode)
    case TotalVatClaimPage                 => _ => purchaseRoutes.CheckYourPurchaseDetailsController.onPageLoad()
    case _                                 => _ => controllers.routes.IndexController.onPageLoad()
  }

  private def navigateFromRefundingCountryPage(mode: Mode, userAnswers: UserAnswers) = {
    CountryCode.findCountryCode(userAnswers) match {
      case Some(code) if configLanguageMapping.languagesFor(code).size <= 1 =>
        mode match {
          case NormalMode => claimRoutes.RefundPeriodController.onPageLoad(NormalMode)
          case CheckMode  => claimRoutes.CheckYourClaimDetailsController.onPageLoad()
        }
      case _ => claimRoutes.RefundingLanguageController.onPageLoad(mode)
    }
  }

  private def navigateFromRefundingLanguagePage(mode: Mode)(userAnswers: UserAnswers): Call = {
    CountryCode.findCountryCode(userAnswers) match {
      case Some(_) =>
        mode match {
          case NormalMode => claimRoutes.RefundPeriodController.onPageLoad(NormalMode)
          case CheckMode =>
            if (userAnswers.get(CountryChangedPage).contains(true)) {
              claimRoutes.RefundPeriodController.onPageLoad(CheckMode)
            } else {
              claimRoutes.CheckYourClaimDetailsController.onPageLoad()
            }
        }
      case None => controllers.routes.JourneyRecoveryController.onPageLoad()
    }
  }

  private def navigateFromRefundingCurrencyPage(mode: Mode)(userAnswers: UserAnswers): Call =
    mode match {
      case NormalMode => purchaseRoutes.TotalPurchaseAmountBeforeVatController.onPageLoad(NormalMode)
      case CheckMode =>
        if (userAnswers.get(pages.CountryChangedPage).contains(true)) {
          claimRoutes.RefundPeriodController.onPageLoad(CheckMode)
        } else {
          if (CountryCode.findCountryCode(userAnswers).contains("EE") && userAnswers.get(pages.CurrencyChangedPage).contains(true)) {
            purchaseRoutes.TotalPurchaseAmountBeforeVatController.onPageLoad(CheckMode)
          } else {
            purchaseRoutes.CheckYourPurchaseDetailsController.onPageLoad()
          }
        }
    }

  private def navigateFromBusinessActivityPage(mode: Mode)(userAnswers: UserAnswers): Call =
    userAnswers.get(BusinessActivityPage) match {
      case Some(true)  => claimRoutes.BusinessActivityCodeTwoController.onPageLoad(mode)
      case Some(false) => claimRoutes.CheckYourClaimDetailsController.onPageLoad()
      case _           => controllers.routes.JourneyRecoveryController.onPageLoad()
    }

  private def navigateFromBusinessActivity2Page(mode: Mode)(userAnswers: UserAnswers): Call =
    userAnswers.get(BusinessActivityTwoPage) match {
      case Some(true)  => claimRoutes.BusinessActivityCodeThreeController.onPageLoad(mode)
      case Some(false) => claimRoutes.CheckYourClaimDetailsController.onPageLoad()
      case _           => controllers.routes.JourneyRecoveryController.onPageLoad()
    }

  private def navigateFromInvoiceTypePage(mode: Mode)(userAnswers: UserAnswers): Call = {
    userAnswers.get(InvoiceTypePage) match {
      case Some(InvoiceType.StandardInvoice)   => purchaseRoutes.InvoiceNumberController.onPageLoad(mode)
      case Some(InvoiceType.SimplifiedInvoice) => purchaseRoutes.InvoiceNumberController.onPageLoad(mode)
      case _                                   => controllers.routes.JourneyRecoveryController.onPageLoad()
    }
  }

  private def navigateToCurrencyOrPurchaseAmount(mode: Mode)(userAnswers: UserAnswers): Call = {
    CountryCode.findCountryCode(userAnswers) match {
      case Some(countryCode) if currencyConfig.requiresCurrencySelection(countryCode) =>
        purchaseRoutes.RefundingCurrencyController.onPageLoad(mode)
      case Some(_) => purchaseRoutes.TotalPurchaseAmountBeforeVatController.onPageLoad(mode)
      case None    => controllers.routes.JourneyRecoveryController.onPageLoad()
    }
  }

  private def navigateFromSimplifiedInvoiceVatRegCheckPage(mode: Mode)(userAnswers: UserAnswers): Call =
    userAnswers.get(SimplifiedInvoiceVatRegCheckPage) match {
      case Some(true) =>
        userAnswers.get(InvoiceTypePage) match {
          case Some(InvoiceType.SimplifiedInvoice) => purchaseRoutes.SupplierVatRegistrationNumberController.onPageLoad(mode)
          case _ =>
            if (mode == CheckMode && userAnswers.get(pages.InvoiceTypeChangedPage).contains(true))
              purchaseRoutes.CheckYourPurchaseDetailsController.onPageLoad()
            else
              CountryCode.findCountryCode(userAnswers) match {
                case Some(country) if currencyConfig.requiresCurrencySelection(country) =>
                  purchaseRoutes.RefundingCurrencyController.onPageLoad(mode)
                case _ => purchaseRoutes.TotalPurchaseAmountBeforeVatController.onPageLoad(mode)
              }
        }
      case Some(false) =>
        if (mode == CheckMode && userAnswers.get(pages.InvoiceTypeChangedPage).contains(true))
          purchaseRoutes.CheckYourPurchaseDetailsController.onPageLoad()
        else
          CountryCode.findCountryCode(userAnswers) match {
            case Some(country) if currencyConfig.requiresCurrencySelection(country) =>
              purchaseRoutes.RefundingCurrencyController.onPageLoad(mode)
            case _ => purchaseRoutes.TotalPurchaseAmountBeforeVatController.onPageLoad(mode)
          }
      case _ => controllers.routes.JourneyRecoveryController.onPageLoad()
    }

  private def navigateFromSupplierVatRegistrationPage(mode: Mode)(userAnswers: UserAnswers): Call = {
    if (mode == CheckMode && userAnswers.get(pages.InvoiceTypeChangedPage).contains(true)) {
      purchaseRoutes.CheckYourPurchaseDetailsController.onPageLoad()
    } else {
      purchaseRoutes.TotalPurchaseAmountBeforeVatController.onPageLoad(mode)
    }
  }

  private def navigateFromPurchaseTypePage(mode: Mode)(userAnswers: UserAnswers): Call =
    userAnswers.get(PurchaseTypePage) match {
      case Some(parent) =>
        CountryCode.findCountryCode(userAnswers) match {
          case Some(country) =>
            val subs = configPurchaseMapping.subcodesFor(country, parent.toString)
            if (subs.nonEmpty) {
              Call("GET", s"/${PurchaseType.urlSlugForPurchaseType(parent)}")
            } else {
              if (mode == CheckMode) { purchaseRoutes.CheckYourPurchaseDetailsController.onPageLoad() }
              else { purchaseRoutes.InvoiceTypeController.onPageLoad(mode) }
            }
          case _ => purchaseRoutes.DescribeItemsOnInvoiceController.onPageLoad(mode)
        }

      case _ => controllers.routes.JourneyRecoveryController.onPageLoad()
    }

  private def navigateFromSupplierAddressPage(mode: Mode)(userAnswers: UserAnswers): Call = {
    val maybeInvoiceType = userAnswers.get(InvoiceTypePage)

    CountryCode.findCountryCode(userAnswers) match {
      case Some("DE") => purchaseRoutes.SupplierTaxNumberController.onPageLoad(mode)
      case Some(_) =>
        maybeInvoiceType match {
          case Some(InvoiceType.StandardInvoice)   => purchaseRoutes.SupplierVatRegistrationNumberController.onPageLoad(mode)
          case Some(InvoiceType.SimplifiedInvoice) => purchaseRoutes.SimplifiedInvoiceVatRegCheckController.onPageLoad(mode)
          case None                                => purchaseRoutes.SimplifiedInvoiceVatRegCheckController.onPageLoad(mode)
        }
      case None => purchaseRoutes.SimplifiedInvoiceVatRegCheckController.onPageLoad(mode)
    }
  }

  private def navigateFromPurchaseSubCategoryPage(mode: Mode, userAnswers: UserAnswers): Call = {
    userAnswers.get(PurchaseTypePage) match {
      case Some(_) =>
        if (mode == CheckMode) { purchaseRoutes.CheckYourPurchaseDetailsController.onPageLoad() }
        else { purchaseRoutes.InvoiceTypeController.onPageLoad(mode) }
      case _ => controllers.routes.JourneyRecoveryController.onPageLoad()
    }
  }

  private def navigateFromSupplierTaxNumberPage(mode: Mode)(userAnswers: UserAnswers): Call =
    userAnswers.get(SupplierTaxNumberPage) match {
      case Some(SupplierTaxNumber.Vatregistrationnumber) =>
        purchaseRoutes.SupplierVatRegistrationNumberController.onPageLoad(mode)
      case Some(SupplierTaxNumber.Taxidentifiernumber) =>
        purchaseRoutes.SupplierTaxIdentifierNumberController.onPageLoad(mode)
      case Some(SupplierTaxNumber.Neither) =>
        if (mode == CheckMode) {
          purchaseRoutes.CheckYourPurchaseDetailsController.onPageLoad()
        } else {
          navigateToCurrencyOrPurchaseAmount(mode)(userAnswers)
        }
      case _ => controllers.routes.JourneyRecoveryController.onPageLoad()
    }

  private def navigateFromSupplierTaxIdentifierNumberPage(mode: Mode)(userAnswers: UserAnswers): Call = {
    CountryCode.findCountryCode(userAnswers) match {
      case Some(code) if currencyConfig.requiresCurrencySelection(code) => purchaseRoutes.RefundingCurrencyController.onPageLoad(mode)
      case Some(code) if code == "DE" || (mode == CheckMode && currencyConfig.currencyConfig.get(code).exists(_.nonEmpty)) =>
        purchaseRoutes.TotalPurchaseAmountBeforeVatController.onPageLoad(mode)
      case _ => controllers.routes.JourneyRecoveryController.onPageLoad()
    }
  }

  private def navigateFromCheckYourStateDetailsPage(mode: Mode)(userAnswers: UserAnswers): Call =
    userAnswers.get(CheckYourStateDetailsPage) match {
      case Some(true)  => controllers.routes.JourneyRecoveryController.onPageLoad() // TODO: replace when F8 delete application is in place
      case Some(false) => claimRoutes.CheckYourClaimDetailsController.onPageLoad()
      case _           => controllers.routes.JourneyRecoveryController.onPageLoad()
    }

  private def navigateFromInvoiceNumberPage(mode: Mode)(answers: UserAnswers): Call =
    answers.get(VrnWarningFlowPage) match {
      case Some(true)  => controllers.warning.routes.SupplierVrnWarningController.onPageLoad(mode)
      case Some(false) => purchaseRoutes.SupplierVatRegistrationNumberController.onPageLoad(mode)
      case None        => purchaseRoutes.InvoiceDateController.onPageLoad(mode)
    }

}
