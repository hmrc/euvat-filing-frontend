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
import forms.imports.SadReferenceCheckFormProvider
import models.NormalMode
import pages.SadReferenceCheckPage
import models.requests.DataRequest
import models.{Mode, NormalMode, Other}
import navigation.Navigator
import pages.SadReferenceCheckPage
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.imports.SadReferenceCheckView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SadReferenceCheckController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: SadReferenceCheckFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: SadReferenceCheckView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  val form: Form[Boolean] = formProvider()
  private def backLink(mode: Mode): Call = routes.ImportSubCategoryController.onPageLoad(mode)

  private def computeBackLink(implicit request: DataRequest[AnyContent]): Call = {
    val answers = request.userAnswers
    (answers.get(pages.ImportTypePage), answers.get(pages.ImportSubCodePage), answers.get(pages.ImportSubCategoryPage)) match {
      case (Some(_), _, Some(_)) => controllers.imports.routes.ImportSubCategoryController.onPageLoad(NormalMode)
      case (Some(Other), _, _) if answers.get(pages.DescribeItemsOnImportDocPage).isDefined =>
        controllers.imports.routes.DescribeItemsOnImportDocController.onPageLoad(NormalMode)
      case (Some(importType), Some(_), None) => controllers.imports.routes.ImportSubCodeController.onPageLoad(importType.toString)
      case _                                 => controllers.imports.routes.ImportTypeController.onPageLoad(NormalMode)
    }
  }

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
    val preparedForm = request.userAnswers.get(SadReferenceCheckPage).fold(form)(form.fill)
    Ok(view(preparedForm, computeBackLink))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    form
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, computeBackLink))),
        value =>
          for {
            updated <- Future.fromTry(request.userAnswers.set(SadReferenceCheckPage, value))
            _       <- sessionRepository.set(updated)
          } yield Redirect(navigator.nextPage(SadReferenceCheckPage, mode, updated))
      )
  }

}
