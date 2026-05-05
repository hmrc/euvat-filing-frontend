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

package viewmodels.taskList

import models.UserAnswers

/** Status of the "Add claim details" row on the TaskListDashboard. */
enum ClaimDetailsStatus {
  case NotStarted, InProgress, Completed

  /** Messages key the view should look up to render the tag text. */
  def messageKey: String = this match {
    case NotStarted => "taskListDashboard.status1"
    case InProgress => "taskListDashboard.status3"
    case Completed  => "taskListDashboard.status4"
  }

  /** Extra CSS classes applied to the GDS Tag. Empty for default (blue) styling. */
  def tagClasses: String = this match {
    case NotStarted => ""
    case InProgress => "govuk-tag--light-blue"
    case Completed  => ""
  }
}

object ClaimDetailsStatus {

  def from(userAnswers: UserAnswers): ClaimDetailsStatus = {
    val _ = userAnswers // referenced here so future widenings don't change the signature
    NotStarted
  }
}
