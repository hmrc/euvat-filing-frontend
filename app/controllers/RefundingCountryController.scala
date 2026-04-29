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
import models.{NormalMode, UserAnswers}
import navigation.Navigator
import pages.RefundingCountryPage
import play.api.Configuration
import play.api.data.FormError
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.CountryList
import views.html.RefundingCountryView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import play.api.Logger
import scala.util.control.NonFatal

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

  // Build back URL using `urls.loginContinue` which includes the configured context path
  private val taskListBackUrl: Option[String] = Some(config.get[String]("urls.loginContinue") + controllers.routes.TaskListDashboardController.onPageLoad().url)

  private def buildFormAndCountries() = {
    val countries = CountryList.fromConfig(config)
    val allowed: Set[String] = countries.flatMap { case (name, code) => Seq(name, code) }.toSet
    val form = formProvider(allowed)
    (countries, form)
  }

  def onPageLoad(): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>

    val cameFromTaskList = request.headers.get("Referer").exists(_.contains(controllers.routes.TaskListDashboardController.onPageLoad().url))

    val (countries, form) = buildFormAndCountries()

    // If we have a previously selected country, pre-fill the form even when
    // the user arrived from the task list. Otherwise render an empty form.
    val preparedForm = request.userAnswers.get(RefundingCountryPage).fold(form)(form.fill)

    Ok(view(preparedForm, countries, taskListBackUrl, cameFromTaskList))
  }

  def onSubmit(): Action[AnyContent] = (identify andThen getData).async { implicit request =>

    val (countries, form) = buildFormAndCountries()

    val backUrl = taskListBackUrl

    val cameFromTaskListFormFlag: Boolean = request.body.asFormUrlEncoded.flatMap(_.get("cameFromTaskList").flatMap(_.headOption)).contains("true")

    if (request.userAnswers.isEmpty && !cameFromTaskListFormFlag) {
      Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
    } else {

      val baseAnswers: UserAnswers = request.userAnswers.getOrElse(UserAnswers(request.userId))

      val boundResult = form.bindFromRequest().fold(
        formWithErrors => {
          val typed = request.body.asFormUrlEncoded.flatMap(_.get("valueTyped").flatMap(_.headOption)).getOrElse("")
          val adjustedForm = if (typed.trim.nonEmpty) {
            val filtered = formWithErrors.errors.filterNot(e => e.key == "value" && e.message == "refundingCountry.error.required")
            formWithErrors.copy(errors = filtered :+ FormError("value", "refundingCountry.error.invalid"))
          } else {
            formWithErrors
          }
          Future.successful(BadRequest(view(adjustedForm, countries, backUrl, cameFromTaskListFormFlag)))
        },
        value => {
          for {
            updatedAnswers <- Future.fromTry(baseAnswers.set(RefundingCountryPage, value))
            _ <- sessionRepository.set(updatedAnswers)
          } yield Redirect(navigator.nextPage(RefundingCountryPage, NormalMode, updatedAnswers))
        }
      )

      boundResult.recover {
        case NonFatal(e) =>
          Logger(getClass).error("Error in RefundingCountryController.onSubmit", e)
          BadRequest(view(form.bindFromRequest(), countries, backUrl, cameFromTaskListFormFlag))
      }
    }
  }
}
