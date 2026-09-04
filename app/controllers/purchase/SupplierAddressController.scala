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

package controllers.purchase

import controllers.actions.*
import forms.purchase.SupplierAddressFormProvider
import models.requests.DataRequest
import models.{CheckMode, Mode, NormalMode, SupplierAddress, UserAnswers}
import navigation.Navigator
import pages.{PurchaseTypePage, SupplierAddressPage}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.ControllerHelpers
import views.html.purchase.SupplierAddressView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success

class SupplierAddressController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: SupplierAddressFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: SupplierAddressView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  val form: Form[SupplierAddress] = formProvider()

  private def backLink: Call = routes.SuppliersNameController.onPageLoad(NormalMode)

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
    val preparedForm = request.userAnswers.get(SupplierAddressPage).fold(form)(form.fill)
    Ok(view(preparedForm, mode, backLink))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    form
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, mode, backLink))),
        value =>
          ControllerHelpers.shortCircuit(
            SupplierAddressPage,
            value,
            mode,
            request.userAnswers,
            navigator.nextPage(SupplierAddressPage, mode, request.userAnswers),
            routes.CheckYourPurchaseDetailsController.onPageLoad(),
            None
          ) { (answersAfterSet: UserAnswers) =>
            val userAnswersTry = Success(answersAfterSet)
            val redirectCall = computeRedirectAfterSave(answersAfterSet, mode)

            ControllerHelpers.saveTryAndRedirect(userAnswersTry, sessionRepository, redirectCall)
          }
      )
  }

  private def computeRedirectAfterSave(answersAfterSet: UserAnswers, mode: Mode)(implicit request: DataRequest[?]) =
    if (mode == CheckMode && request.userAnswers.get(PurchaseTypePage).isDefined) {
      routes.CheckYourPurchaseDetailsController.onPageLoad()
    } else {
      navigator.nextPage(SupplierAddressPage, mode, answersAfterSet)
    }
}
