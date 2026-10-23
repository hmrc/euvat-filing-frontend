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
import models.{NormalMode, UserAnswers}
import models.requests.{DataRequest, DeleteItemRequest}
import models.responses.{DeleteItemResponse, ImportDetailsResponse}
import navigation.Navigator
import play.api.data.Form
import pages.RemoveItemPage
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.*
import queries.{ClaimApplicationResponseQuery, PurchaseImportTotalItemsQuery, RemoveItemNumberQuery, RemoveItemTypeQuery, UpdateSequenceNumberQuery}
import repositories.SessionRepository
import services.EuVatRefundsService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.{ControllerHelpers, CurrencyConfig}
import views.html.RemoveItemView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class RemoveItemController @Inject() (
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
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with Logging
    with I18nSupport {

  val form: Form[Boolean] = formProvider()

  def onPageLoad(itemType: String, itemNumber: Int): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    val selectedItem = for {
      withNumber <- request.userAnswers.set(RemoveItemNumberQuery, itemNumber)
      withType   <- withNumber.set(RemoveItemTypeQuery, itemType)
    } yield withType

    Future.fromTry(selectedItem).flatMap { answers =>
      sessionRepository.set(answers).flatMap(_ => renderPage(form, badRequest = false, answers))
    }
  }

  def onSubmit: Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    form
      .bindFromRequest()
      .fold(
        formWithErrors => renderPage(formToRender = formWithErrors, badRequest = true, request.userAnswers),
        value => saveAndRedirect(value)
      )
  }

  private def renderPage(formToRender: Form[Boolean], badRequest: Boolean, userAnswers: models.UserAnswers)(implicit
    request: DataRequest[AnyContent]
  ): Future[Result] =
    (getApplicationId(userAnswers), userAnswers.get(RemoveItemNumberQuery), userAnswers.get(RemoveItemTypeQuery)) match {
      case (Some(applicationId), Some(itemNumber), Some(itemType)) =>
        userAnswers.get(PurchaseImportTotalItemsQuery) match {
          case Some(_) =>
            val selectedDetails: Future[Option[(BigDecimal, Int)]] = itemType match {
              case "I" =>
                euVatRefundsService
                  .getImportDetails(applicationId, itemNumber)
                  .map(response => Some(response.deductibleVatAmount -> response.updateSequenceNumber))
                  .recover { case ex =>
                    logger.warn(s"Unable to retrieve import details for item $itemNumber", ex)
                    None
                  }
              case "P" =>
                euVatRefundsService
                  .getPurchaseDetails(applicationId, itemNumber)
                  .map(response => Some(response.deductibleVatAmount.getOrElse(BigDecimal(0)) -> response.updateSequenceNumber))
                  .recover { case ex =>
                    logger.warn(s"Unable to retrieve purchase details for item $itemNumber", ex)
                    None
                  }
              case _ => Future.successful(None)
            }

            selectedDetails.flatMap {
              case Some((amount, sequenceNumber)) =>
                val updatedAnswersTry = userAnswers.set(UpdateSequenceNumberQuery, sequenceNumber)

                Future.fromTry(updatedAnswersTry).flatMap { updatedAnswers =>
                  sessionRepository.set(updatedAnswers).map { _ =>
                    val (displayType, vatClaim) = summaryValues(itemType, amount, updatedAnswers)
                    val page = view(formToRender, displayType, vatClaim, backLink)
                    if (badRequest) BadRequest(page) else Ok(page)
                  }
                }
              case None => recoveryPage(s"Item $itemNumber not found")
            }
          case None => recoveryPage("Missing purchase/import total item count")
        }
      case _ =>
        recoveryPage("Missing or invalid applicationId, itemNumber or itemType")
    }

  private def saveAndRedirect(value: Boolean)(implicit request: DataRequest[AnyContent]): Future[Result] =
    request.userAnswers.get(PurchaseImportTotalItemsQuery) match {
      case Some(_) if !value => saveChoiceAndRedirect(value, None)
      case Some(_) =>
        val updateSequenceNumber = request.userAnswers
          .get(UpdateSequenceNumberQuery)
          .orElse(request.userAnswers.get(ClaimApplicationResponseQuery).map(_.updateSeqNumber))

        (getApplicationId(request.userAnswers),
         request.userAnswers.get(RemoveItemNumberQuery),
         request.userAnswers.get(RemoveItemTypeQuery),
         updateSequenceNumber
        ) match {
          case (Some(applicationId), Some(itemNumber), Some("P"), Some(sequenceNumber)) =>
            val deleteRequest = DeleteItemRequest(applicationId, itemNumber, sequenceNumber)
            deleteAndRedirect(euVatRefundsService.deletePurchase(deleteRequest), "Purchase", itemNumber, value)
          case (Some(applicationId), Some(itemNumber), Some("I"), Some(sequenceNumber)) =>
            val deleteRequest = DeleteItemRequest(applicationId, itemNumber, sequenceNumber)
            deleteAndRedirect(euVatRefundsService.deleteImport(deleteRequest), "Import", itemNumber, value)
          case _ =>
            recoveryPage("Missing or invalid applicationId, itemNumber, itemType or updateSequenceNumber for delete-item")
        }
      case None =>
        recoveryPage("Missing purchase/import total item count")
    }

  private def saveChoiceAndRedirect(value: Boolean, deleteResponse: Option[DeleteItemResponse])(implicit
    request: DataRequest[AnyContent]
  ): Future[Result] =
    for {
      answersWithChoice <- Future.fromTry(request.userAnswers.set(RemoveItemPage, value))
      updatedAnswers <- deleteResponse match {
                          case Some(response) => Future.fromTry(answersWithChoice.set(UpdateSequenceNumberQuery, response.updateSequenceNumber))
                          case None           => Future.successful(answersWithChoice)
                        }
      _ <- sessionRepository.set(updatedAnswers)
    } yield Redirect(navigator.nextPage(RemoveItemPage, NormalMode, updatedAnswers))

  private def deleteAndRedirect(
    deleteResult: Future[DeleteItemResponse],
    itemType: String,
    itemNumber: Int,
    value: Boolean
  )(implicit request: DataRequest[AnyContent]): Future[Result] =
    deleteResult
      .flatMap(response => saveChoiceAndRedirect(value, Some(response)))
      .recover { case ex =>
        logger.error(s"Error deleting $itemType item $itemNumber", ex)
        Redirect(routes.JourneyRecoveryController.onPageLoad())
      }

  private def summaryValues(itemType: String, deductibleVatAmount: BigDecimal, userAnswers: models.UserAnswers)(implicit
    request: DataRequest[AnyContent]
  ): (String, String) = {
    val displayType = if (itemType == "P") "Purchase" else "Import"
    val vatClaim =
      if (deductibleVatAmount == 0) messagesApi.preferred(request)("removeItem.notProvided")
      else {
        val currencySymbol = ControllerHelpers.currencySymbolFromSession(userAnswers, currencyConfig.currencyConfig)
        s"$currencySymbol$deductibleVatAmount"
      }
    (displayType, vatClaim)
  }

  private def backLink: Call = routes.PurchaseImportSummaryController.onPageLoad

  private def recoveryPage(msg: String): Future[Result] = {
    logger.warn(msg)
    Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
  }

  private def getApplicationId(userAnswers: UserAnswers): Option[Long] =
    userAnswers.get(ClaimApplicationResponseQuery).map(_.applicationId)
}
