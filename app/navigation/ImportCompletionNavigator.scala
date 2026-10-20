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

import controllers.imports.routes
import models.{CheckMode, Other, UserAnswers}
import pages.*
import pages.imports.ImportSupplierAddressPage
import play.api.mvc.Call
import utils.{ConfigPurchaseOrImportMapping, CountryCode, CurrencyConfig}
import utils.PurchaseOrImportHelpers.{allowedValues, isNoneSelection}

import javax.inject.Inject

class ImportCompletionNavigator @Inject() (
                                            currencyConfig: CurrencyConfig,
                                            config: ConfigPurchaseOrImportMapping
                                          ) {

  private def nonBlank(value: Option[String]): Boolean =
    value.exists(_.trim.nonEmpty)

  def firstMissingPage(answers: UserAnswers): Option[Call] =
    CountryCode.findCountryCode(answers) match {
      case None =>
        Some(controllers.routes.JourneyRecoveryController.onPageLoad())

      case Some(country) =>
        categoryMissingPage(answers, country)
          .orElse(documentMissingPage(answers))
          .orElse(supplierMissingPage(answers))
          .orElse(amountMissingPage(answers, country))
    }

  private def categoryMissingPage(
                                   answers: UserAnswers,
                                   country: String
                                 ): Option[Call] =
    answers.get(ImportTypePage) match {
      case None =>
        Some(routes.ImportTypeController.onPageLoad(CheckMode))

      case Some(importType) =>
        val subcodes =
          config.selectableSubcodes(country, importType.toString)

        subcodes match {
          case Some(options) =>
            answers.get(ImportSubCodePage) match {
              case Some(code) if allowedValues(options).contains(code) =>
                if (importType == Other && isNoneSelection(code)) {
                  missingDescription(answers)
                } else {
                  val children = config.subcategoriesFor(
                    country,
                    importType.toString,
                    code
                  )

                  Option.when(
                    children.nonEmpty &&
                      !answers.get(ImportSubCategoryPage)
                        .exists(allowedValues(children).contains)
                  )(
                    routes.ImportSubCategoryController.onPageLoad(CheckMode)
                  )
                }

              case _ =>
                Some(
                  routes.ImportSubCodeController
                    .onPageLoad(importType.toString, CheckMode)
                )
            }

          case None if importType == Other =>
            missingDescription(answers)

          case None =>
            None
        }
    }

  private def missingDescription(answers: UserAnswers): Option[Call] =
    Option.when(!nonBlank(answers.get(DescribeItemsOnImportDocPage)))(
      routes.DescribeItemsOnImportDocController.onPageLoad(CheckMode)
    )

  private def documentMissingPage(answers: UserAnswers): Option[Call] = {
    val documentAnswer = answers.get(SadReferenceCheckPage) match {
      case None =>
        Some(routes.SadReferenceCheckController.onPageLoad(CheckMode))

      case Some(true) =>
        Option.when(!nonBlank(answers.get(SadReferenceNumberPage)))(
          routes.SadReferenceNumberController.onPageLoad(CheckMode)
        )

      case Some(false) =>
        Option.when(!nonBlank(answers.get(ImportDetailsInfoPage)))(
          routes.ImportDetailsInfoController.onPageLoad(CheckMode)
        )
    }

    documentAnswer.orElse(
      Option.when(answers.get(ImportDatePage).isEmpty)(
        routes.ImportDateController.onPageLoad(CheckMode)
      )
    )
  }

  private def supplierMissingPage(answers: UserAnswers): Option[Call] = {
    val completeAddress = answers.get(ImportSupplierAddressPage).exists {
      address =>
        address.addressLine1.trim.nonEmpty &&
          address.country.trim.nonEmpty
    }

    Option.when(!nonBlank(answers.get(ImportSuppliersNamePage)))(
      routes.ImportSuppliersNameController.onPageLoad(CheckMode)
    ).orElse(
      Option.when(!completeAddress)(
        routes.ImportSupplierAddressController.onPageLoad(CheckMode)
      )
    )
  }

  private def amountMissingPage(
                                 answers: UserAnswers,
                                 country: String
                               ): Option[Call] = {
    val currencies = currencyConfig.currencyConfig
      .getOrElse(country, currencyConfig.default)

    val missingCurrency =
      currencyConfig.requiresCurrencySelection(country) &&
        !answers.get(ImportCurrencyPage)
          .exists(code => currencies.exists(_.code == code))

    Option.when(missingCurrency)(
      routes.ImportCurrencyController.onPageLoad(CheckMode)
    ).orElse(
      Option.when(answers.get(TotalAmountWithoutVatPage).isEmpty)(
        routes.TotalAmountWithoutVatController.onPageLoad(CheckMode)
      )
    ).orElse(
      Option.when(answers.get(TotalVatPaidImportPage).isEmpty)(
        routes.TotalVatPaidImportController.onPageLoad(CheckMode)
      )
    ).orElse(
      Option.when(answers.get(ImportTotalVatClaimPage).isEmpty)(
        routes.TotalVatClaimController.onPageLoad(CheckMode)
      )
    )
  }

  def navigateFromCheckYourImportDetails(answers: UserAnswers): Call =
    firstMissingPage(answers).getOrElse(
      // Temporary destination: claim save and summary are not implemented.
      controllers.routes.JourneyRecoveryController.onPageLoad()
    )
}
