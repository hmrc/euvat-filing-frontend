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

package services

import base.SpecBase
import play.api.i18n.Messages
import play.api.test.Helpers.stubMessages

class PaginationServiceSpec extends SpecBase {

  implicit val messages: Messages = stubMessages()
  private val service = new PaginationService()
  private val baseUrl = "/file-eu-vat/supporting-documents/uploaded-files"

  "PaginationService" - {

    "must use 10 records per page" in {
      service.recordsPerPage mustBe 10
    }

    "startAt" - {
      "must return 0 for page 1" in {
        service.startAt(1) mustBe 0
      }
      "must return 20 for page 3" in {
        service.startAt(3) mustBe 20
      }
      "must treat a page below 1 as page 1" in {
        service.startAt(0) mustBe 0
      }
    }

    "totalPages" - {
      "must return 1 when there are no records" in {
        service.totalPages(0) mustBe 1
      }
      "must return 1 for exactly 10 records" in {
        service.totalPages(10) mustBe 1
      }
      "must return 2 for 11 records" in {
        service.totalPages(11) mustBe 2
      }
    }

    "paginate" - {

      "must return no items when all records fit on one page" in {
        val result = service.paginate(totalRecords = 3, currentPage = 1, baseUrl = baseUrl)

        result.items mustBe empty
        result.previous mustBe None
        result.next mustBe None
      }

      "must return page items and a next link on the first of two pages" in {
        val result = service.paginate(totalRecords = 12, currentPage = 1, baseUrl = baseUrl)

        result.items.map(_.number) mustBe Seq("1", "2")
        result.items.map(_.href) mustBe Seq(s"$baseUrl?page=1", s"$baseUrl?page=2")
        result.items.find(_.current).map(_.number) mustBe Some("1")
        result.previous mustBe None
        result.next.map(_.href) mustBe Some(s"$baseUrl?page=2")
      }

      "must return a previous link and no next link on the last page" in {
        val result = service.paginate(totalRecords = 12, currentPage = 2, baseUrl = baseUrl)

        result.items.find(_.current).map(_.number) mustBe Some("2")
        result.previous.map(_.href) mustBe Some(s"$baseUrl?page=1")
        result.next mustBe None
      }

      "must show an ellipsis either side when the current page is in the middle of many pages" in {
        val result = service.paginate(totalRecords = 100, currentPage = 5, baseUrl = baseUrl)

        result.items.map(item => if (item.ellipsis) "..." else item.number) mustBe
          Seq("1", "...", "3", "4", "5", "6", "7", "...", "10")
      }

      "must treat a current page past the end as the last page" in {
        val result = service.paginate(totalRecords = 12, currentPage = 9, baseUrl = baseUrl)

        result.items.find(_.current).map(_.number) mustBe Some("2")
      }
    }
  }
}
