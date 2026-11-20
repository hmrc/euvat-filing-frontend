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
import forms.supportingdocuments.YourFileFormProvider
import navigation.SupportingDocumentsNavigator
import models.YourFile
import pages.supportingdocuments.YourFilePage
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
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
  formProvider: YourFileFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: YourFileView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    request.userAnswers.get(YourFilePage) match {
      case Some(yourFile) =>
        Future.successful(Ok(view(yourFile, formProvider().fill(yourFile.description))))

      case None =>
        // TODO: F6.1/F6.4 - use the real filename from the backend upload summary; the upscan reference is a placeholder
        request.getQueryString("key") match {
          case Some(reference) =>
            val yourFile = YourFile(filename = reference, description = None)
            Future
              .fromTry(request.userAnswers.set(YourFilePage, yourFile))
              .flatMap(sessionRepository.set)
              .map(_ => Ok(view(yourFile, formProvider())))
          case None =>
            Future.successful(Redirect(routes.UploadFileController.onPageLoad()))
        }
    }
  }

  def onRemove(): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    // TODO: call to delete Upload Tracking before clearing the UI-only tracking record.
    clearUpload(request.userAnswers).map(_ => Redirect(routes.UploadFileController.onPageLoad()))
  }

  def onSubmit(): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    request.userAnswers.get(YourFilePage) match {
      case None => Future.successful(Redirect(routes.UploadFileController.onPageLoad()))
      case Some(yourFile) =>
        val boundForm = formProvider().bindFromRequest()
        if (boundForm.hasErrors) {
          Future.successful(BadRequest(view(yourFile, boundForm)))
        } else {
          val updatedAnswers =
            request.userAnswers.set(YourFilePage, yourFile.copy(description = boundForm.value.flatten))
          Future
            .fromTry(updatedAnswers)
            .flatMap(sessionRepository.set)
            .map(_ => Redirect(navigator.navigateFromYourFilePage()))
        }
    }
  }

  private def clearUpload(userAnswers: models.UserAnswers): Future[Unit] =
    Future.fromTry(userAnswers.remove(YourFilePage)).flatMap(sessionRepository.set).map(_ => ())
}
