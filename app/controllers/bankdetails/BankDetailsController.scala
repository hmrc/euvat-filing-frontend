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

package controllers.bankdetails

import controllers.actions.*
import forms.bankdetails.BankDetailsFormProvider
import models.Mode
import navigation.Navigator
import pages.bankdetails.BankDetailsPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.bankdetails.BankDetailsView
import config.InternationalCurrencyProvider
import queries.BankDetailsChangedQuery

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class BankDetailsController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: BankDetailsFormProvider,
  internationalCurrencyProvider: InternationalCurrencyProvider,
  val controllerComponents: MessagesControllerComponents,
  view: BankDetailsView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  private val form = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] =
    (identify andThen getData andThen requireData) { implicit request =>

      val preparedForm =
        request.userAnswers
          .get(BankDetailsPage)
          .fold(form)(form.fill)

      Ok(
        view(
          preparedForm,
          mode,
          internationalCurrencyProvider.currencies
        )
      )
    }

  def onSubmit(mode: Mode): Action[AnyContent] =
    (identify andThen getData andThen requireData).async { implicit request =>

      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            Future.successful(
              BadRequest(
                view(
                  formWithErrors,
                  mode,
                  internationalCurrencyProvider.currencies
                )
              )
            ),
          value =>
            for {
              updatedAnswers <-
                Future.fromTry(
                  request.userAnswers.set(
                    BankDetailsPage,
                    value
                  )
                )

              bankDetailsChanged <- Future.fromTry(updatedAnswers.set(BankDetailsChangedQuery, true))

              _ <-
                sessionRepository.set(bankDetailsChanged)

            } yield Redirect(
              navigator.nextPage(
                BankDetailsPage,
                mode,
                bankDetailsChanged
              )
            )
        )
    }
}
