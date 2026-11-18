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
import forms.supportingdocuments.UploadFileFormProvider.{descriptionFieldName, descriptionMaxLength, fileErrorKey, fileInputName}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.Logging
import play.api.libs.Files.TemporaryFile
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, MultipartFormData}
import play.api.mvc.MultipartFormData.FilePart
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.supportingdocuments.UploadFileView

import java.io.IOException
import java.nio.file.Files
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class UploadFileController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
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
  private val fileErrorFlashKey = "uploadFile.fileError"
  private val descriptionErrorFlashKey = "uploadFile.descriptionError"
  private val descriptionFlashKey = "uploadFile.description"
  private val formTokenKey = "uploadFile.formToken"
  private val formTokenField = "uploadFileFormToken"

  private def form: Form[Option[String]] = formProvider()

  def onPageLoad(): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
    val formToken = UUID.randomUUID().toString
    val preparedForm = request.flash
      .get(descriptionFlashKey)
      .fold(form)(description => form.fill(Some(description)))
    val formWithErrors = request.flash
      .get(fileErrorFlashKey)
      .fold(preparedForm)(error => preparedForm.withError(fileErrorKey, error))
    val formWithAllErrors = request.flash
      .get(descriptionErrorFlashKey)
      .fold(formWithErrors)(error => formWithErrors.withError(descriptionFieldName, error, descriptionMaxLength))

    Ok(view(formWithAllErrors, formToken))
      .addingToSession(formTokenKey -> formToken)
      .withHeaders("Cache-Control" -> "no-store")
  }

  def onSubmit(): Action[MultipartFormData[TemporaryFile]] =
    (identify andThen getData andThen requireData)
      .async(parse.multipartFormData(maxLength = maxRequestSize, allowEmptyFiles = true)) { implicit request =>
        val boundForm = form.bind(request.body.dataParts.view.mapValues(_.headOption.getOrElse("")).toMap)
        val validFormToken = request.session
          .get(formTokenKey)
          .exists(token => request.body.dataParts.get(formTokenField).flatMap(_.headOption).contains(token))
        val formWithFileErrors =
          if (!validFormToken) {
            boundForm.withError(fileErrorKey, "supportingDocuments.uploadFile.error.required")
          } else {
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
          }

        if (formWithFileErrors.hasErrors) {
          val flashValues =
            formWithFileErrors.error(fileErrorKey).map(error => fileErrorFlashKey -> error.message).toSeq ++
              formWithFileErrors
                .error(descriptionFieldName)
                .map(error => descriptionErrorFlashKey -> error.message)
                .toSeq ++
              formWithFileErrors(descriptionFieldName).value.filter(_.length <= descriptionMaxLength).map(descriptionFlashKey -> _).toSeq

          Future.successful(
            Redirect(controllers.supportingdocuments.routes.UploadFileController.onPageLoad())
              .flashing(flashValues*)
              .withHeaders("Cache-Control" -> "no-store")
          )
        } else {
          // TODO: redirect to the RA6.2 uploading page when it is implemented.
          Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
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
