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

import controllers.actions.*
import forms.imports.ImportDateFormProvider
import models.requests.DataRequest
import pages.{ImportDatePage, SadReferencePage}
import navigation.Navigator

import javax.inject.Inject
import models.{CheckMode, Mode, NormalMode}
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.*
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.imports.ImportDateView

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ImportDateController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: ImportDateFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: ImportDateView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  private def form(implicit messages: Messages) = formProvider()
  private def backLink(mode: Mode)(implicit request: DataRequest[?]): Call = mode match {
    case CheckMode  => controllers.routes.JourneyRecoveryController.onPageLoad() // TODO: replace with CheckYourImportController once built
    case NormalMode => navigator.nextPage(SadReferencePage, mode, request.userAnswers)
  }

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
    val preparedForm = request.userAnswers.get(ImportDatePage).fold(form)(form.fill)
    Ok(view(preparedForm, mode, backLink(mode)))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    form
      .bindFromRequest()
      .fold(
        formWithErrors => badRequestToImportDate(formWithErrors, mode),
        value =>
          val today = java.time.LocalDate.now()
          if (value.isAfter(today)) {
            val errorForm = form.bindFromRequest().withError("value", "importDate.error.past")
            badRequestToImportDate(errorForm, mode)
          } else {
            handleSubmission(value, mode)(request)
          }
      )
  }

  private def badRequestToImportDate(formWithErrors: Form[?], mode: Mode)(implicit
    request: DataRequest[AnyContent]
  ): Future[play.api.mvc.Result] = {
    val html = view(formWithErrors, mode, backLink(mode))
    Future.successful(BadRequest(html))
  }

  private def handleSubmission(value: LocalDate, mode: Mode)(implicit request: DataRequest[?]): Future[Result] = {
    if (mode == CheckMode && request.userAnswers.isAnswerUnchanged(ImportDatePage, value)) {
      Future.successful(
        Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
      ) // TODO: replace with CheckYourImportController once built
    } else {
      for {
        updatedAnswers <- Future.fromTry(request.userAnswers.set(ImportDatePage, value))
        _              <- sessionRepository.set(updatedAnswers)
      } yield {
        if (mode == CheckMode) {
          Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()) // TODO: replace with CheckYourImportController once built
        } else {
          Redirect(navigator.nextPage(ImportDatePage, mode, updatedAnswers))
        }
      }
    }
  }
}
