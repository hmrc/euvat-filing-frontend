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
import models.{CheckMode, Mode, NormalMode, Other, UserAnswers}
import pages.*
import pages.imports.*
import play.api.mvc.Call
import utils.PurchaseOrImportHelpers.isNoneSelection
import utils.{ConfigPurchaseOrImportMapping, CountryCode, CurrencyConfig}

import javax.inject.{Inject, Singleton}

@Singleton
class ImportNavigator @Inject() (currencyConfig: CurrencyConfig, configPurchaseOrImportMapping: ConfigPurchaseOrImportMapping) {

  private lazy val completionNavigator =
    new ImportCompletionNavigator(
      currencyConfig,
      configPurchaseOrImportMapping
    )

  private val importAnswerPages: Set[Page] = Set(
    ImportTypePage,
    ImportSubCodePage,
    ImportSubCategoryPage,
    DescribeItemsOnImportDocPage,
    SadReferenceCheckPage,
    SadReferenceNumberPage,
    ImportDetailsInfoPage,
    ImportDatePage,
    ImportSuppliersNamePage,
    ImportSupplierAddressPage,
    ImportCurrencyPage,
    TotalAmountWithoutVatPage,
    TotalVatPaidImportPage,
    ImportTotalVatClaimPage
  )

  def continueMissingAnswers(
                              page: Page,
                              mode: Mode,
                              answers: UserAnswers,
                              ordinaryDestination: Call
                            ): Call = {
    val recovering =
      mode == CheckMode &&
        importAnswerPages.contains(page) &&
        answers.get(ImportMissingAnswersJourneyPage).contains(true)

    val warningDestination =
      ordinaryDestination ==
        importsRoutes.ImportAmountWarningController.onVatPaidPageLoad(mode) ||
        ordinaryDestination ==
          importsRoutes.ImportAmountWarningController.onVatClaimPageLoad(mode)

    if (recovering && !warningDestination) {
      completionNavigator.firstMissingPage(answers).getOrElse(
        importsRoutes.CheckYourImportDetailsController.onPageLoad()
      )
    } else {
      ordinaryDestination
    }
  }

  def navigateFromImportTypePage(mode: Mode)(userAnswers: UserAnswers): Call =
    (
      userAnswers.get(ImportTypePage),
      CountryCode.findCountryCode(userAnswers)
    ) match {
      case (Some(importType), Some(country)) =>
        mode match {
          case CheckMode
            if !userAnswers.get(ImportTypeChangedPage).contains(true) =>
            importsRoutes.CheckYourImportDetailsController.onPageLoad()

          case _ =>
            if (
              configPurchaseOrImportMapping
                .selectableSubcodes(country, importType.toString)
                .isDefined
            ) {
              importsRoutes.ImportSubCodeController
                .onPageLoad(importType.toString, mode)
            } else if (importType == Other) {
              importsRoutes.DescribeItemsOnImportDocController.onPageLoad(mode)
            } else {
              mode match {
                case CheckMode =>
                  importsRoutes.CheckYourImportDetailsController.onPageLoad()

                case NormalMode =>
                  importsRoutes.SadReferenceCheckController.onPageLoad(NormalMode)
              }
            }
        }

      case _ =>
        controllers.routes.JourneyRecoveryController.onPageLoad()
    }

  def navigateFromImportSubCodePage(mode: Mode)(
    userAnswers: UserAnswers
  ): Call =
    (
      userAnswers.get(ImportTypePage),
      userAnswers.get(ImportSubCodePage),
      CountryCode.findCountryCode(userAnswers)
    ) match {
      case (Some(importType), Some(subCode), Some(country)) =>
        mode match {
          case CheckMode
            if !userAnswers.get(ImportSubCodeChangedPage).contains(true) =>
            importsRoutes.CheckYourImportDetailsController.onPageLoad()

          case _ =>
            if (importType == Other && isNoneSelection(subCode)) {
              importsRoutes.DescribeItemsOnImportDocController.onPageLoad(mode)
            } else if (
              configPurchaseOrImportMapping
                .subcategoriesFor(country, importType.toString, subCode)
                .nonEmpty
            ) {
              importsRoutes.ImportSubCategoryController.onPageLoad(mode)
            } else {
              mode match {
                case CheckMode =>
                  importsRoutes.CheckYourImportDetailsController.onPageLoad()

                case NormalMode =>
                  importsRoutes.SadReferenceCheckController.onPageLoad(NormalMode)
              }
            }
        }

      case _ =>
        controllers.routes.JourneyRecoveryController.onPageLoad()
    }

