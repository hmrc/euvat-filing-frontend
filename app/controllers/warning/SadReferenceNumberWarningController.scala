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

package controllers.warning

import controllers.actions.*
import pages.imports.SadReferenceNumberPage
import controllers.imports.routes
import models.{CheckMode, Mode, NormalMode}
import navigation.Navigator
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.warning.SadReferenceNumberWarningView

import javax.inject.Inject

class SadReferenceNumberWarningController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  val controllerComponents: MessagesControllerComponents,
  navigator: Navigator,
  view: SadReferenceNumberWarningView
) extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
    Ok(view(routes.SadReferenceNumberController.onPageLoad(mode), mode))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
    mode match {
      case NormalMode => Redirect(navigator.nextPage(SadReferenceNumberPage, NormalMode, request.userAnswers))
      //TODO replace to RA5.10.1 - Check your answers (import details)
//      case CheckMode => Redirect(routes.CheckYourPurchaseDetailsController.onPageLoad())
      case CheckMode => Redirect(routes.SadReferenceNumberController.onPageLoad(mode))
    }
  }
}
