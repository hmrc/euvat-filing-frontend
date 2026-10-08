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
import pages.{ClaimDetailsCompletedPage, PurchaseImportSummaryPage}
import play.api.i18n.Messages
import play.api.test.Helpers.stubMessages
import viewmodels.TaskListViewModel
import queries.ClaimDetailsCompletedQuery

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
  }
}
