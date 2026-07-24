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
import forms.DescribeItemsOnInvoiceFormProvider
import javax.inject.Inject
import models.Mode
import navigation.Navigator
import pages.{DescribeItemsOnInvoicePage, PurchaseTypePage, PurchaseSubTypePage, PurchaseSubCategoryPage}
import play.api.mvc.Call
import scala.util.Try
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.DescribeItemsOnInvoiceView

import scala.concurrent.{ExecutionContext, Future}

class DescribeItemsOnInvoiceController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
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

  val form = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
    val preparedForm = request.userAnswers.get(DescribeItemsOnInvoicePage) match {
      case None        => form
      case Some(value) => form.fill(value)
    }

    // compute a sensible back target: prefer PurchaseSubCategory -> PurchaseSubType -> PurchaseType
    val maybePurchaseTypeSlug = request.userAnswers.get(PurchaseTypePage).map(models.PurchaseType.slugOf)
    val maybeParentCode = request.userAnswers.get(PurchaseSubTypePage)
    val maybeChildCode = request.userAnswers.get(PurchaseSubCategoryPage)

    val back: Call = (maybePurchaseTypeSlug, maybeParentCode, maybeChildCode) match {
      case (Some(slug), Some(parent), Some(child)) =>
        // try a few candidate parent forms safely
        val head = parent.split("\\.").headOption.getOrElse(parent)
        val last = parent.split("\\.").lastOption.getOrElse(parent)
        val candidates = Seq(parent, last, head, child).distinct
        candidates.iterator.map { c => Try(controllers.routes.PurchaseSubCategoryController.onPageLoad(slug, c, mode)).toOption }.collectFirst { case Some(call) => call }.getOrElse(routes.PurchaseTypeController.onPageLoad(mode))

      case (Some(slug), Some(_), None) => controllers.routes.PurchaseSubTypeController.onPageLoad(slug, mode)

      case _ => routes.PurchaseTypeController.onPageLoad(mode)
    }

    Ok(view(preparedForm, mode, back))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    form
      .bindFromRequest()
      .fold(
        formWithErrors =>
          {
            val maybePurchaseTypeSlug = request.userAnswers.get(PurchaseTypePage).map(models.PurchaseType.slugOf)
            val maybeParentCode = request.userAnswers.get(PurchaseSubTypePage)
            val maybeChildCode = request.userAnswers.get(PurchaseSubCategoryPage)

            val back: Call = (maybePurchaseTypeSlug, maybeParentCode, maybeChildCode) match {
              case (Some(slug), Some(parent), Some(child)) =>
                val head = parent.split("\\.").headOption.getOrElse(parent)
                val last = parent.split("\\.").lastOption.getOrElse(parent)
                val candidates = Seq(parent, last, head, child).distinct
                candidates.iterator.map { c => Try(controllers.routes.PurchaseSubCategoryController.onPageLoad(slug, c, mode)).toOption }.collectFirst { case Some(call) => call }.getOrElse(routes.PurchaseTypeController.onPageLoad(mode))
              case (Some(slug), Some(_), None) => controllers.routes.PurchaseSubTypeController.onPageLoad(slug, mode)
              case _ => routes.PurchaseTypeController.onPageLoad(mode)
            }

            Future.successful(BadRequest(view(formWithErrors, mode, back)))
          },
        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(DescribeItemsOnInvoicePage, value))
            _              <- sessionRepository.set(updatedAnswers)
          } yield Redirect(navigator.nextPage(DescribeItemsOnInvoicePage, mode, updatedAnswers))
      )
  }
}
