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

import models.{Mode, UserAnswers}
import pages.{ImportSubCategoryPage, ImportSubCodePage, ImportTypePage}
import play.api.mvc.Call
import utils.{ConfigPurchaseOrImportMapping, CountryCode, CurrencyConfig}
import controllers.imports.routes as importsRoutes

import javax.inject.{Inject, Singleton}

@Singleton
class ImportNavigator @Inject() (currencyConfig: CurrencyConfig, configPurchaseOrImportMapping: ConfigPurchaseOrImportMapping) {

  def navigateFromImportTypePage(mode: Mode)(userAnswers: UserAnswers): Call =
    (userAnswers.get(ImportTypePage), CountryCode.findCountryCode(userAnswers)) match {
      case (Some(importType), Some(country)) if configPurchaseOrImportMapping.selectableSubcodes(country, importType.toString).isDefined =>
        importsRoutes.ImportSubCodeController.onPageLoad(importType.toString)
      case (Some(_), Some(_)) =>
        importsRoutes.SadReferenceController.onPageLoad // TODO: Other with only 10.99 may go to the import free text page when it exists
      case _ =>
        controllers.routes.JourneyRecoveryController.onPageLoad()
    }

  def navigateFromImportSubCodePage(mode: Mode)(userAnswers: UserAnswers): Call =
    (userAnswers.get(ImportTypePage), userAnswers.get(ImportSubCodePage), CountryCode.findCountryCode(userAnswers)) match {
      case (Some(importType), Some(subCode), Some(country))
          if configPurchaseOrImportMapping.subcategoriesFor(country, importType.toString, subCode).nonEmpty =>
        importsRoutes.ImportSubCategoryController.onPageLoad(mode)
      case (_, Some(_), _) =>
        importsRoutes.SadReferenceController.onPageLoad
      case _ =>
        controllers.routes.JourneyRecoveryController.onPageLoad()
    }

  def navigateFromImportSubCategoryPage(userAnswers: UserAnswers): Call =
    userAnswers.get(ImportSubCategoryPage) match {
      case Some(_) => importsRoutes.SadReferenceController.onPageLoad
      case None    => controllers.routes.JourneyRecoveryController.onPageLoad()
    }

}
