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
import navigation.SupportingDocumentsNavigator
import pages.supportingdocuments.YourFileNamePage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.UploadedFileStatusQuery
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.supportingdocuments.YourFileView

import javax.inject.Inject
import repositories.SessionRepository
import scala.concurrent.{ExecutionContext, Future}

class YourFileController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  navigator: SupportingDocumentsNavigator,
  sessionRepository: SessionRepository,
  val controllerComponents: MessagesControllerComponents,
  view: YourFileView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  import YourFileController.*

  def onPageLoad(): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    request.userAnswers.get(YourFileNamePage) match {
      case None => Future.successful(Redirect(routes.UploadFileController.onPageLoad()))
      case Some(filename) =>
        val status = request.getQueryString("status").orElse(request.userAnswers.get(UploadedFileStatusQuery)).getOrElse(AwaitingUpload)
        status match {
          case VerificationFailed =>
            clearUpload(request.userAnswers).map { _ =>
              Redirect(routes.UploadFileController.onPageLoad())
                .flashing("uploadError" -> "supportingDocuments.uploadFile.error.verificationFailed")
            }
          case ValidationFailed =>
            clearUpload(request.userAnswers).map { _ =>
              Redirect(routes.UploadFileController.onPageLoad())
                .flashing("uploadError" -> "supportingDocuments.uploadFile.error.validationFailed")
            }
          case AwaitingUpload | Verifying | Validating | Validated =>
            val response = Ok(view(filename, status))
            Future.successful(if (isUploading(status)) response.withHeaders("Refresh" -> "5") else response)
          case _ =>
            clearUpload(request.userAnswers).map { _ =>
              Redirect(routes.UploadFileController.onPageLoad())
                .flashing("uploadError" -> "supportingDocuments.uploadFile.error.uploadFailed")
            }
        }
    }
  }

  def onRemove(): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    // TODO: call to delete Upload Tracking before clearing the UI-only tracking record.
    clearUpload(request.userAnswers).map(_ => Redirect(routes.UploadFileController.onPageLoad()))
  }

  def onSubmit(): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
    Redirect(navigator.navigateFromYourFilePage(request.userAnswers))
  }

  private def clearUpload(userAnswers: models.UserAnswers): Future[Boolean] =
    Future
      .fromTry(userAnswers.remove(YourFileNamePage).flatMap(_.remove(UploadedFileStatusQuery)))
      .flatMap(sessionRepository.set)
}

object YourFileController {
  val AwaitingUpload = "AWAITING_UPLOAD"
  val Verifying = "VERIFYING"
  val Validating = "VALIDATING"
  val Validated = "VALIDATED"
  val VerificationFailed = "VERIFICATION_FAILED"
  val ValidationFailed = "VALIDATION_FAILED"

  private def isUploading(status: String): Boolean =
    status == AwaitingUpload || status == Verifying || status == Validating
}
