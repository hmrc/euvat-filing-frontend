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

package controllers.supportingdocuments

import base.SpecBase
import forms.supportingdocuments.UploadFileFormProvider
import forms.supportingdocuments.UploadFileFormProvider.{descriptionFieldName, descriptionMaxLength, fileInputName}
import play.api.http.Status.REQUEST_ENTITY_TOO_LARGE
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.verify
import play.api.inject.bind
import models.UserAnswers
import pages.supportingdocuments.YourFileNamePage
import play.api.libs.Files.{TemporaryFile, TemporaryFileCreator}
import play.api.mvc.MultipartFormData
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import org.jsoup.Jsoup

import java.nio.charset.StandardCharsets
import java.nio.file.Files

class UploadFileControllerSpec extends SpecBase {

  private def applicationWithMockSessionRepository =
    applicationBuilder(userAnswers = Some(emptyUserAnswers))
      .overrides(bind[repositories.SessionRepository].toInstance(mockSessionRepository))
      .build()

  "UploadFileController" - {

    "must display the supporting document upload form" in {
      val application = applicationWithMockSessionRepository

      running(application) {
        val result = route(application, FakeRequest(GET, "/file-eu-vat/supporting-documents/upload-file")).value
        val content = contentAsString(result)

        status(result) mustEqual OK
        header("Cache-Control", result).value mustEqual "no-store"
        routes.UploadFileController.onPageLoad().url mustEqual "/file-eu-vat/supporting-documents/upload-file"
        content must include("<title>Upload file - EU VAT - GOV.UK</title>")
        content must include("<h1 class=\"govuk-label-wrapper\">")
        content must include(s"""<span class="govuk-caption-l">${messages(application)("supportingDocuments.caption")}</span>""")
        content must include(messages(application)("supportingDocuments.uploadFile.file.hint"))
        content must include(messages(application)("supportingDocuments.uploadFile.description.label"))
        content must include("""autoComplete="off"""")
        content must include("name=\"file-upload-1\"")
        content must include("name=\"fileuploadDescription\"")
        content must not include "uploadFileFormToken"
        content must include("data-module=\"govuk-file-upload\"")
        content must include("""href="#" class="govuk-back-link" data-module="hmrc-back-link">Back</a>""")
        content must include("You have 255 characters remaining")
      }
    }

    "must redirect a valid upload to the your file page and retain the filename for the UI" in {
      val application = applicationWithMockSessionRepository

      running(application) {
        val temporaryFile = application.injector.instanceOf[TemporaryFileCreator].create()
        Files.write(temporaryFile.path, "receipt contents".getBytes(StandardCharsets.UTF_8))
        val multipartData: MultipartFormData[TemporaryFile] = MultipartFormData(
          dataParts = Map.empty,
          files     = Seq(MultipartFormData.FilePart("file-upload-1", "receipt.pdf", Some("application/pdf"), temporaryFile)),
          badParts  = Seq.empty
        )
        val request = FakeRequest(POST, routes.UploadFileController.onSubmit().url)
          .withMultipartFormDataBody(multipartData)
        val result = route(application, request).value

        try {
          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.YourFileController.onPageLoad().url
          val savedAnswers = ArgumentCaptor.forClass(classOf[UserAnswers])
          verify(mockSessionRepository).set(savedAnswers.capture())
          savedAnswers.getValue.get(YourFileNamePage) mustBe Some("receipt.pdf")
        } finally {
          Files.deleteIfExists(temporaryFile.path)
        }
      }
    }

    "must show an error when the uploaded file is empty" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val temporaryFile = application.injector.instanceOf[TemporaryFileCreator].create()
        val multipartData = MultipartFormData(
          dataParts = Map.empty,
          files     = Seq(MultipartFormData.FilePart("file-upload-1", "empty.pdf", Some("application/pdf"), temporaryFile)),
          badParts  = Seq.empty
        )
        val request = FakeRequest(POST, routes.UploadFileController.onSubmit().url)
          .withMultipartFormDataBody(multipartData)
        val result = route(application, request).value

        try {
          status(result) mustEqual BAD_REQUEST
          val errorContent = contentAsString(result)
          errorContent must include(
            s"""<title>${messages(application)("error.title.prefix")} ${messages(application)("supportingDocuments.uploadFile.title")}"""
          )
          errorContent must include(messages(application)("supportingDocuments.uploadFile.error.empty"))
          errorContent must include("""id="file-upload-1-error"""")
          errorContent must include("""aria-describedby="file-upload-1-error file-upload-1-hint"""")
          val hintPosition = errorContent.indexOf("""id="file-upload-1-hint"""")
          val errorPosition = errorContent.indexOf("""id="file-upload-1-error"""")
          val fileInputPosition = errorContent.indexOf("""type="file"""")
          hintPosition      must be >= 0
          errorPosition     must be > hintPosition
          fileInputPosition must be > errorPosition
          val errorGroupStart = errorContent.indexOf("""class="govuk-form-group govuk-form-group--error"""")
          val captionPosition = errorContent.indexOf(
            s"""<span class="govuk-caption-l">${messages(application)("supportingDocuments.caption")}</span>"""
          )
          val headingPosition = errorContent.indexOf("<h1 class=\"govuk-label-wrapper\">")
          errorGroupStart must be >= 0
          captionPosition must be > errorGroupStart
          headingPosition must be > captionPosition
          header("Cache-Control", result).value mustEqual "no-store"
        } finally {
          Files.deleteIfExists(temporaryFile.path)
        }
      }
    }

    "must represent file-size I/O errors as validation failures" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val temporaryFile = application.injector.instanceOf[TemporaryFileCreator].create()
        Files.write(temporaryFile.path, "receipt contents".getBytes(StandardCharsets.UTF_8))
        Files.delete(temporaryFile.path)
        val filePart = MultipartFormData.FilePart(
          fileInputName,
          "receipt.pdf",
          Some("application/pdf"),
          temporaryFile
        )
        val controller = application.injector.instanceOf[UploadFileController]
        val formProvider = application.injector.instanceOf[UploadFileFormProvider]
        val result = controller.validateFile(
          filePart,
          formProvider()
        )

        try {
          result.isLeft mustBe true
        } finally {
          Files.deleteIfExists(temporaryFile.path)
        }
      }
    }

    "must show a select-file error when no file is submitted" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val multipartData: MultipartFormData[TemporaryFile] = MultipartFormData(
          dataParts = Map.empty,
          files     = Seq.empty,
          badParts  = Seq.empty
        )
        val request = FakeRequest(POST, routes.UploadFileController.onSubmit().url).withMultipartFormDataBody(multipartData)
        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) must include(messages(application)("supportingDocuments.uploadFile.error.required"))
        contentAsString(result) must not include messages(application)("supportingDocuments.uploadFile.error.empty")
      }
    }

    "must reject an unsupported file extension" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val temporaryFile = application.injector.instanceOf[TemporaryFileCreator].create()
        Files.write(temporaryFile.path, "receipt contents".getBytes(StandardCharsets.UTF_8))
        val multipartData = MultipartFormData(
          dataParts = Map.empty,
          files = Seq(
            MultipartFormData.FilePart(fileInputName,
                                       "receipt.docx",
                                       Some("application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
                                       temporaryFile
                                      )
          ),
          badParts = Seq.empty
        )
        val request = FakeRequest(POST, routes.UploadFileController.onSubmit().url).withMultipartFormDataBody(multipartData)
        val result = route(application, request).value

        try {
          status(result) mustEqual BAD_REQUEST
          contentAsString(result) must include(messages(application)("supportingDocuments.uploadFile.error.type"))
        } finally {
          Files.deleteIfExists(temporaryFile.path)
        }
      }
    }

