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
import forms.RemoveItemFormProvider
import models.{NormalMode, PurchaseImport, UserAnswers}
import models.requests.{DataRequest, PurchaseImportListRequest}
import navigation.Navigator
import play.api.data.Form
import pages.RemoveItemPage
import play.api.Logging
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.*
import queries.{IsLastItemQuery, ClaimApplicationResponseQuery, RemoveItemNumberQuery}
import repositories.SessionRepository
import services.EuVatRefundsService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.{ControllerHelpers, CurrencyConfig}
import views.html.RemoveItemView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class RemoveItemController @Inject()(
      override val messagesApi: MessagesApi,
      sessionRepository: SessionRepository,
      navigator: Navigator,
      identify: IdentifierAction,
      getData: DataRetrievalAction,
      requireData: DataRequiredAction,
      formProvider: RemoveItemFormProvider,
      euVatRefundsService: EuVatRefundsService,
      currencyConfig: CurrencyConfig,
      val controllerComponents: MessagesControllerComponents,
      view: RemoveItemView
)(implicit ec: ExecutionContext) extends FrontendBaseController with Logging with I18nSupport {

  val form: Form[Boolean] = formProvider()

  def onPageLoad: Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
      request.getQueryString("itemNumber").flatMap(_.toIntOption) match {
        case Some(itemNumber) => saveItemNumberAndReload(itemNumber)
        case None             => renderPage(formToRender = form, badRequest = false)

    }
  }

  def onSubmit: Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
           form
            .bindFromRequest()
            .fold(
              formWithErrors => renderPage(formToRender = formWithErrors, badRequest = true),
            value => saveAndRedirect(value)
            )
  }

  private def renderPage(formToRender: Form[Boolean], badRequest: Boolean)(implicit request: DataRequest[AnyContent]): Future[Result] =
    withItem(request.userAnswers) { (item, _) =>
      val (itemType, vatClaim) = summaryValues(item)
      val page                 = view(formToRender, itemType, vatClaim, backLink)
      Future.successful(if (badRequest) BadRequest(page) else Ok(page))
    }

  private def saveAndRedirect(value: Boolean)(implicit request: DataRequest[AnyContent]): Future[Result] =
    withItem(request.userAnswers) { (_, isLastItem) =>
      for {
        answersWithChoice <- Future.fromTry(request.userAnswers.set(RemoveItemPage, value))
        updatedAnswers    <- Future.fromTry(answersWithChoice.set(IsLastItemQuery, isLastItem))
        _                 <- sessionRepository.set(updatedAnswers)
      } yield Redirect(navigator.nextPage(RemoveItemPage, NormalMode, updatedAnswers))
    }

  private def saveItemNumberAndReload(itemNumber: Int)(implicit request: DataRequest[AnyContent]): Future[Result] =
    for {
      updatedAnswers <- Future.fromTry(request.userAnswers.set(RemoveItemNumberQuery, itemNumber))
      _               <- sessionRepository.set(updatedAnswers)
    } yield Redirect(routes.RemoveItemController.onPageLoad)

  private def withItem(userAnswers: UserAnswers)(f: (PurchaseImport, Boolean) => Future[Result])(implicit
    request: DataRequest[AnyContent]
  ): Future[Result] =
    (getApplicationId(userAnswers), userAnswers.get(RemoveItemNumberQuery)) match {
      case (Some(applicationId), Some(itemNumber)) =>
        euVatRefundsService
          .getPurchaseImportList(PurchaseImportListRequest(applicationId))
          .flatMap { response =>
            val items = response.purchaseImportList
            items
               .find(_.itemNumber == itemNumber)
               .fold(recoveryPage(s"Item $itemNumber not found")) { item =>
                 f(item, items.size == 1)
               }
          }
      case _ =>
        recoveryPage("Missing or invalid applicationId or itemNumber")
    }

  private def summaryValues(item: PurchaseImport)(implicit request: DataRequest[AnyContent]): (String, String) = {
    val itemType = if (item.itemType == "P") "Purchase" else "Import"
    val vatClaim =
      if(isIncomplete(item)) messagesApi.preferred(request)("removeItem.notProvided")
      else {
        val currencySymbol = ControllerHelpers.currencySymbolFromSession(request.userAnswers, currencyConfig.currencyConfig)
        s"$currencySymbol${item.deductibleVatAmount}"
      }
    (itemType, vatClaim)
  }

  private def isIncomplete(item: PurchaseImport): Boolean = item.deductibleVatAmount == 0

  private def backLink: Call = routes.PurchaseImportSummaryController.onPageLoad

  private def recoveryPage(msg: String): Future[Result] = {
    logger.warn(msg)
    Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
  }

  private def getApplicationId(userAnswers: UserAnswers): Option[Long] =
    userAnswers.get(ClaimApplicationResponseQuery).map(_.applicationId)
}