  def navigateFromImportSubCategoryPage(mode: Mode)(
    userAnswers: UserAnswers
  ): Call =
    userAnswers.get(ImportSubCategoryPage) match {
      case Some(_) =>
        mode match {
          case CheckMode =>
            importsRoutes.CheckYourImportDetailsController.onPageLoad()

          case NormalMode =>
            importsRoutes.SadReferenceCheckController.onPageLoad(NormalMode)
        }

      case None =>
        controllers.routes.JourneyRecoveryController.onPageLoad()
    }

  def navigateFromSadReferenceCheckPage(mode: Mode)(
    userAnswers: UserAnswers
  ): Call =
    userAnswers.get(SadReferenceCheckPage) match {
      case None =>
        controllers.routes.JourneyRecoveryController.onPageLoad()

      case Some(answer) =>
        mode match {
          case CheckMode
            if !userAnswers.get(ImportSadChangedPage).contains(true) =>
            importsRoutes.CheckYourImportDetailsController.onPageLoad()

          case _ =>
            if (answer) {
              importsRoutes.SadReferenceNumberController.onPageLoad(mode)
            } else {
              importsRoutes.ImportDetailsInfoController.onPageLoad(mode)
            }
        }
    }

  def navigateFromSadReferenceNumberPage(mode: Mode)(
    userAnswers: UserAnswers
  ): Call =
    mode match {
      case CheckMode =>
        importsRoutes.CheckYourImportDetailsController.onPageLoad()

      case NormalMode =>
        userAnswers.get(SadReferenceCheckPage) match {
          case Some(true) =>
            importsRoutes.ImportDateController.onPageLoad(NormalMode)

          case _ =>
            importsRoutes.ImportDetailsInfoController.onPageLoad(NormalMode)
        }
    }

  def navigateFromImportDetailsInfoPage(mode: Mode)(
    userAnswers: UserAnswers
  ): Call =
    mode match {
      case CheckMode =>
        importsRoutes.CheckYourImportDetailsController.onPageLoad()

      case NormalMode =>
        importsRoutes.ImportDateController.onPageLoad(NormalMode)
    }

  def navigateToCurrencyOrNextPage(mode: Mode)(userAnswers: UserAnswers): Call =
    CountryCode.findCountryCode(userAnswers) match {
      case Some(countryCode) if currencyConfig.requiresCurrencySelection(countryCode) =>
        importsRoutes.ImportCurrencyController.onPageLoad(mode)
      case Some(_) => importsRoutes.TotalAmountWithoutVatController.onPageLoad(mode) // TODO: next import page
      case None    => controllers.routes.JourneyRecoveryController.onPageLoad()
    }

  def navigateFromTotalAmountWithoutVatPage(mode: Mode)(
    userAnswers: UserAnswers
  ): Call =
    userAnswers.get(TotalAmountWithoutVatPage) match {
      case None =>
        controllers.routes.JourneyRecoveryController.onPageLoad()

      case Some(_) =>
        mode match {
          case CheckMode
            if !userAnswers
              .get(ImportAmountWithoutVatChangedPage)
              .contains(true) =>
            importsRoutes.CheckYourImportDetailsController.onPageLoad()

          case _ =>
            importsRoutes.TotalVatPaidImportController.onPageLoad(mode)
        }
    }

