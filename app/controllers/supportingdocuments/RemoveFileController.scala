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
import forms.supportingdocuments.RemoveFileFormProvider
import models.NormalMode
import models.requests.DeleteAttachmentRequest
import navigation.Navigator
import pages.RemoveFilePage
import play.api.Logging
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.*
import queries.{ClaimApplicationResponseQuery, UpdateSequenceNumberQuery}
import repositories.SessionRepository
import services.EuVatRefundsService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.supportingdocuments.RemoveFileView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class RemoveFileController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: RemoveFileFormProvider,
  service: EuVatRefundsService,
  val controllerComponents: MessagesControllerComponents,
  view: RemoveFileView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with Logging
    with I18nSupport {

  val form: Form[Boolean] = formProvider()

  def onPageLoad(attachmentNumber: Int): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>

    val ua = request.userAnswers

    ua
      .get(ClaimApplicationResponseQuery)
      .map(_.applicationId)
      .fold(recoveryPage("Missing or invalid applicationId")) { applicationId =>
        service
          .getAttachmentDetails(applicationId, attachmentNumber, ua)
          .map(response => Ok(view(form, response)))
          .recoverWith { case e: Exception =>
            logger.error(s"Failed to retrieve attachment for applicationId: $applicationId,attachmentNumber: $attachmentNumber", e)
            recoveryPage("Get attachment failed")
          }
      }

  }

  def onSubmit(attachmentNumber: Int): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>

    val ua = request.userAnswers

    (ua.get(ClaimApplicationResponseQuery).map(_.applicationId), ua.get(UpdateSequenceNumberQuery)) match {
      case (Some(applicationId), Some(updateSequenceNumber)) =>
        form
          .bindFromRequest()
          .fold(
            formWithErrors =>
              service
                .getAttachmentDetails(applicationId, attachmentNumber, ua)
                .map(response => BadRequest(view(formWithErrors, response)))
                .recoverWith { case e: Exception =>
                  logger.error(s"Failed to retrieve attachment for applicationId: $applicationId, attachmentNumber: $attachmentNumber", e)
                  recoveryPage("Get attachment failed")
                },
            value =>
              if (!value) {
                Future.successful(Redirect(navigator.nextPage(RemoveFilePage, NormalMode, request.userAnswers)))
              } else {
                service
                  .deleteAttachment(DeleteAttachmentRequest(applicationId, attachmentNumber, updateSequenceNumber), ua)
                  .map(_ => Redirect(navigator.nextPage(RemoveFilePage, NormalMode, request.userAnswers)))
                  .recoverWith { case e: Exception =>
                    logger.error(s"Failed to delete attachments for applicationId: $applicationId, attachmentNumber: $attachmentNumber", e)
                    recoveryPage("Delete attachments failed")
                  }
              }
          )

      case _ => recoveryPage("Missing or invalid applicationId, updateSequenceNumber")
    }
  }

  private def recoveryPage(msg: String): Future[Result] = {
    logger.warn(msg)
    Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
  }

}
