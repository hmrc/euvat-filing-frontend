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
import forms.supportingdocuments.AnyDocumentsFormProvider
import models.NormalMode
import navigation.Navigator
import pages.supportingdocuments.AnyDocumentsPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.supportingdocuments.AnyDocumentsView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AnyDocumentsController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: AnyDocumentsFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: AnyDocumentsView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  private val form = formProvider()

  def onPageLoad(): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
    val preparedForm = request.userAnswers.get(AnyDocumentsPage).fold(form)(form.fill)
    Ok(view(preparedForm))
  }

  def onSubmit(): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    form
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors))),
        hasDocuments =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(AnyDocumentsPage, hasDocuments))
            _              <- sessionRepository.set(updatedAnswers)
          } yield Redirect(navigator.nextPage(AnyDocumentsPage, NormalMode, updatedAnswers))
      )
  }
}
