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
import models.{Mode, NormalMode, UserAnswers, CheckMode}
import navigation.Navigator
import pages._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.RemoveBusinessActivityView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class RemoveSecondBusinessActivityController @Inject() (
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

  private def headingKey = "removeSecond.heading"

  private def backLinkFrom(origin: Option[String], mode: Mode): Call = origin match {
    case Some("business-activity-2") => routes.BusinessActivityTwoController.onPageLoad(mode)
    case Some("business-activity-3") => routes.BusinessActivityThreeController.onPageLoad()
    case Some("what-is-the-2nd-business-activity") => routes.BusinessActivityCodeTwoController.onPageLoad(mode)
    case _                              => routes.BusinessActivityTwoController.onPageLoad(mode)
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
        val actionCall = Call("POST", routes.RemoveSecondBusinessActivityController.onSubmit().url + qs)

        // show the 2nd BA image on this remove page
        Ok(view(form, headingKey, actionCall, backLinkFrom(origin, mode), mode, Some("/assets/images/2nd-ba-image.png")))
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
              val actionCall = Call("POST", routes.RemoveSecondBusinessActivityController.onSubmit().url + qs)
              Future.successful(BadRequest(view(formWithErrors, headingKey, actionCall, backLinkFrom(origin, mode), mode)))
            },
          {
            case true =>
              val base = request.userAnswers
              val updatedAnswers: UserAnswers = base.get(BusinessActivityCodeThreePage) match {
                case Some(code3) =>
                  // re-index: move 3rd to 2nd, remove 3rd and remove any stored
                  // boolean so the target page doesn't pre-select a radio
                  val u1 = base.set(BusinessActivityCodeTwoPage, code3).get
                  val u2 = u1.remove(BusinessActivityCodeThreePage).get
                  val u3 = u2.remove(BusinessActivityTwoPage).getOrElse(u2)
                  u3
                case None =>
                  // simply remove 2nd and remove any stored BusinessActivityPage
                  // boolean so the main page does not pre-select 'No'
                  val r1Try = base.remove(BusinessActivityCodeTwoPage)
                  val r1 = r1Try.getOrElse(base)
                  val r2 = r1.remove(BusinessActivityTwoPage).getOrElse(r1)
                  val r3 = r2.remove(BusinessActivityPage).getOrElse(r2)
                  r3
              }

              for {
                _ <- sessionRepository.set(updatedAnswers)
              } yield {
                origin match {
                  case Some("business-activity-3") => Redirect(routes.BusinessActivityTwoController.onPageLoad(NormalMode))
                  case _                             => Redirect(routes.BusinessActivityController.onPageLoad(NormalMode))
                }
              }

            case false =>
              Future.successful(Redirect(backLinkFrom(origin, mode)))
          }
        )
  }

}
