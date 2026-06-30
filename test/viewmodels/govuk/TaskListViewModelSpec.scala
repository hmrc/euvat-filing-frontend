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

import base.SpecBase
import pages.ClaimDetailsCompletedPage
import play.api.i18n.Messages
import play.api.test.Helpers.stubMessages

class TaskListViewModelSpec extends SpecBase {

  implicit val messages: Messages = stubMessages()

  private val viewModel = new TaskListViewModel()

  "TaskListViewModel" - {

    "when claim details are not completed" - {

      val answers = emptyUserAnswers

      "must return 6 items" in {
        val taskList = viewModel.buildTaskList(answers)
        taskList.items.size mustEqual 6
      }

      "must show Add claim details as Not yet started with a link" in {
        val taskList = viewModel.buildTaskList(answers)
        val item = taskList.items.head
        item.href mustBe defined
        item.status.tag.map(_.content.asHtml.body) must contain("taskListDashboard.status1")
      }

      "must show Add a purchase as Cannot start yet with no link" in {
        val taskList = viewModel.buildTaskList(answers)
        val item = taskList.items(1)
        item.href mustBe None
        item.status.tag.map(_.content.asHtml.body) must contain("taskListDashboard.status2")
      }

      "must show Add an import as Cannot start yet with no link" in {
        val taskList = viewModel.buildTaskList(answers)
        val item = taskList.items(2)
        item.href mustBe None
        item.status.tag.map(_.content.asHtml.body) must contain("taskListDashboard.status2")
      }

      "must show Add supporting documents as Cannot start yet with no link" in {
        val taskList = viewModel.buildTaskList(answers)
        val item = taskList.items(3)
        item.href mustBe None
        item.status.tag.map(_.content.asHtml.body) must contain("taskListDashboard.status2")
      }

      "must show Add bank details as Cannot start yet with no link" in {
        val taskList = viewModel.buildTaskList(answers)
        val item = taskList.items(4)
        item.href mustBe None
        item.status.tag.map(_.content.asHtml.body) must contain("taskListDashboard.status2")
      }

      "must show Submit claim as Cannot start yet with no link" in {
        val taskList = viewModel.buildTaskList(answers)
        val item = taskList.items(5)
        item.href mustBe None
        item.status.tag.map(_.content.asHtml.body) must contain("taskListDashboard.status2")
      }
    }

    "when claim details are completed" - {

      val answers = emptyUserAnswers.set(ClaimDetailsCompletedPage, true).success.value

      "must return 6 items" in {
        val taskList = viewModel.buildTaskList(answers)
        taskList.items.size mustEqual 6
      }

      "must show View claim details as Completed with a link" in {
        val taskList = viewModel.buildTaskList(answers)
        val item = taskList.items.head
        item.href mustBe defined
        item.status.content.asHtml.body must include("taskListDashboard.status3")
      }

      "must show Add a purchase as Not yet started with a link" in {
        val taskList = viewModel.buildTaskList(answers)
        val item = taskList.items(1)
        item.href mustBe defined
        item.status.tag.map(_.content.asHtml.body) must contain("taskListDashboard.status1")
      }

      "must show Add an import as Not yet started with a link" in {
        val taskList = viewModel.buildTaskList(answers)
        val item = taskList.items(2)
        item.href mustBe defined
        item.status.tag.map(_.content.asHtml.body) must contain("taskListDashboard.status1")
      }

      "must show Add supporting documents as Not yet started with a link" in {
        val taskList = viewModel.buildTaskList(answers)
        val item = taskList.items(3)
        item.href mustBe defined
        item.status.tag.map(_.content.asHtml.body) must contain("taskListDashboard.status1")
      }

      "must show Add bank details as Not yet started with a link" in {
        val taskList = viewModel.buildTaskList(answers)
        val item = taskList.items(4)
        item.href mustBe defined
        item.status.tag.map(_.content.asHtml.body) must contain("taskListDashboard.status1")
      }

      "must show Submit claim as Cannot start yet with no link" in {
        val taskList = viewModel.buildTaskList(answers)
        val item = taskList.items(5)
        item.href mustBe None
        item.status.tag.map(_.content.asHtml.body) must contain("taskListDashboard.status2")
      }
    }

    "must use the correct idPrefix" in {
      val taskList = viewModel.buildTaskList(emptyUserAnswers)
      taskList.idPrefix mustEqual "make-eu-vat-claim"
    }
  }
}