  def navigateFromTotalVatPaidImportPage(mode: Mode)(
    userAnswers: UserAnswers
  ): Call =
    userAnswers.get(TotalVatPaidImportPage) match {
      case None =>
        controllers.routes.JourneyRecoveryController.onPageLoad()

      case Some(paid) =>
        mode match {
          case CheckMode
            if !userAnswers.get(ImportVatPaidChangedPage).contains(true) =>
            importsRoutes.CheckYourImportDetailsController.onPageLoad()

          case _ =>
            userAnswers.get(TotalAmountWithoutVatPage) match {
              case None =>
                importsRoutes.TotalAmountWithoutVatController.onPageLoad(mode)

              case Some(amount) if paid >= amount =>
                importsRoutes.ImportAmountWarningController
                  .onVatPaidPageLoad(mode)

              case Some(_) =>
                importsRoutes.TotalVatClaimController.onPageLoad(mode)
            }
        }
    }

  def navigateFromImportTotalVatClaimPage(mode: Mode)(
    userAnswers: UserAnswers
  ): Call =
    userAnswers.get(ImportTotalVatClaimPage) match {
      case None =>
        controllers.routes.JourneyRecoveryController.onPageLoad()

      case Some(claim) =>
        mode match {
          case CheckMode
            if !userAnswers.get(ImportVatClaimChangedPage).contains(true) =>
            importsRoutes.CheckYourImportDetailsController.onPageLoad()

          case _ =>
            userAnswers.get(TotalVatPaidImportPage) match {
              case None =>
                importsRoutes.TotalVatPaidImportController.onPageLoad(mode)

              case Some(paid) if claim > paid =>
                importsRoutes.ImportAmountWarningController
                  .onVatClaimPageLoad(mode)

              case Some(_) =>
                importsRoutes.CheckYourImportDetailsController.onPageLoad()
            }
        }
    }

  def vatPaidWarningPage(mode: Mode)(
    userAnswers: UserAnswers
  ): Either[Call, BigDecimal] =
    (
      userAnswers.get(TotalVatPaidImportPage),
      userAnswers.get(TotalAmountWithoutVatPage)
    ) match {
      case (Some(paid), Some(amount)) if paid >= amount =>
        Right(paid)

      case _ =>
        Left(
          importsRoutes.TotalVatPaidImportController.onPageLoad(mode)
        )
    }

  def vatClaimWarningPage(mode: Mode)(
    userAnswers: UserAnswers
  ): Either[Call, BigDecimal] =
    (
      userAnswers.get(ImportTotalVatClaimPage),
      userAnswers.get(TotalVatPaidImportPage)
    ) match {
      case (Some(claim), Some(paid)) if claim > paid =>
        Right(claim)

      case _ =>
        Left(
          importsRoutes.TotalVatClaimController.onPageLoad(mode)
        )
    }

  def navigateFromVatPaidWarning(mode: Mode)(
    userAnswers: UserAnswers
  ): Call =
    vatPaidWarningPage(mode)(userAnswers).fold(
      identity,
      _ => importsRoutes.TotalVatClaimController.onPageLoad(mode)
    )

  def navigateFromVatClaimWarning(mode: Mode)(
    userAnswers: UserAnswers
  ): Call =
    vatClaimWarningPage(mode)(userAnswers).fold(
      identity,
      _ => importsRoutes.CheckYourImportDetailsController.onPageLoad()
    )

  def backLinkFromImportDatePage(mode: Mode)(userAnswers: UserAnswers): Call =
    mode match {
      case CheckMode =>
        importsRoutes.CheckYourImportDetailsController.onPageLoad()

      case NormalMode =>
        userAnswers.get(SadReferenceCheckPage) match {
          case Some(true) =>
            importsRoutes.SadReferenceNumberController.onPageLoad(NormalMode)

          case Some(false) =>
            importsRoutes.ImportDetailsInfoController.onPageLoad(NormalMode)

          case None =>
            controllers.routes.JourneyRecoveryController.onPageLoad()
        }
    }
}
