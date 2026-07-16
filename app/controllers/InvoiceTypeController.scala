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
import forms.InvoiceTypeFormProvider

import javax.inject.Inject
import models.Mode
import navigation.Navigator
import pages.{InvoiceTypePage, PurchaseTypePage, PurchaseSubTypePage, PurchaseSubCategoryPage, PurchaseSubCategoryLabelPage, PurchaseSubTypeLabelPage, DescribeItemsOnInvoicePage}
import models.requests.DataRequest
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.InvoiceTypeView
import play.api.mvc.Call

import scala.concurrent.{ExecutionContext, Future}

class InvoiceTypeController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: InvoiceTypeFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: InvoiceTypeView
)(implicit ec: ExecutionContext)
  extends FrontendBaseController
  with I18nSupport
  with play.api.Logging {

  val form = formProvider()

  /**
   * Compute the back target Call without mutating session. Used to render the back link.
   */
  private def computeBackTarget(mode: Mode)(implicit request: DataRequest[?]): play.api.mvc.Call = {
    val maybePurchaseTypeSlug = request.userAnswers.get(PurchaseTypePage).map(models.PurchaseType.slugOf)
    val maybeParentCode = request.userAnswers.get(PurchaseSubTypePage)
    val maybeChildCode = request.userAnswers.get(PurchaseSubCategoryPage)

    try {
      logger.info(s"InvoiceTypeController.backLink - purchaseType=${maybePurchaseTypeSlug}, parent=${maybeParentCode}, child=${maybeChildCode}")
    } catch { case _: Throwable => }

    // Only route back to Describe Items when PurchaseType is Other and the
    // chosen sub-category indicates "None of these" (sentinel 99).
    val isOtherWithNoneSelected: Boolean = request.userAnswers.get(PurchaseTypePage).contains(models.PurchaseType.Other) && {
      val childIsNone = request.userAnswers.get(PurchaseSubCategoryPage).exists(v => v.split("\\.").lastOption.contains("99"))
      val parentIsNone = request.userAnswers.get(PurchaseSubTypePage).exists(v => v.split("\\.").lastOption.contains("99"))
      childIsNone || parentIsNone
    }

    if (isOtherWithNoneSelected) controllers.routes.DescribeItemsOnInvoiceController.onPageLoad(mode)
    else

    (maybePurchaseTypeSlug, maybeParentCode, maybeChildCode) match {
      case (Some(slug), None, Some(child)) =>
        // Only derive a parent when the child contains a dotted parent
        // component (e.g. "1.2"). For simple single-segment children we
        // prefer the safe fallback to the PurchaseType page (per tests).
        if (child.contains(".")) {
          val parent = child.split("\\.").head
          controllers.routes.PurchaseSubCategoryController.onPageLoad(slug, parent, mode)
        } else routes.PurchaseTypeController.onPageLoad(mode)

      case (Some(slug), Some(parent), Some(child)) =>
        val head = parent.split("\\.").headOption.getOrElse(parent)
        val last = parent.split("\\.").lastOption.getOrElse(parent)
        val candidates = Seq(parent, last, head, child).distinct

        val maybeCall = candidates.iterator.map { c =>
          try Some(controllers.routes.PurchaseSubCategoryController.onPageLoad(slug, c, mode))
          catch { case _: Throwable => None }
        }.collectFirst { case Some(call) => call }

        maybeCall.getOrElse(routes.PurchaseTypeController.onPageLoad(mode))

      case (Some(slug), Some(_), None) =>
        controllers.routes.PurchaseSubTypeController.onPageLoad(slug, mode)

      case _ =>
        routes.PurchaseTypeController.onPageLoad(mode)
    }
  }

  /**
   * Back-link endpoint: when the user clicks the back link this endpoint is hit, clears the
   * appropriate session keys and then redirects to the computed target. This ensures clearing
   * happens at the click moment instead of when InvoiceType is rendered.
   */


  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    val preparedForm = request.userAnswers.get(InvoiceTypePage) match {
      case None        => form
      case Some(value) => form.fill(value)
    }

    val back = computeBackTarget(mode)
    Future.successful(Ok(view(preparedForm, mode, back)))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>

    form
      .bindFromRequest()
      .fold(
              formWithErrors =>
                // render errors
                Future.successful(BadRequest(view(formWithErrors, mode, computeBackTarget(mode)))),
        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(InvoiceTypePage, value))
            _              <- sessionRepository.set(updatedAnswers)
          } yield Redirect(navigator.nextPage(InvoiceTypePage, mode, updatedAnswers))
      )
  }
}
