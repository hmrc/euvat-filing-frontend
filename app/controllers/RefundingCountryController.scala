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

import controllers.actions._
import forms.RefundingCountryFormProvider
import javax.inject.Inject
import models.{Mode, NormalMode}
import navigation.Navigator
import pages.RefundingCountryPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.Configuration
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.RefundingCountryView

import scala.concurrent.{ExecutionContext, Future}
import play.api.data.FormError

class RefundingCountryController @Inject()(
                                           override val messagesApi: MessagesApi,
                                           sessionRepository: SessionRepository,
                                           navigator: Navigator,
                                           identify: IdentifierAction,
                                           getData: DataRetrievalAction,
                                           requireData: DataRequiredAction,
                                          formProvider: RefundingCountryFormProvider,
                                          config: Configuration,
                                           val controllerComponents: MessagesControllerComponents,
                                           view: RefundingCountryView
                                         )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  val form = formProvider()

  def onPageLoad(): Action[AnyContent] = (identify andThen getData) { implicit request =>

    val preparedForm = request.userAnswers.flatMap(_.get(RefundingCountryPage)) match {
      case None => form
      case Some(value) => form.fill(value)
    }

    val countries = config.getOptional[Seq[String]]("eu.member-states").getOrElse(Seq.empty).map { s =>
      s.split("\\|") match {
        case Array(name, code) => (name.trim, code.trim)
        case Array(name)       => (name.trim, "")
        case _                 => (s, "")
      }
    }

    Ok(view(preparedForm, countries))
  }

  def onSubmit(): Action[AnyContent] = (identify andThen getData).async { implicit request =>

    val countries = config.getOptional[Seq[String]]("eu.member-states").getOrElse(Seq.empty).map { s =>
      s.split("\\|") match {
        case Array(name, code) => (name.trim, code.trim)
        case Array(name)       => (name.trim, "")
        case _                 => (s, "")
      }
    }

    form.bindFromRequest().fold(
      formWithErrors => {
        val typed = request.body.asFormUrlEncoded.flatMap(_.get("valueTyped").flatMap(_.headOption)).getOrElse("")
        val adjustedForm = if (typed.trim.nonEmpty) {
          val filtered = formWithErrors.errors.filterNot(e => e.key == "value" && e.message == "refundingCountry.error.required")
          formWithErrors.copy(errors = filtered :+ FormError("value", "refundingCountry.error.invalid"))
        } else {
          formWithErrors
        }
        Future.successful(BadRequest(view(adjustedForm, countries)))
      },
      value => {
        val allowed = countries.exists { case (name, code) => code == value || name == value }
        if (!allowed) {
          val formWithInvalid = form.bindFromRequest().withError("value", "refundingCountry.error.invalid")
          Future.successful(BadRequest(view(formWithInvalid, countries)))
        } else {
          val baseAnswers = request.userAnswers.getOrElse(models.UserAnswers(request.userId))
          for {
            updatedAnswers <- Future.fromTry(baseAnswers.set(RefundingCountryPage, value))
            _ <- sessionRepository.set(updatedAnswers)
          } yield Redirect(navigator.nextPage(RefundingCountryPage, NormalMode, updatedAnswers))
        }
      }
    )
  }
}
