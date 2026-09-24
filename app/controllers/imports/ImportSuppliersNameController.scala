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
import forms.SuppliersNameFormProvider
import models.{Mode, NormalMode}
import navigation.Navigator
import pages.ImportSuppliersNamePage
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.PurchaseOrImportSuppliersNameView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ImportSuppliersNameController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: SuppliersNameFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: PurchaseOrImportSuppliersNameView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  val form: Form[String] = formProvider()

  private def submitCall(mode: Mode): Call = controllers.imports.routes.ImportSuppliersNameController.onSubmit(mode)

  private def backLink: Call = controllers.imports.routes.SadReferenceController.onPageLoad

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
    val preparedForm = request.userAnswers.get(ImportSuppliersNamePage).fold(form)(form.fill)
    Ok(view(preparedForm, submitCall(mode), backLink, "import.caption", "suppliersName.import.hint"))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    form
      .bindFromRequest()
      .fold(
        formWithErrors =>
          Future.successful(BadRequest(view(formWithErrors, submitCall(mode), backLink, "import.caption", "suppliersName.import.hint"))),
        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(ImportSuppliersNamePage, value))
            _              <- sessionRepository.set(updatedAnswers)
          } yield Redirect(navigator.nextPage(ImportSuppliersNamePage, mode, updatedAnswers))
      )
  }
}
