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

package controllers.imports

import controllers.actions.*
import forms.DescribeItemsFormProvider
import models.requests.DataRequest
import models.{Mode, Other, UserAnswers}
import navigation.Navigator
import pages.{DescribeItemsOnImportDocPage, ImportSubCodePage, ImportTypePage, RefundingLanguagePage}
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents, Result}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.{ConfigPurchaseOrImportMapping, CountryCode}
import views.html.PurchaseOrImportDescribeItemsView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DescribeItemsOnImportDocController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: DescribeItemsFormProvider,
  config: ConfigPurchaseOrImportMapping,
  val controllerComponents: MessagesControllerComponents,
  view: PurchaseOrImportDescribeItemsView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  private val messagePrefix = "describeItemsOnImportDoc"

  val form: Form[String] = formProvider(messagePrefix)

  private def submitCall(mode: Mode): Call = controllers.imports.routes.DescribeItemsOnImportDocController.onSubmit(mode)

  private def isReachable(answers: UserAnswers): Boolean =
    answers.get(ImportTypePage).contains(Other) && {
      answers.get(ImportSubCodePage) match {
        case Some(subCode) =>
          subCode == ConfigPurchaseOrImportMapping.NoneValue || subCode.split("\\.").lastOption.contains("99")
        case None =>
          CountryCode
            .findCountryCode(answers)
            .forall(country => config.selectableSubcodes(country, Other.toString).isEmpty)
      }
    }

  private def hint(answers: UserAnswers)(implicit messages: Messages): Option[String] =
    answers
      .get(RefundingLanguagePage)
      .map(language => messages(s"$messagePrefix.hint", messages(s"refundingLanguage.${language.value}")))

  private def renderView(form: Form[String], mode: Mode)(implicit request: DataRequest[AnyContent]) =
    view(form, submitCall(mode), messagePrefix, "import.caption", hint(request.userAnswers))

  private def withGuard(block: => Future[Result])(implicit request: DataRequest[AnyContent]): Future[Result] =
    if (isReachable(request.userAnswers)) block
    else Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    withGuard {
      val preparedForm = request.userAnswers.get(DescribeItemsOnImportDocPage).fold(form)(form.fill)
      Future.successful(Ok(renderView(preparedForm, mode)))
    }
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    withGuard {
      form
        .bindFromRequest()
        .fold(
          // TODO: an empty field should show the warning interruption page once the import version exists
          formWithErrors => Future.successful(BadRequest(renderView(formWithErrors, mode))),
          value =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(DescribeItemsOnImportDocPage, value))
              _              <- sessionRepository.set(updatedAnswers)
            } yield Redirect(navigator.nextPage(DescribeItemsOnImportDocPage, mode, updatedAnswers))
        )
    }
  }
}
