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

package controllers

import com.google.inject.Inject
import controllers.actions.{DataRequiredAction, DataRetrievalAction, IdentifierAction}
import forms.ImportTypeFormProvider
import models.requests.DataRequest
import models.{Mode, PurchaseAndImportType}
import navigation.Navigator
import pages.ImportTypePage
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.PurchaseAndImportTypeView

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
  view: PurchaseAndImportTypeView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  val form: Form[PurchaseAndImportType] = formProvider()

  private def backLink(mode: Mode)(implicit request: DataRequest[?]) = routes.PurchaseOrImportController.onPageLoad

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
    val preparedForm = request.userAnswers.get(ImportTypePage) match {
      case None        => form
      case Some(value) => form.fill(value)
    }

    Ok(view(preparedForm, mode, backLink(mode), routes.ImportTypeController.onSubmit(mode), "importType", "import.caption", showIntro = false))
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
                   showIntro = false
                  )
            )
          ),
        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(ImportTypePage, value))
            _              <- sessionRepository.set(updatedAnswers)
          } yield Redirect(navigator.nextPage(ImportTypePage, mode, updatedAnswers))
      )
  }
}
