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

import play.api.data.Form
import play.api.data.Forms.{optional, text}
import play.api.data.validation.{Constraint, Invalid, Valid, ValidationError}

import javax.inject.Inject

object UploadFileFormProvider {
  val fileErrorKey = "file"
  val fileInputName = "file-upload-1"
  val descriptionFieldName = "fileuploadDescription"
  val descriptionMaxLength = 255
}

class UploadFileFormProvider @Inject() {

  def apply(): Form[Option[String]] =
    Form(
      UploadFileFormProvider.descriptionFieldName -> optional(text).verifying(
        Constraint[Option[String]] { description =>
          if (description.forall(_.length <= UploadFileFormProvider.descriptionMaxLength)) {
            Valid
          } else {
            Invalid(
              ValidationError(
                "supportingDocuments.uploadFile.error.description.maxLength",
                UploadFileFormProvider.descriptionMaxLength
              )
            )
          }
        }
      )
    )
}
