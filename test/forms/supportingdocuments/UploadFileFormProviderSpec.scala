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

package forms.supportingdocuments

import forms.supportingdocuments.UploadFileFormProvider.{fileErrorKey, fileInputName}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

class UploadFileFormProviderSpec extends AnyWordSpec with Matchers {

  private val form = new UploadFileFormProvider()()

  "UploadFileFormProvider" should {

    "use the field name upscan requires" in {
      fileInputName mustBe "file"
      fileErrorKey mustBe "file"
    }

    "start with no errors" in {
      form.hasErrors mustBe false
    }

    "hold an upscan error against the file field" in {
      val formWithError = form.withError(fileErrorKey, "supportingDocuments.uploadFile.error.size")

      formWithError.error(fileErrorKey).map(_.message) mustBe Some("supportingDocuments.uploadFile.error.size")
    }
  }
}