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
import models.{Mode, RefundPeriod}
import navigation.Navigator
import pages.RefundPeriodPage
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import services.EuVatRefundsService
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
  euVatRefundsService: EuVatRefundsService,
  val controllerComponents: MessagesControllerComponents,
  view: RefundPeriodView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  private def errorMessage(form: Form[RefundPeriodData], keys: Seq[String])(implicit messages: Messages): Option[String] = {
    val errors = form.errors.filter(e => keys.contains(e.key))
    if (errors.isEmpty) None
    else Some(errors.map(e => messages(e.message, e.args*)).mkString("<br>"))
  }

  private def errorLinkOverrides(form: Form[RefundPeriodData]): Map[String, String] = Map(
    ""                           -> s"${form("start").id}.month",
    "start"                      -> s"${form("start").id}.month",
    "end"                        -> s"${form("end").id}.month",
    s"${form("start").id}.year"  -> s"${form("start").id}.year",
    s"${form("end").id}.year"    -> s"${form("end").id}.year",
    s"${form("start").id}.month" -> s"${form("start").id}.month",
    s"${form("end").id}.month"   -> s"${form("end").id}.month"
  )

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
    val preparedForm = request.userAnswers.get(RefundPeriodPage) match {
      case None => formProvider()
      case Some(value) =>
        val start = java.time.YearMonth.of(value.startDate.getYear, value.startDate.getMonthValue)
        val end = java.time.YearMonth.of(value.endDate.getYear, value.endDate.getMonthValue)
        formProvider().fill(RefundPeriodData(start, end))
    }
    val (mappedForm, highlighted) = formProvider.withMappedErrors(preparedForm)
    val startMsg = errorMessage(mappedForm, Seq("start", "start.month", "start.year"))
    val endMsg = errorMessage(mappedForm, Seq("end", "end.month", "end.year"))
    Ok(
      view(mappedForm,
           mode,
           controllers.routes.RefundingLanguageController.onPageLoad(mode),
           startMsg,
           endMsg,
           highlighted,
           errorLinkOverrides(mappedForm)
          )
    )
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    formProvider()
      .bindFromRequest()
      .fold(
        formWithErrors => {
          val (mappedForm, highlighted) = formProvider.withMappedErrors(formWithErrors)
          val startMsg = errorMessage(mappedForm, Seq("start", "start.month", "start.year"))
          val endMsg = errorMessage(mappedForm, Seq("end", "end.month", "end.year"))
          Future.successful(
            BadRequest(
              view(mappedForm,
                   mode,
                   controllers.routes.RefundingLanguageController.onPageLoad(mode),
                   startMsg,
                   endMsg,
                   highlighted,
                   errorLinkOverrides(mappedForm)
                  )
            )
          )
        },
        value =>
          euVatRefundsService.retrieveTraderKnownFacts().flatMap { traderResponse =>

            val startDate = value.start.atDay(1).atStartOfDay()
            val endDate = value.end.atEndOfMonth().atTime(23, 59, 59)

            val maybeRegDate = traderResponse.dateOfRegistration
            val maybeDeregDate = traderResponse.dateOfRegistration

            val registrationError = maybeRegDate match {
              case Some(regDate) =>
                if (startDate.isBefore(regDate))
                  Some("start" -> "refundPeriod.error.periodStartDateAfterVatRegistration")
                else None
              case None => None
            }

            for {
              updatedAnswers <- Future.fromTry(
                                  request.userAnswers.set(
                                    RefundPeriodPage,
                                    RefundPeriod(
                                      java.time.YearMonth.of(value.start.getYear, value.start.getMonthValue).atDay(1).atStartOfDay(),
                                      java.time.YearMonth.of(value.end.getYear, value.end.getMonthValue).atEndOfMonth().atTime(23, 59, 59, 999000000)
                                    )
                                  )
                                )
              _ <- sessionRepository.set(updatedAnswers)
            } yield Redirect(navigator.nextPage(RefundPeriodPage, mode, updatedAnswers))
          }
      )
  }
}