    "must reject a filename without an extension separator" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val temporaryFile = application.injector.instanceOf[TemporaryFileCreator].create()
        Files.write(temporaryFile.path, "receipt contents".getBytes(StandardCharsets.UTF_8))
        val multipartData = MultipartFormData(
          dataParts = Map.empty,
          files = Seq(
            MultipartFormData.FilePart(fileInputName, "pdf", Some("application/pdf"), temporaryFile)
          ),
          badParts = Seq.empty
        )
        val request = FakeRequest(POST, routes.UploadFileController.onSubmit().url).withMultipartFormDataBody(multipartData)
        val result = route(application, request).value

        try {
          status(result) mustEqual BAD_REQUEST
          contentAsString(result) must include(messages(application)("supportingDocuments.uploadFile.error.type"))
        } finally {
          Files.deleteIfExists(temporaryFile.path)
        }
      }
    }

    "must show the description limit using the shared maximum length" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val temporaryFile = application.injector.instanceOf[TemporaryFileCreator].create()
        Files.write(temporaryFile.path, "receipt contents".getBytes(StandardCharsets.UTF_8))
        val description = "x" * (descriptionMaxLength + 1)
        val multipartData = MultipartFormData(
          dataParts = Map(descriptionFieldName -> Seq(description)),
          files = Seq(
            MultipartFormData.FilePart("file-upload-1", "receipt.pdf", Some("application/pdf"), temporaryFile)
          ),
          badParts = Seq.empty
        )
        val request = FakeRequest(POST, routes.UploadFileController.onSubmit().url).withMultipartFormDataBody(multipartData)
        val result = route(application, request).value

        try {
          status(result) mustEqual BAD_REQUEST
          val errorMessage =
            messages(application)("supportingDocuments.uploadFile.error.description.maxLength", descriptionMaxLength)
          val errorContent = contentAsString(result)

          errorContent must include(errorMessage)
          errorContent must include(description)
          val summaryErrors = Jsoup.parse(errorContent).select(".govuk-error-summary__list a")
          summaryErrors.size mustBe 1
          summaryErrors.get(0).text mustBe errorMessage
          summaryErrors.get(0).attr("href") mustBe "#fileupload-description"
        } finally {
          Files.deleteIfExists(temporaryFile.path)
        }
      }
    }

    "must show file and description errors in the summary with the file error first" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val temporaryFile = application.injector.instanceOf[TemporaryFileCreator].create()
        Files.write(temporaryFile.path, "receipt contents".getBytes(StandardCharsets.UTF_8))
        val description = "x" * (descriptionMaxLength + 1)
        val multipartData = MultipartFormData(
          dataParts = Map(descriptionFieldName -> Seq(description)),
          files = Seq(
            MultipartFormData.FilePart(fileInputName, "receipt.docx", Some("application/octet-stream"), temporaryFile)
          ),
          badParts = Seq.empty
        )
        val request = FakeRequest(POST, routes.UploadFileController.onSubmit().url).withMultipartFormDataBody(multipartData)
        val result = route(application, request).value

        try {
          status(result) mustEqual BAD_REQUEST
          val errorLinks = Jsoup.parse(contentAsString(result)).select(".govuk-error-summary__list a")

          errorLinks.size mustBe 2
          errorLinks.get(0).attr("href") mustBe "#file-upload-1"
          errorLinks.get(0).text mustBe messages(application)("supportingDocuments.uploadFile.error.type")
          errorLinks.get(1).attr("href") mustBe "#fileupload-description"
          errorLinks.get(1).text mustBe messages(application)(
            "supportingDocuments.uploadFile.error.description.maxLength",
            descriptionMaxLength
          )
        } finally {
          Files.deleteIfExists(temporaryFile.path)
        }
      }
    }

    "must show the file size error for a file above the upload limit" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val oversizedFile = application.injector.instanceOf[TemporaryFileCreator].create()
        Files.write(oversizedFile.path, Array.fill(5 * 1024 * 1024 + 1)(0.toByte))
        val oversizedMultipartData = MultipartFormData(
          dataParts = Map.empty,
          files = Seq(
            MultipartFormData.FilePart("file-upload-1", "large.pdf", Some("application/pdf"), oversizedFile)
          ),
          badParts = Seq.empty
        )
        val oversizedRequest =
          FakeRequest(POST, routes.UploadFileController.onSubmit().url).withMultipartFormDataBody(oversizedMultipartData)
        val oversizedResult = route(application, oversizedRequest).value

        try {
          status(oversizedResult) mustEqual BAD_REQUEST
          contentAsString(oversizedResult) must include(messages(application)("supportingDocuments.uploadFile.error.size"))
        } finally {
          Files.deleteIfExists(oversizedFile.path)
        }
      }
    }

    "must show the file size error when the multipart request exceeds the parser limit" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val boundary = "upload-boundary"
        val multipartStart =
          s"""--$boundary\r
             |Content-Disposition: form-data; name="file-upload-1"; filename="large.pdf"\r
             |Content-Type: application/pdf\r
             |\r
             |""".stripMargin.getBytes(StandardCharsets.UTF_8)
        val multipartEnd = s"\r\n--$boundary--\r\n".getBytes(StandardCharsets.UTF_8)
        val requestBody =
          multipartStart ++ Array.fill(7 * 1024 * 1024)(0.toByte) ++ multipartEnd
        val request = FakeRequest(POST, routes.UploadFileController.onSubmit().url)
          .withHeaders("Content-Type" -> s"multipart/form-data; boundary=$boundary")
          .withBody(requestBody)
        val result = route(application, request).value

        status(result) mustEqual REQUEST_ENTITY_TOO_LARGE
        contentAsString(result) must include(messages(application)("supportingDocuments.uploadFile.error.size"))
        contentAsString(result) must include(
          s"""<title>${messages(application)("error.title.prefix")} ${messages(application)("supportingDocuments.uploadFile.title")}"""
        )
        header("Cache-Control", result).value mustEqual "no-store"
      }
    }
  }
}
