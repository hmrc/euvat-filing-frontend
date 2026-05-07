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
import forms.{RefundPeriodData, RefundPeriodFormProvider}
import models.RefundPeriod
import navigation.Navigator
import pages.RefundPeriodPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.RefundPeriodView

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class RefundPeriodController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: RefundPeriodFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: RefundPeriodView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>

    val preparedForm = request.userAnswers.get(RefundPeriodPage) match {
      case None => formProvider()
      case Some(value) =>
        val start = java.time.YearMonth.of(value.startDate.getYear, value.startDate.getMonthValue)
        val end = java.time.YearMonth.of(value.endDate.getYear, value.endDate.getMonthValue)
        formProvider().fill(RefundPeriodData(start, end))
    }

    Ok(view(formProvider.withMappedErrors(preparedForm)))
  }

  def onSubmit(): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>

    formProvider()
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formProvider.withMappedErrors(formWithErrors)))),
        value =>
          for {
            updatedAnswers <- Future.fromTry(
                                request.userAnswers.set(
                                  RefundPeriodPage,
                                  RefundPeriod(
                                    LocalDate.of(value.start.getYear, value.start.getMonthValue, 1),
                                    LocalDate.of(value.end.getYear, value.end.getMonthValue, 1)
                                  )
                                )
                              )
            _ <- sessionRepository.set(updatedAnswers)
          } yield Redirect(navigator.nextPage(RefundPeriodPage, models.NormalMode, updatedAnswers))
      )
  }
}
