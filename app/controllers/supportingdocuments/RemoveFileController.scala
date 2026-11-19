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
import forms.RemoveFileFormProvider
import models.NormalMode
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

  // todo wire it after rebase 
//  private val uploadedFilesPage = controllers.supportingdocuments.routes.UploadedFilesController.onPageLoad()
  private val uploadedFilesPage = controllers.routes.JourneyRecoveryController.onPageLoad()
  private val journeyRecovery = controllers.routes.JourneyRecoveryController.onPageLoad()

  def onPageLoad(attachmentNumber: Int): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>

    request.userAnswers
      .get(ClaimApplicationResponseQuery)
      .map(_.applicationId)
      .fold(recoveryPage("Missing or invalid applicationId")) { applicationId =>
        service
          .getAttachmentDetails(applicationId, attachmentNumber)
          .flatMap { response =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(UpdateSequenceNumberQuery, response.updateSequenceNumber))
              _ <- sessionRepository.set(updatedAnswers)
            } yield Ok(view(form, response))
          }
          .recoverWith { case e: Exception =>
            logger.error(s"Failed to retrieve attachments for applicationId: $applicationId", e)
            recoveryPage("Get all attachments failed")
          }

        //        renderPage(form, attachmentNumber, badRequest = false)
      }

  }

  def onSubmit(attachmentNumber: Int): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
//    form
//      .bindFromRequest()
//      .fold(
//        formWithErrors => renderPage(formWithErrors, attachmentNumber, badRequest = true),
//        value =>
//          if (!value) {
//            Future.successful(Redirect(uploadedFilesPage))
//          } else {
//            deleteAttachment(attachmentNumber)
//          }
//      )
    recoveryPage("Get all attachments failed")
  }

  private def recoveryPage(msg: String): Future[Result] = {
    logger.warn(msg)
    Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
  }

//  private def renderPage(formToRender: Form[Boolean], attachmentNumber: Int, badRequest: Boolean)(implicit
//    request: DataRequest[AnyContent]
//  ): Future[Result] =
//    getApplicationId match {
//      case None =>
//        logger.error("Application id missing from user answers")
//        Future.successful(Redirect(journeyRecovery))
//      case Some(applicationId) =>
//        euvatRefundsService
//          .getAttachmentDetails(applicationId, attachmentNumber)
//          .map(attachment)
//    }
  
  
}
