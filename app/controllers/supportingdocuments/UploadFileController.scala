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

import controllers.actions.*
import forms.supportingdocuments.UploadFileFormProvider
import forms.supportingdocuments.UploadFileFormProvider.{descriptionFieldName, fileErrorKey, fileInputName}
import pages.supportingdocuments.YourFileNamePage
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.Logging
import play.api.libs.Files.TemporaryFile
import play.api.mvc.{Action, AnyContent, BodyParser, MessagesControllerComponents, MultipartFormData, Results}
import play.api.mvc.MultipartFormData.FilePart
import queries.UploadedFileStatusQuery
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import repositories.SessionRepository
import views.html.supportingdocuments.UploadFileView

import java.io.IOException
import java.nio.file.Files
import java.util.Locale
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class UploadFileController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  sessionRepository: SessionRepository,
  formProvider: UploadFileFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: UploadFileView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  private val maxFileSize = 5 * 1024 * 1024
  private val maxRequestSize = 6 * 1024 * 1024
  private val acceptedExtensions = Set("jpg", "jpeg", "tif", "tiff", "pdf", "zip")

  private def form: Form[Option[String]] = formProvider()

  def onPageLoad(): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
    Ok(view(form))
      .withHeaders("Cache-Control" -> "no-store")
  }

  def onSubmit(): Action[MultipartFormData[TemporaryFile]] =
    (identify andThen getData andThen requireData)
      .async(multipartFormDataParser) { implicit request =>
        val boundForm = form.bind(request.body.dataParts.view.mapValues(_.headOption.getOrElse("")).toMap)
        val formWithFileErrors =
          request.body.file(fileInputName) match {
            case None =>
              boundForm.withError(fileErrorKey, "supportingDocuments.uploadFile.error.required")
            case Some(file) if file.filename.trim.isEmpty =>
              boundForm.withError(fileErrorKey, "supportingDocuments.uploadFile.error.required")
            case Some(file) =>
              validateFile(file, boundForm) match {
                case Right(validatedForm) =>
                  validatedForm
                case Left(error) =>
                  logger.error("Unable to determine the size of the selected supporting document", error)
                  boundForm.withError(fileErrorKey, "supportingDocuments.uploadFile.error.upload")
              }
          }

        val orderedForm = formWithFileErrors.copy(
          errors = formWithFileErrors.errors.filter(_.key == fileErrorKey) ++
            formWithFileErrors.errors.filterNot(_.key == fileErrorKey)
        )

        if (orderedForm.hasErrors) {
          Future.successful(BadRequest(view(orderedForm)).withHeaders("Cache-Control" -> "no-store"))
        } else {
          val filename = request.body.file("file-upload-1").map(_.filename).get
          val updatedAnswers = for {
            withFilename <- request.userAnswers.set(YourFileNamePage, filename)
            withStatus   <- withFilename.set(UploadedFileStatusQuery, YourFileController.AwaitingUpload)
          } yield withStatus

          for {
            answers <- Future.fromTry(updatedAnswers)
            _       <- sessionRepository.set(answers)
          } yield {
            // TODO: call Upscan initiate and add-attachment instead of using the UI-only placeholder status.
            Redirect(routes.YourFileController.onPageLoad())
          }
        }
      }

  private def multipartFormDataParser: BodyParser[MultipartFormData[TemporaryFile]] = {
    val parser = parse.multipartFormData(maxLength = maxRequestSize, allowEmptyFiles = true)

    BodyParser("supporting-document-upload") { request =>
      parser(request).map {
        case Left(result) if result.header.status == 413 =>
          val errorForm = form.withError(fileErrorKey, "supportingDocuments.uploadFile.error.size")
          val messages = messagesApi.preferred(request)
          Left(
            Results
              .Status(413)(view(errorForm)(request, messages))
              .withHeaders("Cache-Control" -> "no-store")
          )
        case parseResult =>
          parseResult
      }
    }
  }

  private[supportingdocuments] def validateFile(
    file: FilePart[TemporaryFile],
    form: Form[Option[String]]
  ): Either[IOException, Form[Option[String]]] =
    try {
      val fileSize = Files.size(file.ref.path)
      Right(
        if (fileSize == 0) {
          form.withError(fileErrorKey, "supportingDocuments.uploadFile.error.empty")
        } else if (fileSize > maxFileSize) {
          form.withError(fileErrorKey, "supportingDocuments.uploadFile.error.size")
        } else if (!hasAcceptedExtension(file.filename)) {
          form.withError(fileErrorKey, "supportingDocuments.uploadFile.error.type")
        } else {
          form
        }
      )
    } catch {
      case error: IOException => Left(error)
    }

  private def hasAcceptedExtension(filename: String): Boolean =
    filename.lastIndexOf('.') match {
      case dotIndex if dotIndex >= 0 =>
        acceptedExtensions.contains(filename.substring(dotIndex + 1).toLowerCase(Locale.ROOT))
      case _ =>
        false
    }
}
