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

import models.{Currency, UserAnswers}
import pages.bankdetails.{AccountHolderPage, BankDetailsPage}
import play.api.i18n.Messages

object CheckYourBankDetailsSummary {

  type Row = (String, Option[String], Seq[(String, String, String)])

  def rows(answers: UserAnswers, currencies: Seq[Currency])(implicit messages: Messages): Seq[Row] =
    Seq(
      rowAccountHolder(answers),
      rowAccountName(answers),
      rowBic(answers),
      rowIban(answers),
      rowCurrency(answers, currencies)
    ).flatten

  def rowAccountHolder(answers: UserAnswers)(implicit messages: Messages): Option[Row] =
    answers.get(AccountHolderPage).map { answer =>
      (
        messages("bankDetails.checkYourBankDetails.accountHolder.label"),
        Some(messages(s"bankDetails.checkYourBankDetails.accountHolder.${answer.toString}")),
        Seq.empty
      )
    }

  def rowAccountName(answers: UserAnswers)(implicit messages: Messages): Option[Row] =
    answers.get(BankDetailsPage).map { details =>
      (messages("bankDetails.checkYourBankDetails.accountName.label"), Some(details.NameOnTheAccount), Seq.empty)
    }

  def rowBic(answers: UserAnswers)(implicit messages: Messages): Option[Row] =
    answers.get(BankDetailsPage).map { details =>
      (messages("bankDetails.checkYourBankDetails.bic.label"), Some(details.BIC), Seq.empty)
    }

  def rowIban(answers: UserAnswers)(implicit messages: Messages): Option[Row] =
    answers.get(BankDetailsPage).map { details =>
      (messages("bankDetails.checkYourBankDetails.iban.label"), Some(details.accountNumber), Seq.empty)
    }

  def rowCurrency(answers: UserAnswers, currencies: Seq[Currency])(implicit messages: Messages): Option[Row] =
    answers.get(BankDetailsPage).map { details =>
      val label = currencies
        .find(_.code == details.CurrencyOfAccount)
        .map(c => formatCurrency(c.description))
        .getOrElse(details.CurrencyOfAccount)
      (messages("bankDetails.checkYourBankDetails.currency.label"), Some(label), Seq.empty)
    }

  private def formatCurrency(description: String): String =
    description.split(",", 2).map(_.trim) match {
      case Array(country, currency) => s"$country (${currency.toLowerCase})"
      case _                        => description
    }
}
