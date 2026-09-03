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
import forms.purchase.SuppliersNameFormProvider
import models.requests.DataRequest
import models.{CheckMode, Mode}
import navigation.Navigator
import pages.SuppliersNamePage
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.CheckModeShortCircuit
import utils.ControllerHelpers.*
import views.html.purchase.SuppliersNameView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SuppliersNameController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: SuppliersNameFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: SuppliersNameView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  val form: Form[String] = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
    val preparedForm = preparedFormFromAnswers(_.get(SuppliersNamePage), form)
    renderOk(preparedForm, mode)
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    form
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(renderBadRequest(formWithErrors, mode)),
        value =>
          val inPurchaseFlow = request.userAnswers.get(pages.PurchaseTypePage).isDefined

          if (inPurchaseFlow) {
            CheckModeShortCircuit.shortCircuitIfUnchanged(
              pages.SuppliersNamePage,
              value,
              mode,
              request.userAnswers,
              routes.CheckYourPurchaseDetailsController.onPageLoad()
            ) match {
              case Some(res) => Future.successful(res)
              case None =>
                val userAnswersTry = request.userAnswers.set(SuppliersNamePage, value)
                if (mode == CheckMode)
                  persistAndThen(userAnswersTry, sessionRepository)(_ =>
                    Future.successful(Redirect(routes.CheckYourPurchaseDetailsController.onPageLoad()))
                  )
                else
                  persistAndThen(userAnswersTry, sessionRepository)(ua =>
                    Future.successful(Redirect(navigator.nextPage(SuppliersNamePage, mode, ua)))
                  )
            }
          } else {
            val userAnswersTry = request.userAnswers.set(SuppliersNamePage, value)
            persistAndThen(userAnswersTry, sessionRepository)(ua => Future.successful(Redirect(navigator.nextPage(SuppliersNamePage, mode, ua))))
          }
      )
  }

  private def renderOk(preparedForm: Form[String], mode: Mode)(implicit request: DataRequest[?]) = Ok(
    view(preparedForm, mode, routes.InvoiceDateController.onPageLoad(mode))
  )

  private def renderBadRequest(formWithErrors: Form[String], mode: Mode)(implicit request: DataRequest[?]) = BadRequest(
    view(formWithErrors, mode, routes.InvoiceDateController.onPageLoad(mode))
  )
}
