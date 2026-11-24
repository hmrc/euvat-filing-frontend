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

package utils

object AttachmentMimeType {

  private val mimeTypes: Map[String, String] = Map(
    "pdf"  -> "application/pdf",
    "jpg"  -> "image/jpeg",
    "jpeg" -> "image/jpeg",
    "png"  -> "image/png",
    "doc"  -> "application/msword",
    "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    "xls"  -> "application/vnd.ms-excel",
    "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
  )

  def forFileType(fileType: Option[String]): String =
    fileType.map(_.trim.toLowerCase.stripPrefix(".")).flatMap(mimeTypes.get).getOrElse("application/octet-stream")
}
