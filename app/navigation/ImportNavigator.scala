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

import models.{CheckMode, Mode, NormalMode, Other, UserAnswers}
import pages.{ImportSubCategoryPage, ImportSubCodePage, ImportTypePage, SadReferenceCheckPage}
import play.api.mvc.Call
import utils.{ConfigPurchaseOrImportMapping, CountryCode, CurrencyConfig}
import controllers.imports.routes as importsRoutes
import utils.PurchaseOrImportHelpers.isNoneSelection

import javax.inject.{Inject, Singleton}

@Singleton
class ImportNavigator @Inject() (currencyConfig: CurrencyConfig, configPurchaseOrImportMapping: ConfigPurchaseOrImportMapping) {

  def navigateFromImportTypePage(mode: Mode)(userAnswers: UserAnswers): Call =
    (userAnswers.get(ImportTypePage), CountryCode.findCountryCode(userAnswers)) match {
      case (Some(importType), Some(country)) if configPurchaseOrImportMapping.selectableSubcodes(country, importType.toString).isDefined =>
        importsRoutes.ImportSubCodeController.onPageLoad(importType.toString)
      case (Some(Other), Some(_)) =>
        importsRoutes.DescribeItemsOnImportDocController.onPageLoad(mode)
      case (Some(_), Some(_)) =>
        importsRoutes.SadReferenceCheckController.onPageLoad(mode)
      case _ =>
        controllers.routes.JourneyRecoveryController.onPageLoad()
    }

  def navigateFromImportSubCodePage(mode: Mode)(userAnswers: UserAnswers): Call =
    (userAnswers.get(ImportTypePage), userAnswers.get(ImportSubCodePage), CountryCode.findCountryCode(userAnswers)) match {
      case (Some(Other), Some(subCode), _) if isNoneSelection(subCode) =>
        importsRoutes.DescribeItemsOnImportDocController.onPageLoad(mode)
      case (Some(importType), Some(subCode), Some(country))
          if configPurchaseOrImportMapping.subcategoriesFor(country, importType.toString, subCode).nonEmpty =>
        importsRoutes.ImportSubCategoryController.onPageLoad(mode)
      case (_, Some(_), _) =>
        importsRoutes.SadReferenceCheckController.onPageLoad(mode)
      case _ =>
        controllers.routes.JourneyRecoveryController.onPageLoad()
    }

  def navigateFromImportSubCategoryPage(mode: Mode)(userAnswers: UserAnswers): Call =
    userAnswers.get(ImportSubCategoryPage) match {
      case Some(_) => importsRoutes.SadReferenceCheckController.onPageLoad(mode)
      case None    => controllers.routes.JourneyRecoveryController.onPageLoad()
    }

  def navigateFromSadReferenceCheckPage(mode: Mode)(userAnswers: UserAnswers): Call =
    userAnswers.get(SadReferenceCheckPage) match {
      case Some(true)  => importsRoutes.SadReferenceNumberController.onPageLoad(mode)
      case Some(false) => importsRoutes.ImportDetailsInfoController.onPageLoad(mode)
      case _           => controllers.routes.JourneyRecoveryController.onPageLoad()
    }

  def navigateToCurrencyOrNextPage(mode: Mode)(userAnswers: UserAnswers): Call =
    CountryCode.findCountryCode(userAnswers) match {
      case Some(countryCode) if currencyConfig.requiresCurrencySelection(countryCode) =>
        importsRoutes.ImportCurrencyController.onPageLoad(mode)
      case Some(_) => importsRoutes.TotalAmountWithoutVatController.onPageLoad(mode) // TODO: next import page
      case None    => controllers.routes.JourneyRecoveryController.onPageLoad()
    }

  def navigateFromSadReferenceNumberPage(mode: Mode)(userAnswers: UserAnswers): Call =
    userAnswers.get(SadReferenceCheckPage) match {
      case Some(true)  => importsRoutes.ImportSuppliersNameController.onPageLoad(mode)
      case Some(false) => importsRoutes.ImportDetailsInfoController.onPageLoad(mode)
      case None        => importsRoutes.ImportDetailsInfoController.onPageLoad(mode)
    }

  // TODO: replace once the page following total amount without VAT is built
  def navigateFromTotalAmountWithoutVatPage(mode: Mode)(userAnswers: UserAnswers): Call =
    controllers.routes.JourneyRecoveryController.onPageLoad()

}
