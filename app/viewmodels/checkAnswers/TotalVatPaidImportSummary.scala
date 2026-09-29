package viewmodels.checkAnswers

import controllers.routes
import models.{CheckMode, UserAnswers}
import pages.TotalVatPaidImportPage
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist._
import viewmodels.implicits._

object TotalVatPaidImportSummary  {

  def row(answers: UserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(TotalVatPaidImportPage).map {
      answer =>

        SummaryListRowViewModel(
          key     = "totalVatPaidImport.checkYourAnswersLabel",
          value   = ValueViewModel(answer.toString),
          actions = Seq(
            ActionItemViewModel("site.change", routes.TotalVatPaidImportController.onPageLoad(CheckMode).url)
              .withVisuallyHiddenText(messages("totalVatPaidImport.change.hidden"))
          )
        )
    }
}
