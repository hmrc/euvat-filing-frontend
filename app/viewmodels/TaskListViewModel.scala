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

package viewmodels

import models.UserAnswers
import pages.{ClaimDetailsCompletedPage, PurchaseImportSummaryPage}
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.tag.Tag
import uk.gov.hmrc.govukfrontend.views.viewmodels.tasklist.{TaskList, TaskListItem, TaskListItemStatus, TaskListItemTitle}

import javax.inject.Inject

class TaskListViewModel @Inject() () {

  def showDeleteLink(answers: UserAnswers): Boolean = answers.get(ClaimDetailsCompletedPage).contains(true)

  def buildTaskList(answers: UserAnswers)(implicit messages: Messages): TaskList = {
    val claimDetailsDone = answers.get(ClaimDetailsCompletedPage).contains(true)
    def notStartedStatus = TaskListItemStatus(tag = Some(Tag(content = Text(messages("taskListDashboard.status1")))))
    def cannotStartStatus =
      TaskListItemStatus(content = Text(messages("taskListDashboard.status2")), classes = "govuk-task-list__status--cannot-start-yet")
    def completedStatus = TaskListItemStatus(content = Text(messages("taskListDashboard.status3")))

    val claimDetailsItem = claimTaskList(messages, claimDetailsDone, notStartedStatus, completedStatus)
    val purchaseImportItems = purchaseImportTaskList(answers, messages, claimDetailsDone, cannotStartStatus, notStartedStatus, completedStatus)
    val supportingDocsItem = supportingDocItemList(messages, claimDetailsDone, notStartedStatus, cannotStartStatus)
    val bankDetailsItem = bankDetailsTaskList(messages, claimDetailsDone, notStartedStatus, cannotStartStatus)
    val submitClaimItem = submitClaimTaskList(messages, cannotStartStatus)

    TaskList(
      items    = Seq(claimDetailsItem, purchaseImportItems, supportingDocsItem, bankDetailsItem, submitClaimItem),
      idPrefix = "make-a-claim-eu-vat-refund"
    )
  }

  private def submitClaimTaskList(messages: Messages, cannotStartStatus: TaskListItemStatus): TaskListItem = {
    TaskListItem(
      title  = TaskListItemTitle(content = Text(messages("taskListDashboard.listItem5"))),
      status = cannotStartStatus
    )
  }

  private def bankDetailsTaskList(messages: Messages,
                                  claimDetailsDone: Boolean,
                                  notStartedStatus: TaskListItemStatus,
                                  cannotStartStatus: TaskListItemStatus
                                 ): TaskListItem = {
    TaskListItem(
      title  = TaskListItemTitle(content = Text(messages("taskListDashboard.listItem4"))),
      status = if (claimDetailsDone) notStartedStatus else cannotStartStatus,
      href   = if (claimDetailsDone) Some("#") else None
    )
  }

  private def supportingDocItemList(messages: Messages,
                                    claimDetailsDone: Boolean,
                                    notStartedStatus: TaskListItemStatus,
                                    cannotStartStatus: TaskListItemStatus
                                   ): TaskListItem = {
    TaskListItem(
      title  = TaskListItemTitle(content = Text(messages("taskListDashboard.listItem3"))),
      status = if (claimDetailsDone) notStartedStatus else cannotStartStatus,
      href   = if (claimDetailsDone) Some("#") else None
    )
  }

  private def purchaseImportTaskList(answers: UserAnswers,
                                     messages: Messages,
                                     claimDetailsDone: Boolean,
                                     cannotStartStatus: TaskListItemStatus,
                                     notStartedStatus: TaskListItemStatus,
                                     completedStatus: TaskListItemStatus
                                    ): TaskListItem = {
    TaskListItem(
      title = TaskListItemTitle(content = Text(messages("taskListDashboard.listItem2"))),
      status = if (claimDetailsDone) {
        answers.get(PurchaseImportSummaryPage) match {
          case Some(false) => completedStatus
          case _           => notStartedStatus
        }
      } else { cannotStartStatus },
      href = if (claimDetailsDone) {
        answers.get(PurchaseImportSummaryPage) match {
          case Some(false) => Some(controllers.routes.PurchaseImportSummaryController.onPageLoad.url)
          case _           => Some(controllers.routes.BeforeYouStartController.onPageLoad().url)
        }
      } else { None }
    )
  }

  private def claimTaskList(messages: Messages,
                            claimDetailsDone: Boolean,
                            notStartedStatus: TaskListItemStatus,
                            completedStatus: TaskListItemStatus
                           ): TaskListItem = {
    TaskListItem(
      title  = TaskListItemTitle(content = Text(messages("taskListDashboard.listItem1"))),
      status = if (claimDetailsDone) completedStatus else notStartedStatus,
      href = Some(
        if (claimDetailsDone) { "/file-eu-vat/claim-details" }
        else { controllers.claim.routes.RefundingCountryController.onPageLoad(models.NormalMode).url }
      )
    )
  }
}
