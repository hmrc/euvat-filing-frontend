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

package viewmodels.checkAnswers

import controllers.imports.routes
import models.{CheckMode, Other, PurchaseOrImportSubCategoryType, PurchaseOrImportType, UserAnswers}
import pages.imports.*
import play.api.i18n.{Lang, Messages}
import utils.{ConfigPurchaseOrImportMapping, CountryCode, Currency, CurrencyConfig, DateTimeFormats}
import utils.PurchaseOrImportHelpers.isNoneSelection

object CheckYourImportDetailsSummary {

  type Row = (String, Option[String], Seq[(String, String, String)])
  type Section = (String, Seq[Row])

  private val prefix = "checkYourImportDetails"

  private def subCategoryUrl(slug: String): String = {
    val typeUrl = routes.ImportTypeController.onPageLoad(CheckMode).url
    typeUrl.take(typeUrl.lastIndexOf('/')) + s"/change-$slug"
  }

  private def text(value: Option[String]): Option[String] =
    value.filter(_.trim.nonEmpty)

  private def row(
    labelKey: String,
    value: Option[String],
    url: String
  )(implicit messages: Messages): Row = {
    val action = if (value.isDefined) "change" else "add"

    (
      messages(s"$prefix.$labelKey.label"),
      Some(value.getOrElse(messages("site.notProvided"))),
      Seq((url, if (action == "add") "site.add" else "site.change", s"$prefix.$labelKey.$action.hidden"))
    )
  }

  private def selection(
    value: Option[String],
    options: Seq[(String, String)],
    config: ConfigPurchaseOrImportMapping
  )(implicit messages: Messages): Option[String] =
    value.flatMap {
      case ConfigPurchaseOrImportMapping.NoneValue =>
        Some(messages("site.none"))

      case code =>
        config
          .buildRadioItems(options, messages)
          .find(_.value.contains(code))
          .map(item =>
            item.content match {
              case uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text(value) => value
              case content                                                        => content.asHtml.toString
            }
          )
    }

  private def dynamicRow(
    label: String,
    value: Option[String],
    url: String
  )(implicit messages: Messages): Row = {
    val action = if (value.isDefined) "change" else "add"

    (
      label,
      Some(value.getOrElse(messages("site.notProvided"))),
      Seq(
        (
          url,
          if (action == "add") "site.add" else "site.change",
          messages(s"$prefix.category.$action.hidden", label)
        )
      )
    )
  }

  def categoryRows(
    answers: UserAnswers,
    config: ConfigPurchaseOrImportMapping
  )(implicit messages: Messages): Seq[Row] = {
    val importType = answers.get(ImportTypePage)
    val country = CountryCode.findCountryCode(answers)

    val typeRow = row(
      "importType",
      importType.map(value => messages(s"importType.$value")),
      routes.ImportTypeController.onPageLoad(CheckMode).url
    )

    val subCodeOptions = (
      for {
        selectedType <- importType
        countryCode  <- country
        options <- config.selectableSubcodes(
                     countryCode,
                     selectedType.toString
                   )
      } yield options
    ).getOrElse(Seq.empty)

    val selectedSubCode = answers.get(ImportSubCodePage)

    val descriptionRequired =
      importType.contains(Other) &&
        (
          subCodeOptions.isEmpty ||
            selectedSubCode.exists(isNoneSelection)
        )

    if (descriptionRequired) {
      Seq(
        typeRow,
        row(
          "importDescription",
          text(answers.get(DescribeItemsOnImportDocPage)),
          routes.DescribeItemsOnImportDocController
            .onPageLoad(CheckMode)
            .url
        )
      )
    } else {
      val subCodeRows = importType.toSeq.flatMap { selectedType =>
        if (subCodeOptions.isEmpty) {
          Seq.empty
        } else {
          val slug =
            PurchaseOrImportType.urlSlugForPurchaseType(selectedType)

          val label =
            if (selectedType == Other) {
              messages(s"$prefix.otherImportType.label")
            } else {
              messages(s"purchase.subType.$slug")
            }

          Seq(
            dynamicRow(
              label,
              selection(selectedSubCode, subCodeOptions, config),
              routes.ImportSubCodeController
                .onPageLoad(selectedType.toString, CheckMode)
                .url
            )
          )
        }
      }

      val subCategoryRows = (
        for {
          selectedType <- importType
          countryCode  <- country
          subCode      <- selectedSubCode
          if subCodeOptions.exists(_._1 == subCode)
          options = config.subcategoriesFor(
                      countryCode,
                      selectedType.toString,
                      subCode
                    )
          if options.nonEmpty
          slug <- PurchaseOrImportSubCategoryType
                    .purchaseOrImportSubCategoryUrlSlugFor(
                      selectedType.toString,
                      subCode
                    )
        } yield Seq(
          dynamicRow(
            messages(s"purchase.subCategory.$slug"),
            selection(
              answers.get(ImportSubCategoryPage),
              options,
              config
            ),
            subCategoryUrl(slug)
          )
        )
      ).getOrElse(Seq.empty)

      Seq(typeRow) ++ subCodeRows ++ subCategoryRows
    }
  }

