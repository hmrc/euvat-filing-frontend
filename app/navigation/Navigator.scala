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
import controllers.imports.routes as importRoutes
import controllers.purchase.routes as purchaseRoutes
import models.*
import models.PurchaseOrImport.{Import, Purchase}
import pages.*
import play.api.mvc.Call
import utils.{ConfigLanguageMapping, ConfigPurchaseOrImportMapping, CountryCode, CurrencyConfig}

import javax.inject.{Inject, Singleton}

@Singleton
class Navigator @Inject() (claimNavigator: ClaimNavigator, purchaseNavigator: PurchaseNavigator, importNavigator: ImportNavigator) {

  def nextPage(page: Page, mode: Mode, userAnswers: UserAnswers): Call = mode match {
    case NormalMode => normalRoutes(page)(userAnswers)
    case CheckMode  => checkRoutes(page)(userAnswers)
  }

  private val normalRoutes: Page => UserAnswers => Call = {
    case RefundingCountryPage              => userAnswers => claimNavigator.navigateFromRefundingCountryPage(NormalMode, userAnswers)
    case RefundingLanguagePage             => userAnswers => claimNavigator.navigateFromRefundingLanguagePage(NormalMode)(userAnswers)
    case RefundPeriodPage                  => _ => claimRoutes.ContactDetailsController.onPageLoad(NormalMode)
    case ContactDetailsPage                => _ => claimRoutes.BusinessActivityController.onPageLoad(NormalMode)
    case BusinessActivityPage              => userAnswers => claimNavigator.navigateFromBusinessActivityPage(NormalMode)(userAnswers)
    case BusinessActivityTwoPage           => userAnswers => claimNavigator.navigateFromBusinessActivity2Page(NormalMode)(userAnswers)
    case BusinessActivityCodeThreePage     => _ => claimRoutes.BusinessActivityThreeController.onPageLoad()
    case CheckYourStateDetailsPage         => userAnswers => claimNavigator.navigateFromCheckYourStateDetailsPage(NormalMode)(userAnswers)
    case PurchaseOrImportPage              => userAnswers => navigateFromPurchaseOrImportPage(userAnswers)
    case PurchaseTypePage                  => userAnswers => purchaseNavigator.navigateFromPurchaseTypePage(NormalMode)(userAnswers)
    case PurchaseSubCategoryPage           => userAnswers => purchaseNavigator.navigateFromPurchaseSubCategoryPage(NormalMode, userAnswers)
    case DescribeItemsOnInvoicePage        => _ => purchaseRoutes.InvoiceTypeController.onPageLoad(NormalMode)
    case InvoiceTypePage                   => _ => purchaseRoutes.InvoiceNumberController.onPageLoad(NormalMode)
    case InvoiceNumberPage                 => userAnswers => purchaseNavigator.navigateFromInvoiceNumberPage(NormalMode)(userAnswers)
    case InvoiceDatePage                   => _ => purchaseRoutes.SuppliersNameController.onPageLoad(NormalMode)
    case SuppliersNamePage                 => _ => purchaseRoutes.SupplierAddressController.onPageLoad(NormalMode)
    case SupplierAddressPage               => userAnswers => purchaseNavigator.navigateFromSupplierAddressPage(NormalMode)(userAnswers)
    case SimplifiedInvoiceVatRegCheckPage  => userAnswers => purchaseNavigator.navigateFromSimplifiedInvoiceVatRegCheckPage(NormalMode)(userAnswers)
    case SupplierTaxNumberPage             => userAnswers => purchaseNavigator.navigateFromSupplierTaxNumberPage(NormalMode)(userAnswers)
    case SupplierVatRegistrationNumberPage => userAnswers => purchaseNavigator.navigateFromSupplierVatRegistrationPage()(userAnswers)
    case SupplierTaxIdentifierNumberPage   => userAnswers => purchaseNavigator.navigateFromSupplierTaxIdentifierNumberPage()(userAnswers)
    case RefundingCurrencyPage             => userAnswers => purchaseNavigator.navigateFromRefundingCurrencyPage(NormalMode)(userAnswers)
    case TotalPurchaseAmountBeforeVatPage  => _ => purchaseRoutes.TotalVatPaidController.onPageLoad(NormalMode)
    case TotalVatPaidPage                  => _ => purchaseRoutes.TotalVatClaimController.onPageLoad(NormalMode)
    case TotalVatClaimPage                 => _ => purchaseRoutes.CheckYourPurchaseDetailsController.onPageLoad()
    case ImportTypePage                    => userAnswers => importNavigator.navigateFromImportTypePage(NormalMode)(userAnswers)
    case ImportSubCodePage                 => userAnswers => importNavigator.navigateFromImportSubCodePage(NormalMode)(userAnswers)
    case SadReferencePage                  => userAnswers => importNavigator.navigateFromSadReferencePage(NormalMode)(userAnswers)
    case ImportDetailsInfoPage             => userAnswers => importNavigator.navigateFromImportDetailsInfoPage(NormalMode)(userAnswers)
    case _                                 => _ => controllers.routes.IndexController.onPageLoad()
  }

