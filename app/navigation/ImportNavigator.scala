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

import controllers.imports.routes as importsRoutes
import models.{CheckMode, Mode, Other, UserAnswers}
import pages.imports.*
import play.api.mvc.Call
import utils.PurchaseOrImportHelpers.isNoneSelection
import utils.{ConfigPurchaseOrImportMapping, CountryCode, CurrencyConfig}
import javax.inject.{Inject, Singleton}

@Singleton
class ImportNavigator @Inject() (currencyConfig: CurrencyConfig, configPurchaseOrImportMapping: ConfigPurchaseOrImportMapping) {
  private def cya: Call = importsRoutes.CheckYourImportDetailsController.onPageLoad()
  private def recovery: Call = controllers.routes.JourneyRecoveryController.onPageLoad()
  private lazy val missingAnswers = new ImportMissingAnswersNavigator(currencyConfig, configPurchaseOrImportMapping)

  // Used only by import CheckMode routes; purchase and claim navigation are unaffected.
  def continueImportEdit(answers: UserAnswers)(ordinary: => Call): Call =
    if (answers.get(ImportMissingAnswersJourneyPage).contains(true)) missingAnswers.firstMissingPage(answers).getOrElse(cya)
    else ordinary

  def navigateFromImportTypePage(mode: Mode)(answers: UserAnswers): Call =
    (answers.get(ImportTypePage), CountryCode.findCountryCode(answers)) match {
      case (Some(_), Some(_)) if mode == CheckMode && !answers.get(ImportTypeChangedPage).contains(true) => cya
      case (Some(importType), Some(country)) if configPurchaseOrImportMapping.selectableSubcodes(country, importType.toString).isDefined =>
        importsRoutes.ImportSubCodeController.onPageLoad(importType.toString, mode)
      case (Some(Other), Some(_)) => importsRoutes.DescribeItemsOnImportDocController.onPageLoad(mode)
      case (Some(_), Some(_))     => if (mode == CheckMode) cya else importsRoutes.SadReferenceCheckController.onPageLoad(mode)
      case _                      => recovery
    }

  def navigateFromImportSubCodePage(mode: Mode)(answers: UserAnswers): Call =
    (answers.get(ImportTypePage), answers.get(ImportSubCodePage), CountryCode.findCountryCode(answers)) match {
      case (Some(_), Some(_), Some(_)) if mode == CheckMode && !answers.get(ImportSubCodeChangedPage).contains(true) => cya
      case (Some(Other), Some(code), Some(_)) if isNoneSelection(code) => importsRoutes.DescribeItemsOnImportDocController.onPageLoad(mode)
      case (Some(importType), Some(code), Some(country))
        if configPurchaseOrImportMapping.subcategoriesFor(country, importType.toString, code).nonEmpty =>
        importsRoutes.ImportSubCategoryController.onPageLoad(mode)
      case (Some(_), Some(_), Some(_)) => if (mode == CheckMode) cya else importsRoutes.SadReferenceCheckController.onPageLoad(mode)
      case _                           => recovery
    }

  def navigateFromImportSubCategoryPage(mode: Mode)(answers: UserAnswers): Call =
    answers.get(ImportSubCategoryPage) match {
      case Some(_) => if (mode == CheckMode) cya else importsRoutes.SadReferenceCheckController.onPageLoad(mode)
      case None    => recovery
    }

  def navigateFromSadReferenceCheckPage(mode: Mode)(answers: UserAnswers): Call =
    answers.get(SadReferenceCheckPage) match {
      case None                                                                              => recovery
      case Some(_) if mode == CheckMode && !answers.get(ImportSadChangedPage).contains(true) => cya
      case Some(true)  => importsRoutes.SadReferenceNumberController.onPageLoad(mode)
      case Some(false) => importsRoutes.ImportDetailsInfoController.onPageLoad(mode)
    }

  def navigateFromSupplierAddressPage(mode: Mode)(answers: UserAnswers): Call =
    if (mode == CheckMode) cya
    else
      CountryCode.findCountryCode(answers) match {
        case Some(country) if currencyConfig.requiresCurrencySelection(country) => importsRoutes.ImportCurrencyController.onPageLoad(mode)
        case _                                                                  => importsRoutes.TotalAmountWithoutVatController.onPageLoad(mode)
      }

  def navigateFromImportCurrencyPage(mode: Mode)(answers: UserAnswers): Call =
    if (mode == CheckMode && !answers.get(ImportCurrencyChangedPage).contains(true)) cya
    else importsRoutes.TotalAmountWithoutVatController.onPageLoad(mode)

  def navigateFromTotalAmountWithoutVatPage(mode: Mode)(answers: UserAnswers): Call =
    answers.get(TotalAmountWithoutVatPage) match {
      case None                                                                                           => recovery
      case Some(_) if mode == CheckMode && !answers.get(ImportAmountWithoutVatChangedPage).contains(true) => cya
      case Some(_) => importsRoutes.TotalVatPaidImportController.onPageLoad(mode)
    }

  def navigateFromTotalVatPaidImportPage(mode: Mode)(answers: UserAnswers): Call =
    answers.get(TotalVatPaidImportPage) match {
      case None                                                                                  => recovery
      case Some(_) if mode == CheckMode && !answers.get(ImportVatPaidChangedPage).contains(true) => cya
      case Some(_) => importsRoutes.TotalVatClaimController.onPageLoad(mode)
    }

  def navigateFromImportTotalVatClaimPage(mode: Mode)(answers: UserAnswers): Call =
    if (answers.get(ImportTotalVatClaimPage).isDefined) cya else recovery

  def navigateFromCheckYourImportDetails(answers: UserAnswers): Call =
    missingAnswers.navigateFromCheckYourImportDetails(answers)

  def firstMissingImportPage(answers: UserAnswers): Option[Call] =
    missingAnswers.firstMissingPage(answers)
}
