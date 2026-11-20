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

import play.api.i18n.Messages
import viewmodels.govuk.PaginationFluency.*

import javax.inject.{Inject, Singleton}

case class PaginationConfig(
  recordsPerPage: Int = 10,
  maxVisiblePages: Int = 5
)

@Singleton
class PaginationService @Inject() {

  private val config = PaginationConfig()

  def recordsPerPage: Int = config.recordsPerPage

  def startAt(currentPage: Int): Int =
    (Math.max(1, currentPage) - 1) * config.recordsPerPage

  def totalPages(totalRecords: Int): Int =
    Math.max(1, Math.ceil(totalRecords.toDouble / config.recordsPerPage).toInt)

  def paginate(
    totalRecords: Int,
    currentPage: Int,
    baseUrl: String
  )(implicit messages: Messages): PaginationViewModel = {
    val pages = totalPages(totalRecords)
    val validCurrentPage = Math.max(1, Math.min(currentPage, pages))

    if (pages <= 1) {
      PaginationViewModel()
    } else {
      PaginationViewModel(
        items    = generatePageItems(validCurrentPage, pages, baseUrl),
        previous = createPreviousLink(validCurrentPage, baseUrl),
        next     = createNextLink(validCurrentPage, pages, baseUrl)
      )
    }
  }

  private def buildUrl(baseUrl: String, page: Int): String =
    s"$baseUrl?page=$page"

  private def createPreviousLink(currentPage: Int, baseUrl: String)(implicit messages: Messages): Option[PaginationLinkViewModel] =
    if (currentPage > 1) {
      Some(
        PaginationLinkViewModel(buildUrl(baseUrl, currentPage - 1))
          .withText("site.pagination.previous")
          .withAriaLabel(messages("site.pagination.goToPage", currentPage - 1))
      )
    } else {
      None
    }

  private def createNextLink(currentPage: Int, totalPages: Int, baseUrl: String)(implicit messages: Messages): Option[PaginationLinkViewModel] =
    if (currentPage < totalPages) {
      Some(
        PaginationLinkViewModel(buildUrl(baseUrl, currentPage + 1))
          .withText("site.pagination.next")
          .withAriaLabel(messages("site.pagination.goToPage", currentPage + 1))
      )
    } else {
      None
    }

  private def createPageItem(page: Int, currentPage: Int, baseUrl: String): PaginationItemViewModel =
    PaginationItemViewModel(number = page.toString, href = buildUrl(baseUrl, page))
      .withVisuallyHiddenText("site.pagination.goToPage")
      .withCurrent(page == currentPage)

  private def generatePageItems(currentPage: Int, totalPages: Int, baseUrl: String): Seq[PaginationItemViewModel] = {
    val pageRange = calculatePageRange(currentPage, totalPages)
    val items = scala.collection.mutable.ListBuffer[PaginationItemViewModel]()

    if (pageRange.head > 1) {
      items += createPageItem(page = 1, currentPage = currentPage, baseUrl = baseUrl)
      if (pageRange.head > 2) {
        items += PaginationItemViewModel.ellipsis()
      }
    }

    pageRange.foreach { page =>
      items += createPageItem(page = page, currentPage = currentPage, baseUrl = baseUrl)
    }

    if (pageRange.last < totalPages) {
      if (pageRange.last < totalPages - 1) {
        items += PaginationItemViewModel.ellipsis()
      }
      items += createPageItem(page = totalPages, currentPage = currentPage, baseUrl = baseUrl)
    }

    items.toSeq
  }

  private def calculatePageRange(currentPage: Int, totalPages: Int): Range = {
    val halfVisible = config.maxVisiblePages / 2
    val startPage = Math.max(1, currentPage - halfVisible)
    val endPage = Math.min(totalPages, startPage + config.maxVisiblePages - 1)

    val adjustedStartPage = if (endPage - startPage < config.maxVisiblePages - 1) {
      Math.max(1, endPage - config.maxVisiblePages + 1)
    } else startPage

    adjustedStartPage to endPage
  }
}
