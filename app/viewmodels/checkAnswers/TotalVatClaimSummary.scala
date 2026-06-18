package viewmodels.checkAnswers

import config.CurrencyFormatter.currencyFormat
import controllers.routes
import models.{CheckMode, UserAnswers}
import pages.TotalVatClaimPage
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist._
import viewmodels.implicits._

object TotalVatClaimSummary  {

  def row(answers: UserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(TotalVatClaimPage).map {
      answer =>

        SummaryListRowViewModel(
          key     = "totalVatClaim.checkYourAnswersLabel",
          value   = ValueViewModel(currencyFormat(answer)),
          actions = Seq(
            ActionItemViewModel("site.change", routes.TotalVatClaimController.onPageLoad(CheckMode).url)
              .withVisuallyHiddenText(messages("totalVatClaim.change.hidden"))
          )
        )
    }
}
