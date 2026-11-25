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
import forms.supportingdocuments.UploadFileFormProvider.fileErrorKey
import models.UploadFileError
import play.api.Logging
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.*
import queries.ClaimApplicationResponseQuery
import services.EuVatRefundsService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import views.html.supportingdocuments.UploadFileView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class UploadFileController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: UploadFileFormProvider,
  euVatRefundsService: EuVatRefundsService,
  val controllerComponents: MessagesControllerComponents,
  view: UploadFileView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  private def form: Form[Option[String]] = formProvider()

  // TODO: check for a reusable upload first (F6.1 - Get Upload Summary) once it is built.
  def onPageLoad(): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    request.userAnswers.get(ClaimApplicationResponseQuery).map(_.applicationId) match {
      case None =>
        logger.warn("Missing applicationId when starting a supporting document upload")
        Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))

      case Some(applicationId) =>
        val preparedForm = request
          .getQueryString("errorCode")
          .map(UploadFileError.fromUpscanErrorCode)
          .fold(form)(error => form.withError(fileErrorKey, error.messageKey))

        euVatRefundsService
          .createUploadTracking(applicationId)
          .map { uploadTracking =>
            Ok(view(preparedForm, uploadTracking.uploadUrl, uploadTracking.fields))
              .withHeaders("Cache-Control" -> "no-store")
          }
          .recover { case ex =>
            logger.error(s"Could not start a supporting document upload for application $applicationId", ex)
            Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
          }
    }
  }
}
