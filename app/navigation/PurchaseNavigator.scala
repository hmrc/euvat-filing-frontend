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

import controllers.purchase.routes as purchaseRoutes
import models.{CheckMode, InvoiceType, Mode, NormalMode, PurchaseOrImportType, SupplierTaxNumber, UserAnswers}
import pages.*
import play.api.mvc.Call
import utils.{ConfigPurchaseOrImportMapping, CountryCode, CurrencyConfig}

import javax.inject.{Inject, Singleton}

@Singleton
class PurchaseNavigator @Inject() (currencyConfig: CurrencyConfig, configPurchaseOrImportMapping: ConfigPurchaseOrImportMapping) {
  def navigateFromPurchaseTypePage(mode: Mode)(userAnswers: UserAnswers): Call =
    (userAnswers.get(PurchaseTypePage), CountryCode.findCountryCode(userAnswers)) match {
      case (Some(purchaseTypeCode), Some(country)) =>
        if (configPurchaseOrImportMapping.subcodesFor(country, purchaseTypeCode.toString).nonEmpty) {
          Call("GET", s"/${PurchaseOrImportType.urlSlugForPurchaseType(purchaseTypeCode)}")
        } else {
          mode match {
            case CheckMode => purchaseRoutes.CheckYourPurchaseDetailsController.onPageLoad()
            case _         => purchaseRoutes.InvoiceTypeController.onPageLoad(mode)
          }
        }
      case (Some(_), None) => purchaseRoutes.DescribeItemsOnInvoiceController.onPageLoad(mode)
      case _               => controllers.routes.JourneyRecoveryController.onPageLoad()
    }

  def navigateFromPurchaseSubCategoryPage(mode: Mode, userAnswers: UserAnswers): Call = {
    userAnswers.get(PurchaseTypePage) match {
      case Some(_) =>
        if (mode == CheckMode) {
          purchaseRoutes.CheckYourPurchaseDetailsController.onPageLoad()
        } else {
          purchaseRoutes.InvoiceTypeController.onPageLoad(mode)
        }
      case _ => controllers.routes.JourneyRecoveryController.onPageLoad()
    }
  }

  def navigateFromInvoiceNumberPage(mode: Mode)(answers: UserAnswers): Call =
    mode match {
      case NormalMode => purchaseRoutes.InvoiceDateController.onPageLoad(NormalMode)
      case CheckMode =>
        if (answers.get(SupplierVatRegistrationNumberPage).isDefined) {
          purchaseRoutes.SupplierVatRegistrationNumberController.onPageLoad(CheckMode)
        } else if (answers.get(SupplierTaxIdentifierNumberPage).isDefined) {
          purchaseRoutes.SupplierTaxIdentifierNumberController.onPageLoad(CheckMode)
        } else {
          purchaseRoutes.CheckYourPurchaseDetailsController.onPageLoad()
        }
    }

  def navigateFromSupplierAddressPage(mode: Mode)(userAnswers: UserAnswers): Call = {
    CountryCode.findCountryCode(userAnswers) match {
      case Some("DE") => purchaseRoutes.SupplierTaxNumberController.onPageLoad(mode)
      case _ =>
        userAnswers.get(InvoiceTypePage) match {
          case Some(InvoiceType.StandardInvoice) => purchaseRoutes.SupplierVatRegistrationNumberController.onPageLoad(mode)
          case _                                 => purchaseRoutes.SimplifiedInvoiceVatRegCheckController.onPageLoad(mode)
        }
    }
  }

  def navigateFromSimplifiedInvoiceVatRegCheckPage(mode: Mode)(userAnswers: UserAnswers): Call =
    userAnswers.get(SimplifiedInvoiceVatRegCheckPage) match {
      case Some(true) => purchaseRoutes.SupplierVatRegistrationNumberController.onPageLoad(mode)
      case _ =>
        CountryCode.findCountryCode(userAnswers) match {
          case Some(country) if currencyConfig.requiresCurrencySelection(country) =>
            purchaseRoutes.RefundingCurrencyController.onPageLoad(mode)
          case _ => purchaseRoutes.TotalPurchaseAmountBeforeVatController.onPageLoad(mode)
        }
    }

  def navigateFromSupplierTaxNumberPage(mode: Mode)(userAnswers: UserAnswers): Call =
    userAnswers.get(SupplierTaxNumberPage) match {
      case Some(SupplierTaxNumber.Vatregistrationnumber) => purchaseRoutes.SupplierVatRegistrationNumberController.onPageLoad(mode)
      case Some(SupplierTaxNumber.Taxidentifiernumber)   => purchaseRoutes.SupplierTaxIdentifierNumberController.onPageLoad(mode)
      case Some(SupplierTaxNumber.Neither) =>
        if (mode == CheckMode) {
          purchaseRoutes.CheckYourPurchaseDetailsController.onPageLoad()
        } else {
          purchaseRoutes.TotalPurchaseAmountBeforeVatController.onPageLoad(NormalMode)
        }
      case _ => controllers.routes.JourneyRecoveryController.onPageLoad()
    }

  def navigateFromSupplierVatRegistrationPage()(userAnswers: UserAnswers): Call = {
    if (userAnswers.get(TotalPurchaseAmountBeforeVatPage).isDefined) {
      purchaseRoutes.CheckYourPurchaseDetailsController.onPageLoad()
    } else {
      CountryCode.findCountryCode(userAnswers) match {
        case Some(countryCode) if currencyConfig.requiresCurrencySelection(countryCode) =>
          purchaseRoutes.RefundingCurrencyController.onPageLoad(NormalMode)
        case _ => purchaseRoutes.TotalPurchaseAmountBeforeVatController.onPageLoad(NormalMode)
      }
    }
  }

  def navigateFromSupplierTaxIdentifierNumberPage()(userAnswers: UserAnswers): Call = {
    if (userAnswers.get(TotalPurchaseAmountBeforeVatPage).isDefined) {
      purchaseRoutes.CheckYourPurchaseDetailsController.onPageLoad()
    } else {
      purchaseRoutes.TotalPurchaseAmountBeforeVatController.onPageLoad(NormalMode)
    }
  }

  def navigateFromRefundingCurrencyPage(mode: Mode)(userAnswers: UserAnswers): Call =
    mode match {
      case NormalMode => purchaseRoutes.TotalPurchaseAmountBeforeVatController.onPageLoad(NormalMode)
      case CheckMode =>
        if (userAnswers.get(CurrencyChangedPage).contains(true)) {
          purchaseRoutes.TotalPurchaseAmountBeforeVatController.onPageLoad(CheckMode)
        } else {
          purchaseRoutes.CheckYourPurchaseDetailsController.onPageLoad()
        }
    }

}
