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
import controllers.purchase.routes
import models.{Mode, NormalMode}
import pages.*
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.warning.SupplierTaxIdentifierWarningView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SupplierTaxIdentifierWarningController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: repositories.SessionRepository,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  val controllerComponents: MessagesControllerComponents,
  view: SupplierTaxIdentifierWarningView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    for {
      answers <- Future.fromTry(request.userAnswers.set(SupplierTaxIdentifierWarningPage, true))
      _       <- sessionRepository.set(answers)
    } yield {
      Ok(view(routes.SupplierTaxIdentifierNumberController.onPageLoad(mode), routes.InvoiceNumberController.onPageLoad(mode)))
    }
  }

  def onSubmit(): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    val cleared = request.userAnswers.set(SupplierTaxIdentifierWarningPage, true)
    Future
      .fromTry(cleared)
      .flatMap(ua =>
        sessionRepository
          .set(ua)
          .map(_ =>
            if (request.userAnswers.get(TotalPurchaseAmountBeforeVatPage).isDefined) {
              Redirect(routes.CheckYourPurchaseDetailsController.onPageLoad())
            } else {
              Redirect(routes.TotalPurchaseAmountBeforeVatController.onPageLoad(NormalMode))
            }
          )
      )
  }

}
