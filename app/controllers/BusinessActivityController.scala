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

import controllers.actions.*
import forms.BusinessActivityFormProvider
import models.Mode
import navigation.Navigator
import pages.{BusinessActivityCodePage, BusinessActivityCodeThreePage, BusinessActivityCodeTwoPage, BusinessActivityPage, BusinessActivityTwoPage}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import repositories.SessionRepository
import services.EuVatRefundsService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.BusinessActivityView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class BusinessActivityController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: BusinessActivityFormProvider,
  euVatRefundsService: EuVatRefundsService,
  val controllerComponents: MessagesControllerComponents,
  view: BusinessActivityView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  val form: Form[Boolean] = formProvider()

  private def backLink(mode: Mode): Call = routes.ContactDetailsController.onPageLoad(mode)

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    euVatRefundsService.retrieveTraderKnownFacts().map { traderResponse =>
      val preparedForm = request.userAnswers.get(BusinessActivityPage).fold(form)(form.fill)
      Ok(view(preparedForm, mode, backLink(mode), traderResponse.tradeClass.getOrElse("")))
    }
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    euVatRefundsService.retrieveTraderKnownFacts().flatMap { traderResponse =>
      val baCode = traderResponse.tradeClass.getOrElse("")
      form
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(view(formWithErrors, mode, backLink(mode), baCode))),
          value =>
            for {
              updateAnswer1 <- Future.fromTry(request.userAnswers.set(BusinessActivityCodePage, baCode))
              updateAnswer2 <- Future.fromTry(updateAnswer1.set(BusinessActivityPage, value))
              finalAnswers <- if (value) {
                                  Future.successful(updateAnswer1)
                                } else {
                                  val remove1 = updateAnswer1.remove(BusinessActivityCodeTwoPage)
                                  Future.fromTry(remove1.flatMap(_.remove(BusinessActivityCodeThreePage)))
                                }
              _ <- sessionRepository.set(finalAnswers)
            } yield Redirect(navigator.nextPage(BusinessActivityPage, mode, finalAnswers))
        )
    }
  }
}
