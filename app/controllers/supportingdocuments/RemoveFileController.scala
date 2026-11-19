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
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.*
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.RemoveFileView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class RemoveFileController @Inject()(
                                         override val messagesApi: MessagesApi,
                                         sessionRepository: SessionRepository,
                                         navigator: Navigator,
                                         identify: IdentifierAction,
                                         getData: DataRetrievalAction,
                                         requireData: DataRequiredAction,
                                         formProvider: RemoveFileFormProvider,
                                         val controllerComponents: MessagesControllerComponents,
                                         view: RemoveFileView
                                 )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  val form: Form[Boolean] = formProvider()
  
  private val uploadedFilesPage = controllers.supportingdocuments.routes.UploadedFilesController.onPageLoad()
  private val journeyRecovery = controllers.routes.JourneyRecoveryController.onPageLoad()

  def onPageLoad(attachmentNumber: Int): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      renderPage(form, attachmentNumber, badRequest = false)
  }

  def onSubmit(attachmentNumber: Int): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      form.bindFromRequest().fold(
        formWithErrors => renderPage(formWithErrors, attachmentNumber, badRequest = true),
        value =>
          if (!value) {
            Future.successful(Redirect(uploadedFilesPage))
          } else {
          deleteAttachment(attachmentNumber)
        }
      )
  }
  
  private def renderPage(formToRender: Form[Boolean], attachmentNumber: Int, badRequest: Boolean)(implicit 
    request: DataRequest[AnyContent]
  ): Future[Result] =
    getApplicationId match {
      case None => 
        logger.error("Application id missing from user answers")
        Future.successful(Redirect(journeyRecovery))
      case Some(applicationId) => 
        euvatRefundsService
          .getAttachmentDetails(applicationId, attachmentNumber)
          .map { attachment
            
          }
    }  
}
