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
import forms.RemoveBusinessActivityFormProvider
import models.{Mode, NormalMode, CheckMode}
import navigation.Navigator
import pages._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.RemoveBusinessActivityView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class RemoveThirdBusinessActivityController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: RemoveBusinessActivityFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: RemoveBusinessActivityView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  private def headingKey = "removeThird.heading"

  private def backLinkFrom(origin: Option[String], mode: Mode): Call = origin match {
    case Some("business-activity-3") => routes.BusinessActivityThreeController.onPageLoad()
    case Some("business-activity-2") => routes.BusinessActivityTwoController.onPageLoad(mode)
    case _                              => routes.BusinessActivityThreeController.onPageLoad()
  }

  private def parseOrigin(implicit request: play.api.mvc.Request[_]): Option[String] = request.getQueryString("origin")

  private def parseMode(implicit request: play.api.mvc.Request[_]): Mode = request.getQueryString("mode") match {
    case Some("check") => CheckMode
    case _              => NormalMode
  }

  def onPageLoad(): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
      val form = formProvider()
      val origin = parseOrigin
      val mode = parseMode

      val qs = origin.map(o => s"?origin=$o" + (if (mode == CheckMode) "&mode=check" else "")).getOrElse(if (mode == CheckMode) "?mode=check" else "")
      val actionCall = Call("POST", routes.RemoveThirdBusinessActivityController.onSubmit().url + qs)

      Ok(view(form, headingKey, actionCall, backLinkFrom(origin, mode), mode))
  }

  def onSubmit(): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
      val form = formProvider()
      val origin = parseOrigin
      val mode = parseMode

      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            {
              val qs = origin.map(o => s"?origin=$o" + (if (mode == CheckMode) "&mode=check" else "")).getOrElse(if (mode == CheckMode) "?mode=check" else "")
              val actionCall = Call("POST", routes.RemoveThirdBusinessActivityController.onSubmit().url + qs)
              Future.successful(BadRequest(view(formWithErrors, headingKey, actionCall, backLinkFrom(origin, mode), mode)))
            },
          {
            case true =>
              val base = request.userAnswers
              val updatedAnswers = for {
                r1 <- Future.fromTry(base.remove(BusinessActivityCodeThreePage))
                r2 <- Future.fromTry(r1.remove(BusinessActivityTwoPage))
              } yield r2

              updatedAnswers.flatMap(u => sessionRepository.set(u).map(_ => Redirect(routes.BusinessActivityTwoController.onPageLoad(NormalMode))))

            case false =>
              Future.successful(Redirect(backLinkFrom(origin, mode)))
          }
        )
  }

}
