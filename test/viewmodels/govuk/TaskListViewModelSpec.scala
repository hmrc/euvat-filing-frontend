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

package viewmodels.govuk

import base.SpecBase
import models.Attachment
import pages.PurchaseImportSummaryPage
import pages.supportingdocuments.AnyDocumentsPage
import play.api.i18n.Messages
import play.api.test.Helpers.stubMessages
import viewmodels.TaskListViewModel
import queries.{BankDetailsCompletedQuery, ClaimDetailsCompletedQuery, PurchaseImportSummaryIncompleteQuery, SupportingDocumentsCompletedQuery, UploadedFilesQuery}

class TaskListViewModelSpec extends SpecBase {
  implicit val messages: Messages = stubMessages()
  private val viewModel = new TaskListViewModel()

  "TaskListViewModel" - {

    "when claim details are not completed" - {
      val answers = emptyUserAnswers

      "must return 5 items" in {
        val taskList = viewModel.buildTaskList(answers)
        taskList.items.size mustEqual 5
      }

      "must show claim details as Not yet started with a link" in {
        val taskList = viewModel.buildTaskList(answers)
        val item = taskList.items.head
        item.href mustBe defined
        item.status.tag.map(_.content.asHtml.body) must contain("taskListDashboard.status1")
      }

      "must show purchase and import as Cannot start yet with no link" in {
        val taskList = viewModel.buildTaskList(answers)
        val item = taskList.items(1)
        item.href mustBe None
        item.status.content.asHtml.body mustBe "taskListDashboard.status2"
      }

      "must show supporting documents as Cannot start yet with no link" in {
        val taskList = viewModel.buildTaskList(answers)
        val item = taskList.items(2)
        item.href mustBe None
        item.status.content.asHtml.body mustBe "taskListDashboard.status2"
      }

      "must show bank details as Cannot start yet with no link" in {
        val taskList = viewModel.buildTaskList(answers)
        val item = taskList.items(3)
        item.href mustBe None
        item.status.content.asHtml.body mustBe "taskListDashboard.status2"
      }

      "must show Submit claim as Cannot start yet with no link" in {
        val taskList = viewModel.buildTaskList(answers)
        val item = taskList.items(4)
        item.href mustBe None
        item.status.content.asHtml.body mustBe "taskListDashboard.status2"
      }
    }

    "when claim details are completed" - {
      val answers = emptyUserAnswers.set(ClaimDetailsCompletedQuery, true).success.value

      "must return 5 items" in {
        val taskList = viewModel.buildTaskList(answers)
        taskList.items.size mustEqual 5
      }

      "must show claim details as Completed with a link" in {
        val taskList = viewModel.buildTaskList(answers)
        val item = taskList.items.head
        item.href mustBe defined
        item.status.content.asHtml.body must include("taskListDashboard.status3")
      }

      "must show purchase and import as Not yet started with a link" in {
        val taskList = viewModel.buildTaskList(answers)
        val item = taskList.items(1)
        item.href mustBe defined
        item.status.tag.map(_.content.asHtml.body) must contain("taskListDashboard.status1")
      }

      "must show purchase and import as incomplete with a link" in {
        val userAnswers = answers
          .set(PurchaseImportSummaryIncompleteQuery, true)
          .success
          .value
          .set(PurchaseImportSummaryPage, false)
          .success
          .value
        val taskList = viewModel.buildTaskList(userAnswers)
        val item = taskList.items(1)
        item.href mustBe defined
        item.status.tag.map(_.content.asHtml.body) must contain("taskListDashboard.status4")
      }

      "must show purchase and import as completed with a link" in {
        val userAnswers = answers.set(PurchaseImportSummaryPage, false).success.value
        val taskList = viewModel.buildTaskList(userAnswers)
        val item = taskList.items(1)
        item.href mustBe defined
        item.status.content.asHtml.body mustBe "taskListDashboard.status3"
      }

      "must show supporting documents as Not yet started with a link" in {
        val taskList = viewModel.buildTaskList(answers)
        val item = taskList.items(2)
        item.href mustBe defined
        item.status.tag.map(_.content.asHtml.body) must contain("taskListDashboard.status1")
      }

      "must show supporting documents as completed when the user has none to upload" in {
        val userAnswers = answers.set(AnyDocumentsPage, false).success.value
        val taskList = viewModel.buildTaskList(userAnswers)
        val item = taskList.items(2)
        item.href mustBe defined
        item.status.content.asHtml.body must include("taskListDashboard.status3")
      }

      "must link supporting documents to the start page" in {
        val taskList = viewModel.buildTaskList(answers)
        taskList.items(2).href mustBe Some(controllers.supportingdocuments.routes.BeforeYouStartSupportingDocsController.onPageLoad().url)
      }

      "must show supporting documents as completed when the uploaded files page is continued" in {
        val userAnswers = answers.set(SupportingDocumentsCompletedQuery, true).success.value
        val item = viewModel.buildTaskList(userAnswers).items(2)
        item.status.content.asHtml.body must include("taskListDashboard.status3")
      }

      "must link supporting documents to the uploaded files page when files exist" in {
        val userAnswers = answers
          .set(UploadedFilesQuery, List(Attachment(1, Some("a.pdf"), Some("pdf"), None, Some(1825))))
          .success
          .value
        viewModel.buildTaskList(userAnswers).items(2).href mustBe
          Some(controllers.supportingdocuments.routes.UploadedFilesController.onPageLoad().url)
      }

      "must link supporting documents to the start page when the uploaded files list is empty" in {
        val userAnswers = answers.set(UploadedFilesQuery, List.empty[Attachment]).success.value
        viewModel.buildTaskList(userAnswers).items(2).href mustBe
          Some(controllers.supportingdocuments.routes.BeforeYouStartSupportingDocsController.onPageLoad().url)
      }

      "must show bank details as Not yet started with a link" in {
        val taskList = viewModel.buildTaskList(answers)
        val item = taskList.items(3)
        item.href mustBe defined
        item.status.tag.map(_.content.asHtml.body) must contain("taskListDashboard.status1")
      }

      "must show Submit claim as Cannot start yet with no link" in {
        val taskList = viewModel.buildTaskList(answers)
        val item = taskList.items(4)
        item.href mustBe None
        item.status.content.asHtml.body mustBe "taskListDashboard.status2"
      }
    }

    "when bank details are completed" - {
      val claimDetailsCompleted = emptyUserAnswers.set(ClaimDetailsCompletedQuery, true).success.value
      val bankDetailsCompleted = claimDetailsCompleted.set(BankDetailsCompletedQuery, true).success.value

      "must return 5 items" in {
        val taskList = viewModel.buildTaskList(bankDetailsCompleted)
        taskList.items.size mustEqual 5
      }

      "must show claim details as Completed with a link" in {
        val taskList = viewModel.buildTaskList(bankDetailsCompleted)
        val item = taskList.items.head
        item.href mustBe defined
        item.status.content.asHtml.body must include("taskListDashboard.status3")
      }

      "must show purchase and import as Not yet started with a link" in {
        val taskList = viewModel.buildTaskList(bankDetailsCompleted)
        val item = taskList.items(1)
        item.href mustBe defined
        item.status.tag.map(_.content.asHtml.body) must contain("taskListDashboard.status1")
      }

      "must show bank details as Completed with a link" in {
        val taskList = viewModel.buildTaskList(bankDetailsCompleted)
        val item = taskList.items(3)
        item.href mustBe defined
        item.status.content.asHtml.body must include("taskListDashboard.status3")
      }

    }

    "must use the correct idPrefix" in {
      val taskList = viewModel.buildTaskList(emptyUserAnswers)
      taskList.idPrefix mustEqual "make-a-claim-eu-vat-refund"
    }

    "showDeleteLink" - {
      "must return false when claim details are not completed" in {
        viewModel.showDeleteLink(emptyUserAnswers) mustBe false
      }

      "must return true when claim details are completed" in {
        val answers = emptyUserAnswers.set(ClaimDetailsCompletedQuery, true).success.value
        viewModel.showDeleteLink(answers) mustBe true
      }
    }

    "link Submit claim to the declaration page when bank details are completed" in {
      val answers = emptyUserAnswers
        .set(ClaimDetailsCompletedQuery, true)
        .success
        .value
        .set(BankDetailsCompletedQuery, true)
        .success
        .value
      val item = viewModel.buildTaskList(answers).items(4)

      item.href mustBe Some(controllers.routes.SubmitClaimDeclarationController.onPageLoad.url)
      item.status.tag.map(_.content.asHtml.body) must contain("taskListDashboard.status1")
    }
  }
}