  def documentRows(answers: UserAnswers)(implicit
    messages: Messages
  ): Seq[Row] = {
    val sadAnswer = answers.get(SadReferenceCheckPage)

    val sadRow = row(
      "sad",
      sadAnswer.map(value => messages(if (value) "site.yes" else "site.no")),
      routes.SadReferenceCheckController.onPageLoad(CheckMode).url
    )

    val referenceRows = sadAnswer.toSeq.map {
      case true =>
        row(
          "sadReference",
          text(answers.get(SadReferenceNumberPage)),
          routes.SadReferenceNumberController.onPageLoad(CheckMode).url
        )

      case false =>
        row(
          "documentInformation",
          text(answers.get(ImportDetailsInfoPage)),
          routes.ImportDetailsInfoController.onPageLoad(CheckMode).url
        )
    }

    Seq(sadRow) ++ referenceRows ++ Seq(rowImportDate(answers))
  }

  def rowImportDate(answers: UserAnswers)(implicit
    messages: Messages
  ): Row = {
    implicit val lang: Lang = messages.lang

    row(
      "importDate",
      answers
        .get(ImportDatePage)
        .map(_.format(DateTimeFormats.dateTimeFormat())),
      routes.ImportDateController.onPageLoad(CheckMode).url
    )
  }

  def rowSupplierName(answers: UserAnswers)(implicit
    messages: Messages
  ): Row =
    row(
      "supplierName",
      text(answers.get(ImportSuppliersNamePage)),
      routes.ImportSuppliersNameController.onPageLoad(CheckMode).url
    )

  def rowSupplierAddress(
    answers: UserAnswers,
    supplierCountries: Map[String, String]
  )(implicit messages: Messages): Row = {
    val value = answers
      .get(ImportSupplierAddressPage)
      .filter(address =>
        address.addressLine1.trim.nonEmpty &&
          supplierCountries.contains(address.country)
      )
      .map { address =>
        Seq(
          Some(address.addressLine1),
          address.addressLine2,
          address.addressLine3,
          Some(supplierCountries(address.country))
        ).flatten
          .filter(_.trim.nonEmpty)
          .mkString("\n")
      }

    row(
      "supplierAddress",
      value,
      routes.ImportSupplierAddressController.onPageLoad(CheckMode).url
    )
  }

  private def amount(
    value: Option[BigDecimal],
    currency: Option[Currency]
  ): Option[String] =
    value.map { number =>
      val formatted = number.bigDecimal
        .setScale(2, java.math.RoundingMode.HALF_UP)
        .toPlainString

      currency.map(_.symbol).getOrElse("") + formatted
    }

  def amountRows(
    answers: UserAnswers,
    currencyConfig: CurrencyConfig
  )(implicit messages: Messages): Seq[Row] = {
    val currencies = CountryCode
      .findCountryCode(answers)
      .flatMap(currencyConfig.currencyConfig.get)
      .getOrElse(currencyConfig.default)

    val showCurrency = currencies.size > 1

    val selectedCurrency =
      if (showCurrency) {
        answers
          .get(ImportCurrencyPage)
          .flatMap(code => currencies.find(_.code == code))
      } else {
        currencies.headOption
      }

    val currencyRows =
      if (showCurrency) {
        Seq(
          row(
            "currency",
            selectedCurrency.map(currency =>
              messages(
                s"refundingCurrency.${currency.name}",
                currency.symbol
              )
            ),
            routes.ImportCurrencyController.onPageLoad(CheckMode).url
          )
        )
      } else {
        Seq.empty
      }

    currencyRows ++ Seq(
      row(
        "amountWithoutVat",
        amount(answers.get(TotalAmountWithoutVatPage), selectedCurrency),
        routes.TotalAmountWithoutVatController.onPageLoad(CheckMode).url
      ),
      row(
        "vatPaid",
        amount(answers.get(TotalVatPaidImportPage), selectedCurrency),
        routes.TotalVatPaidImportController.onPageLoad(CheckMode).url
      ),
      row(
        "vatClaim",
        amount(answers.get(ImportTotalVatClaimPage), selectedCurrency),
        routes.TotalVatClaimController.onPageLoad(CheckMode).url
      )
    )
  }

  def sections(
    answers: UserAnswers,
    config: ConfigPurchaseOrImportMapping,
    currencyConfig: CurrencyConfig,
    supplierCountries: Map[String, String]
  )(implicit messages: Messages): Seq[Section] =
    Seq(
      s"$prefix.importCategory" ->
        categoryRows(answers, config),
      s"$prefix.importDocumentDetails" ->
        documentRows(answers),
      s"$prefix.supplierDetails" ->
        Seq(
          rowSupplierName(answers),
          rowSupplierAddress(answers, supplierCountries)
        ),
      s"$prefix.importAmounts" ->
        amountRows(answers, currencyConfig)
    )
}
