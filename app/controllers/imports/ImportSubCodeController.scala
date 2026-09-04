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
import forms.PurchaseSubTypeFormProvider
import models.requests.DataRequest
import models.{ImportType, NormalMode}
import navigation.Navigator
import pages.{ImportSubCodePage, ImportTypePage}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import repositories.SessionRepository
import uk.gov.hmrc.govukfrontend.views.viewmodels.radios.RadioItem
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.{ConfigPurchaseMapping, CountryCode}
import views.html.imports.ImportSubCodeView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ImportSubCodeController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: PurchaseSubTypeFormProvider,
  config: ConfigPurchaseMapping,
  val controllerComponents: MessagesControllerComponents,
  view: ImportSubCodeView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  private val onlyOtherSubCode = "10.99"

  private def backUrl: String = routes.TaskListDashboardController.onPageLoad().url

  private def withPageData(importTypeKey: String)(
    block: (ImportType, Seq[(String, String)]) => Future[Result]
  )(implicit request: DataRequest[AnyContent]): Future[Result] = {
    val resolved = for {
      importType <- ImportType.fromKey(importTypeKey)
      answered   <- request.userAnswers.get(ImportTypePage) if answered == importType
      country    <- CountryCode.findCountryCode(request.userAnswers)
    } yield (importType, config.subcodesFor(country, importType.toString).filter(_._1.split("\\.").length == 2))

    resolved match {
      case Some((importType, options)) if options.nonEmpty && options.map(_._1) != Seq(onlyOtherSubCode) =>
        block(importType, options)
      case _ =>
        Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
    }
  }

  private def radioItems(options: Seq[(String, String)])(implicit request: DataRequest[AnyContent]): Seq[RadioItem] = {
    val items = config.buildRadioItems(options, request2Messages)
    if (options.map(_._1).contains(onlyOtherSubCode)) {
      items.filterNot(_.value.contains(ConfigPurchaseMapping.NoneValue))
    } else {
      items
    }
  }

  private def allowedValues(options: Seq[(String, String)]): Seq[String] = {
    val codes = options.map(_._1)
    if (codes.contains(onlyOtherSubCode)) codes else codes :+ ConfigPurchaseMapping.NoneValue
  }

  private def renderView(importType: ImportType, options: Seq[(String, String)], form: Form[String])(implicit
    request: DataRequest[AnyContent]
  ) = {
    val messages = request2Messages
    view(
      form,
      radioItems(options),
      messages(s"importSubCode.$importType.title"),
      messages(s"importSubCode.$importType.heading"),
      controllers.imports.routes.ImportSubCodeController.onSubmit(importType.toString),
      backUrl
    )
  }

  def onPageLoad(importTypeKey: String): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    withPageData(importTypeKey) { (importType, options) =>
      val form = formProvider(s"importSubCode.$importType.error.required")
      val preparedForm = request.userAnswers.get(ImportSubCodePage) match {
        case Some(value) => form.fill(value)
        case None        => form
      }

      Future.successful(Ok(renderView(importType, options, preparedForm)))
    }
  }

  def onSubmit(importTypeKey: String): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    withPageData(importTypeKey) { (importType, options) =>
      formProvider(s"importSubCode.$importType.error.required")
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(renderView(importType, options, formWithErrors))),
          value =>
            if (allowedValues(options).contains(value)) {
              for {
                updatedAnswers <- Future.fromTry(request.userAnswers.set(ImportSubCodePage, value))
                _              <- sessionRepository.set(updatedAnswers)
              } yield Redirect(navigator.nextPage(ImportSubCodePage, NormalMode, updatedAnswers))
            } else {
              Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
            }
        )
    }
  }
}
