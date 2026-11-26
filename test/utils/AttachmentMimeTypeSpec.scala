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

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers

class AttachmentMimeTypeSpec extends AnyFreeSpec with Matchers {

  "AttachmentMimeType.forFileType" - {
    "must map known file types" in {
      AttachmentMimeType.forFileType(Some("pdf")) mustBe "application/pdf"
      AttachmentMimeType.forFileType(Some("jpg")) mustBe "image/jpeg"
      AttachmentMimeType.forFileType(Some("png")) mustBe "image/png"
    }

    "must ignore case, whitespace and a leading dot" in {
      AttachmentMimeType.forFileType(Some(" PDF ")) mustBe "application/pdf"
      AttachmentMimeType.forFileType(Some(".jpg")) mustBe "image/jpeg"
    }

    "must fall back to octet-stream for unknown or missing types" in {
      AttachmentMimeType.forFileType(Some("exe")) mustBe "application/octet-stream"
      AttachmentMimeType.forFileType(None) mustBe "application/octet-stream"
    }
  }

  "must map known file types" in {
    AttachmentMimeType.forFileType(Some("pdf")) mustBe "application/pdf"
    AttachmentMimeType.forFileType(Some("jpg")) mustBe "image/jpeg"
    AttachmentMimeType.forFileType(Some("jpeg")) mustBe "image/jpeg"
    AttachmentMimeType.forFileType(Some("png")) mustBe "image/png"
    AttachmentMimeType.forFileType(Some("tif")) mustBe "image/tiff"
    AttachmentMimeType.forFileType(Some("tiff")) mustBe "image/tiff"
    AttachmentMimeType.forFileType(Some("zip")) mustBe "application/zip"
  }
}
