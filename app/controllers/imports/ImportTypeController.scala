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

package controllers.imports

import com.google.inject.Inject
import controllers.actions.{DataRequiredAction, DataRetrievalAction, IdentifierAction}
import forms.ImportTypeFormProvider
import models.requests.{AddImportRequest, DataRequest}
import models.{Mode, PurchaseOrImportType, UserAnswers}
import navigation.Navigator
import pages.{AddImportResponsePage, ImportTypePage}
import play.api.Logging
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import queries.ClaimApplicationResponseQuery
import repositories.SessionRepository
import services.EuVatRefundsService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import views.html.PurchaseOrImportTypeView

import scala.concurrent.{ExecutionContext, Future}

class ImportTypeController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: ImportTypeFormProvider,
  val controllerComponents: MessagesControllerComponents,
  euVatRefundsService: EuVatRefundsService,
  view: PurchaseOrImportTypeView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  val form: Form[PurchaseOrImportType] = formProvider()

  private def backLink(mode: Mode)(implicit request: DataRequest[?]) = controllers.routes.PurchaseOrImportController.onPageLoad

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
    val preparedForm = request.userAnswers.get(ImportTypePage) match {
      case None        => form
      case Some(value) => form.fill(value)
    }

    Ok(view(preparedForm, mode, backLink(mode), routes.ImportTypeController.onSubmit(mode), "importType", "import.caption", false, legendKey = None))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    form
      .bindFromRequest()
      .fold(
        formWithErrors =>
          Future.successful(
            BadRequest(
              view(formWithErrors,
                   mode,
                   backLink(mode),
                   routes.ImportTypeController.onSubmit(mode),
                   "importType",
                   "import.caption",
                   false,
                   legendKey = None
                  )
            )
          ),
        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(ImportTypePage, value))
            _              <- sessionRepository.set(updatedAnswers)
            result         <- addImportIfRequired(updatedAnswers, value, mode)
          } yield result
      )
  }

  private def addImportIfRequired(answers: UserAnswers, importType: PurchaseOrImportType, mode: Mode)(implicit
    request: DataRequest[?]
  ): Future[Result] =
    if (answers.get(AddImportResponsePage).isEmpty && answers.get(ClaimApplicationResponseQuery).isDefined) {
      addImportAndPersist(answers, importType, mode)
    } else {
      Future.successful(Redirect(navigator.nextPage(ImportTypePage, mode, answers)))
    }

  private def addImportAndPersist(answers: UserAnswers, importType: PurchaseOrImportType, mode: Mode)(implicit
    request: DataRequest[?]
  ): Future[Result] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
    answers
      .get(ClaimApplicationResponseQuery)
      .fold {
        logger.warn("Missing applicationId for addImport")
        Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
      } { claimResponse =>
        val importRequest = AddImportRequest(
          applicationId            = claimResponse.applicationId,
          goodsDescriptionCategory = PurchaseOrImportType.codes(importType),
          updateSequenceNumber     = claimResponse.updateSeqNumber
        )

        euVatRefundsService
          .addImport(importRequest)
          .flatMap { response =>
            for {
              updatedAnswers <- Future.fromTry(answers.set(AddImportResponsePage, response))
              _              <- sessionRepository.set(updatedAnswers)
            } yield Redirect(navigator.nextPage(ImportTypePage, mode, updatedAnswers))
          }
          .recover { case ex =>
            logger.error("Error while adding the import", ex)
            Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
          }
      }
  }
}
