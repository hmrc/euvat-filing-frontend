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

import controllers.supportingdocuments.routes
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.pagination.{Pagination, PaginationItem, PaginationLink}

object UploadedFilesPagination {

  def apply(currentPage: Int, totalPages: Int)(implicit messages: Messages): Option[Pagination] =
    Option.when(totalPages > 1) {
      Pagination(
        items = Some((1 to totalPages).map { page =>
          PaginationItem(
            href    = routes.UploadedFilesController.onPageLoad(page).url,
            number  = Some(page.toString),
            current = Some(page == currentPage)
          )
        }),
        previous = Option.when(currentPage > 1)(
          PaginationLink(href = routes.UploadedFilesController.onPageLoad(currentPage - 1).url)
        ),
        next = Option.when(currentPage < totalPages)(
          PaginationLink(href = routes.UploadedFilesController.onPageLoad(currentPage + 1).url)
        ),
        landmarkLabel = Some(messages("supportingDocuments.uploadedFiles.pagination.label"))
      )
    }
}
