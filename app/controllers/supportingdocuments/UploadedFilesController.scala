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
import models.requests.{DataRequest, GetAllAttachmentsRequest}
import navigation.SupportingDocumentsNavigator
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import queries.{ClaimApplicationResponseQuery, SupportingDocumentsCompletedQuery, UploadedFilesQuery}
import repositories.SessionRepository
import services.{EuVatRefundsService, PaginationService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.supportingdocuments.UploadedFilesView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class UploadedFilesController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  navigator: SupportingDocumentsNavigator,
  sessionRepository: SessionRepository,
  service: EuVatRefundsService,
  paginationService: PaginationService,
  val controllerComponents: MessagesControllerComponents,
  view: UploadedFilesView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with Logging
    with I18nSupport {

  def onPageLoad(page: Int): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    request.userAnswers.get(ClaimApplicationResponseQuery).map(_.applicationId) match {
      case None                => recoveryPage("Missing or invalid applicationId")
      case Some(_) if page < 1 => Future.successful(Redirect(routes.UploadedFilesController.onPageLoad()))
      case Some(applicationId) => retrieveAttachments(applicationId, page)
    }
  }

  def onSubmit(): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    for {
      updatedAnswers <- Future.fromTry(request.userAnswers.set(SupportingDocumentsCompletedQuery, true))
      _              <- sessionRepository.set(updatedAnswers)
    } yield Redirect(navigator.navigateFromUploadedFilesPage())
  }

  private def retrieveAttachments(applicationId: Long, page: Int)(implicit request: DataRequest[AnyContent]): Future[Result] =
    service
      .getAllAttachments(
        GetAllAttachmentsRequest(
          applicationId = applicationId,
          orderBy       = Some(0),
          sortOrder     = Some("ASC"),
          startAt       = Some(paginationService.startAt(page)),
          maxNumber     = Some(paginationService.recordsPerPage)
        )
      )
      .flatMap { response =>
        val totalPages = paginationService.totalPages(response.totalAttachments)
        if (page > totalPages) {
          Future.successful(Redirect(routes.UploadedFilesController.onPageLoad(totalPages)))
        } else {
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(UploadedFilesQuery, response.attachments))
            _              <- sessionRepository.set(updatedAnswers)
          } yield Ok(
            view(
              attachments         = response.attachments,
              totalAttachments    = response.totalAttachments,
              paginationViewModel = paginationService.paginate(response.totalAttachments, page, routes.UploadedFilesController.onPageLoad().url),
              currentPage         = page,
              totalPages          = totalPages,
              firstRecord         = paginationService.startAt(page) + 1
            )
          )
        }
      }
      .recoverWith { case e: Exception =>
        logger.error(s"Failed to retrieve attachments for applicationId: $applicationId", e)
        recoveryPage("Get all attachments failed")
      }

  private def recoveryPage(msg: String): Future[Result] = {
    logger.warn(msg)
    Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
  }
}
