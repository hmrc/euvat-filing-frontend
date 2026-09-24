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
import forms.purchase.DescribeItemsOnInvoiceFormProvider
import models.requests.DataRequest
import models.{CheckMode, Mode, Other, PurchaseOrImportType, UserAnswers}
import navigation.Navigator
import pages.*
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.{ConfigPurchaseOrImportMapping, CountryCode}
import utils.ControllerHelpers.*
import views.html.purchase.DescribeItemsOnInvoiceView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DescribeItemsOnInvoiceController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  configPurchaseMapping: ConfigPurchaseOrImportMapping,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: DescribeItemsOnInvoiceFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: DescribeItemsOnInvoiceView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  val form: Form[String] = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    val preparedForm = request.userAnswers.get(DescribeItemsOnInvoicePage).fold(form)(form.fill)

    if (mode == CheckMode && !request.userAnswers.get(pages.DescribeItemsArrivedFromCheckYourAnswersPage).contains(true)) {
      val markedTry = request.userAnswers.set(pages.DescribeItemsArrivedFromCheckYourAnswersPage, true)
      Future.fromTry(markedTry).flatMap { updated =>
        sessionRepository.set(updated).map(_ => Ok(view(preparedForm, mode)))
      }
    } else {
      Future.successful(Ok(view(preparedForm, mode)))
    }
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    form
      .bindFromRequest()
      .fold(
        formWithErrors =>
          if (formWithErrors.errors.exists(_.message == "describeItemsOnInvoice.error.required")) {
            saveToSession("").map(_ => Redirect(controllers.warning.routes.PurchaseWarningController.onPageLoad(mode)))
          } else {
            Future.successful(BadRequest(view(formWithErrors, mode)))
          },
        value => saveToSession(value).map(userAnswers => Redirect(navigator.nextPage(DescribeItemsOnInvoicePage, mode, userAnswers)))
      )
  }

  private def saveToSession(value: String)(implicit request: DataRequest[?]): Future[UserAnswers] =
    for {
      userAnswers <- Future.fromTry(request.userAnswers.set(DescribeItemsOnInvoicePage, value))
      _           <- sessionRepository.set(userAnswers)
    } yield userAnswers

}
