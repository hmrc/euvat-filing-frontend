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
import controllers.claim.routes
import models.{Mode, RefundPeriod}
import navigation.Navigator
import pages.claim.RefundPeriodPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.warning.RefundPeriodEndDateWarningView

import java.time.format.DateTimeFormatter
import javax.inject.Inject

class RefundPeriodEndDateWarningController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  val controllerComponents: MessagesControllerComponents,
  navigator: Navigator,
  view: RefundPeriodEndDateWarningView
) extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
    request.userAnswers.get(RefundPeriodPage) match {
      case None => Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
      case Some(refundPeriod) =>
        val endDate = refundPeriod.endDate.format(DateTimeFormatter.ofPattern("MM/yyyy"))
        val call = routes.RefundPeriodController.onPageLoad(mode)
        Ok(view(endDate, call, mode))
    }
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
    Redirect(navigator.nextPage(RefundPeriodPage, mode, request.userAnswers))
  }
}
