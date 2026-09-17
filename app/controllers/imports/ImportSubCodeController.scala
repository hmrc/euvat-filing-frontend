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
import controllers.routes
import forms.PurchaseOrImportSubTypeFormProvider
import models.requests.DataRequest
import models.{NormalMode, PurchaseOrImportType, UserAnswers}
import navigation.Navigator
import pages.{ImportSubCategoryPage, ImportSubCodePage, ImportTypePage}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents, Result}
import queries.{ImportSubCategoryLabelQuery, ImportSubTypeLabelQuery}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.PurchaseOrImportHelpers.*
import utils.{ConfigPurchaseMapping, CountryCode}
import views.html.PurchaseOrImportSubTypeView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Try}

class ImportSubCodeController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: PurchaseOrImportSubTypeFormProvider,
  config: ConfigPurchaseMapping,
  val controllerComponents: MessagesControllerComponents,
  view: PurchaseOrImportSubTypeView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  private def backUrl: String = controllers.imports.routes.ImportTypeController.onPageLoad(NormalMode).url

  private def withPageData(importTypeKey: String)(
    block: (PurchaseOrImportType, String, Seq[(String, String)]) => Future[Result]
  )(implicit request: DataRequest[AnyContent]): Future[Result] = {
    val resolved = for {
      importType <- PurchaseOrImportType.values.find(_.toString == importTypeKey)
      answered   <- request.userAnswers.get(ImportTypePage) if answered == importType
      country    <- CountryCode.findCountryCode(request.userAnswers)
      options    <- config.selectableImportSubcodes(country, importType.toString)
    } yield (importType, country, options)

    resolved match {
      case Some((importType, country, options)) => block(importType, country, options)
      case None                                 => Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
    }
  }

  private def renderView(importType: PurchaseOrImportType, options: Seq[(String, String)], form: Form[String])(implicit
    request: DataRequest[AnyContent]
  ) = {
    val messages = request2Messages
    view(
      form,
      radioItems(config, options),
      messages(s"importSubCode.$importType.title"),
      messages(s"importSubCode.$importType.heading"),
      "import.caption",
      controllers.imports.routes.ImportSubCodeController.onSubmit(importType.toString),
      backUrl
    )
  }

  private def clearSubCategoryIfChanged(answers: UserAnswers, value: String): Try[UserAnswers] =
    if (answers.get(ImportSubCodePage).contains(value)) Success(answers)
    else answers.remove(ImportSubCategoryPage).flatMap(_.remove(ImportSubCategoryLabelQuery))

  private def nextPage(importType: PurchaseOrImportType, country: String, subCode: String, answers: UserAnswers): Call =
    if (config.subcategoriesFor(country, importType.toString, subCode).nonEmpty) {
      controllers.imports.routes.ImportSubCategoryController.onPageLoad(NormalMode)
    } else {
      navigator.nextPage(ImportSubCodePage, NormalMode, answers)
    }

  def onPageLoad(importTypeKey: String): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    withPageData(importTypeKey) { (importType, _, options) =>
      val form = preparedForm(formProvider, s"importSubCode.$importType.error.required", request.userAnswers.get(ImportSubCodePage))
      Future.successful(Ok(renderView(importType, options, form)))
    }
  }

  def onSubmit(importTypeKey: String): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    withPageData(importTypeKey) { (importType, country, options) =>
      formProvider(s"importSubCode.$importType.error.required")
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(renderView(importType, options, formWithErrors))),
          value =>
            if (allowedValues(options).contains(value)) {
              val label = labelFor(value, options)
              for {
                cleared        <- Future.fromTry(clearSubCategoryIfChanged(request.userAnswers, value))
                updatedAnswers <- Future.fromTry(setSelection(cleared, ImportSubCodePage, ImportSubTypeLabelQuery, value, label))
                _              <- sessionRepository.set(updatedAnswers)
              } yield Redirect(nextPage(importType, country, value, updatedAnswers))
            } else {
              Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
            }
        )
    }
  }
}