  private val checkRoutes: Page => UserAnswers => Call = {
    case RefundingCountryPage              => userAnswers => claimNavigator.navigateFromRefundingCountryPage(CheckMode, userAnswers)
    case RefundingLanguagePage             => userAnswers => claimNavigator.navigateFromRefundingLanguagePage(CheckMode)(userAnswers)
    case RefundPeriodPage                  => _ => claimRoutes.CheckYourClaimDetailsController.onPageLoad()
    case ContactDetailsPage                => _ => claimRoutes.CheckYourClaimDetailsController.onPageLoad()
    case BusinessActivityPage              => userAnswers => claimNavigator.navigateFromBusinessActivityPage(CheckMode)(userAnswers)
    case BusinessActivityTwoPage           => userAnswers => claimNavigator.navigateFromBusinessActivity2Page(CheckMode)(userAnswers)
    case BusinessActivityCodeThreePage     => _ => claimRoutes.BusinessActivityThreeController.onPageLoad()
    case CheckYourStateDetailsPage         => userAnswers => claimNavigator.navigateFromCheckYourStateDetailsPage(CheckMode)(userAnswers)
    case PurchaseTypePage                  => userAnswers => purchaseNavigator.navigateFromPurchaseTypePage(CheckMode)(userAnswers)
    case PurchaseSubCategoryPage           => userAnswers => purchaseNavigator.navigateFromPurchaseSubCategoryPage(CheckMode, userAnswers)
    case DescribeItemsOnInvoicePage        => _ => purchaseRoutes.CheckYourPurchaseDetailsController.onPageLoad()
    case InvoiceTypePage                   => _ => purchaseRoutes.InvoiceNumberController.onPageLoad(CheckMode)
    case InvoiceNumberPage                 => userAnswers => purchaseNavigator.navigateFromInvoiceNumberPage(CheckMode)(userAnswers)
    case InvoiceDatePage                   => _ => purchaseRoutes.SuppliersNameController.onPageLoad(CheckMode)
    case SuppliersNamePage                 => _ => purchaseRoutes.SupplierAddressController.onPageLoad(CheckMode)
    case SupplierAddressPage               => userAnswers => purchaseNavigator.navigateFromSupplierAddressPage(CheckMode)(userAnswers)
    case SimplifiedInvoiceVatRegCheckPage  => userAnswers => purchaseNavigator.navigateFromSimplifiedInvoiceVatRegCheckPage(CheckMode)(userAnswers)
    case SupplierTaxNumberPage             => userAnswers => purchaseNavigator.navigateFromSupplierTaxNumberPage(CheckMode)(userAnswers)
    case SupplierVatRegistrationNumberPage => userAnswers => purchaseNavigator.navigateFromSupplierVatRegistrationPage()(userAnswers)
    case SupplierTaxIdentifierNumberPage   => userAnswers => purchaseNavigator.navigateFromSupplierTaxIdentifierNumberPage()(userAnswers)
    case RefundingCurrencyPage             => userAnswers => purchaseNavigator.navigateFromRefundingCurrencyPage(CheckMode)(userAnswers)
    case TotalPurchaseAmountBeforeVatPage  => _ => purchaseRoutes.TotalVatPaidController.onPageLoad(CheckMode)
    case TotalVatPaidPage                  => _ => purchaseRoutes.TotalVatClaimController.onPageLoad(CheckMode)
    case TotalVatClaimPage                 => _ => purchaseRoutes.CheckYourPurchaseDetailsController.onPageLoad()
    case ImportTypePage                    => userAnswers => importNavigator.navigateFromImportTypePage(CheckMode)(userAnswers)
    case ImportSubCodePage                 => _ => importRoutes.SadReferenceController.onPageLoad
    case SadReferencePage                  => userAnswers => importNavigator.navigateFromSadReferencePage(CheckMode)(userAnswers)
    case ImportDetailsInfoPage             => userAnswers => importNavigator.navigateFromImportDetailsInfoPage(CheckMode)(userAnswers)
    case _                                 => _ => controllers.routes.IndexController.onPageLoad()
  }

  private def navigateFromPurchaseOrImportPage(userAnswers: UserAnswers): Call =
    userAnswers.get(PurchaseOrImportPage) match {
      case Some(Purchase) => purchaseRoutes.PurchaseTypeController.onPageLoad(NormalMode)
      case Some(Import)   => importRoutes.ImportTypeController.onPageLoad(NormalMode)
      case None           => controllers.routes.JourneyRecoveryController.onPageLoad()
    }

}
