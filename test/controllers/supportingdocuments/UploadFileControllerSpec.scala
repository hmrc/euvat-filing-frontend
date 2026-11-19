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
<<<<<<< HEAD
import models.responses.{ApplicationResponse, CreateUploadTrackingResponse}
import org.mockito.ArgumentMatchers.{any, eq as mockitoEq}
import org.mockito.Mockito.{never, verify, when}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.ClaimApplicationResponseQuery
import uk.gov.hmrc.http.UpstreamErrorResponse

import scala.concurrent.Future

class UploadFileControllerSpec extends SpecBase {

  private val applicationId = 133L
  private lazy val onPageLoadUrl = routes.UploadFileController.onPageLoad().url

  private val uploadTracking = CreateUploadTrackingResponse(
    reference = "ref-123",
    uploadUrl = "http://localhost:9570/upscan/upload-proxy",
    fields    = Map("key" -> "ref-123", "x-amz-algorithm" -> "AWS4-HMAC-SHA256")
  )

  // adjust to match the fields of the ApplicationResponse stored by ClaimApplicationResponseQuery
  private val userAnswersWithApplication =
    emptyUserAnswers
      .set(ClaimApplicationResponseQuery, ApplicationResponse(applicationId = applicationId, applicationNumber = "GB123456789", updateSeqNumber = 1))
      .success
      .value

  // SpecBase binds mockEuVatRefundsService into every application and resets it before each test,
  // so each test only needs to stub it
  private def uploadTrackingReturns(result: Future[CreateUploadTrackingResponse]): Unit =
    when(mockEuVatRefundsService.createUploadTracking(any[Long]())(any())).thenReturn(result)

  private def applicationWithUpload = {
    uploadTrackingReturns(Future.successful(uploadTracking))
    applicationBuilder(userAnswers = Some(userAnswersWithApplication)).build()
  }

  "UploadFileController.onPageLoad" - {

    "must start an upscan upload for the application" in {
      val application = applicationWithUpload

      running(application) {
        status(route(application, FakeRequest(GET, onPageLoadUrl)).value) mustEqual OK
        verify(mockEuVatRefundsService).createUploadTracking(mockitoEq(applicationId))(any())
      }
    }

    "must display the upload page as designed" in {
      val application = applicationWithUpload

      running(application) {
        val result = route(application, FakeRequest(GET, onPageLoadUrl)).value
=======
import forms.supportingdocuments.UploadFileFormProvider
import forms.supportingdocuments.UploadFileFormProvider.{descriptionFieldName, descriptionMaxLength, fileInputName}
import play.api.http.Status.REQUEST_ENTITY_TOO_LARGE
import play.api.libs.Files.{TemporaryFile, TemporaryFileCreator}
import play.api.mvc.MultipartFormData
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import org.jsoup.Jsoup

import java.nio.charset.StandardCharsets
import java.nio.file.Files

class UploadFileControllerSpec extends SpecBase {

  "UploadFileController" - {

    "must display the supporting document upload form" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val result = route(application, FakeRequest(GET, "/file-eu-vat/supporting-documents/upload-file")).value
>>>>>>> main
        val content = contentAsString(result)

        status(result) mustEqual OK
        header("Cache-Control", result).value mustEqual "no-store"
<<<<<<< HEAD
        onPageLoadUrl mustEqual "/file-eu-vat/supporting-documents/upload-file"
=======
        routes.UploadFileController.onPageLoad().url mustEqual "/file-eu-vat/supporting-documents/upload-file"
>>>>>>> main
        content must include("<title>Upload file - EU VAT - GOV.UK</title>")
        content must include("<h1 class=\"govuk-label-wrapper\">")
        content must include(s"""<span class="govuk-caption-l">${messages(application)("supportingDocuments.caption")}</span>""")
        content must include(messages(application)("supportingDocuments.uploadFile.file.hint"))
        content must include(messages(application)("supportingDocuments.uploadFile.description.label"))
<<<<<<< HEAD
=======
        content must include("""autoComplete="off"""")
        content must include("name=\"file-upload-1\"")
        content must include("name=\"fileuploadDescription\"")
        content must not include "uploadFileFormToken"
>>>>>>> main
        content must include("data-module=\"govuk-file-upload\"")
        content must include("""href="#" class="govuk-back-link" data-module="hmrc-back-link">Back</a>""")
        content must include("You have 255 characters remaining")
      }
    }

<<<<<<< HEAD
    "must post the file straight to upscan with the upscan fields" in {
      val application = applicationWithUpload

      running(application) {
        val content = contentAsString(route(application, FakeRequest(GET, onPageLoadUrl)).value)

        content must include(s"""action="${uploadTracking.uploadUrl}"""")
        content must include("""enctype="multipart/form-data"""")
        content must include("""name="key" value="ref-123"""")
        content must include("""name="x-amz-algorithm" value="AWS4-HMAC-SHA256"""")
        content must include("""name="file"""")
        content must not include "csrfToken"
      }
    }

    "must keep the description and the button outside the upscan form" in {
      val application = applicationWithUpload

      running(application) {
        val content = contentAsString(route(application, FakeRequest(GET, onPageLoadUrl)).value)
        val formStart = content.indexOf("""id="upscan-form"""")
        val formEnd = content.indexOf("</form>", formStart)
        val upscanForm = content.substring(formStart, formEnd)

        formStart must be >= 0
        upscanForm must include("""name="file"""")
        upscanForm must not include "fileuploadDescription"
        content.indexOf("""name="fileuploadDescription"""") must be > formEnd
        content must include("""form="upscan-form"""")
      }
    }

    "must show an error when upscan sends the user back with an error code" in {
      val application = applicationWithUpload

      running(application) {
        val result = route(application, FakeRequest(GET, s"$onPageLoadUrl?key=ref-123&errorCode=EntityTooLarge")).value
        val content = contentAsString(result)

        status(result) mustEqual OK
        content must include(
          s"""<title>${messages(application)("error.title.prefix")} ${messages(application)("supportingDocuments.uploadFile.title")}"""
        )
        content must include(messages(application)("supportingDocuments.uploadFile.error.size"))
        content must include("""id="file-error"""")
        content must include("""aria-describedby="file-error file-hint"""")
        content must include("""href="#file"""")
      }
    }

    "must show the empty-file error when upscan reports the file is too small" in {
      val application = applicationWithUpload

      running(application) {
        val content = contentAsString(route(application, FakeRequest(GET, s"$onPageLoadUrl?errorCode=EntityTooSmall")).value)

        content must include(messages(application)("supportingDocuments.uploadFile.error.empty"))
        content must not include messages(application)("supportingDocuments.uploadFile.error.required")
      }
    }

    "must redirect to Journey Recovery when the upload cannot be started" in {
      uploadTrackingReturns(Future.failed(UpstreamErrorResponse("No application found", 404)))
      val application = applicationBuilder(userAnswers = Some(userAnswersWithApplication)).build()

      running(application) {
        val result = route(application, FakeRequest(GET, onPageLoadUrl)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery without starting an upload when there is no application id" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val result = route(application, FakeRequest(GET, onPageLoadUrl)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
        verify(mockEuVatRefundsService, never()).createUploadTracking(any[Long]())(any())
      }
    }
  }
}
=======
    "must redirect a valid upload to journey recovery until the uploading page exists" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

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
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
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
>>>>>>> main
