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

import controllers.actions._
import forms.RemoveItemFormProvider
import javax.inject.Inject
import models.NormalMode
import navigation.Navigator
import play.api.data.Form
import pages.RemoveItemPage
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.*
import queries.IsLastItemQuery
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.RemoveItemView

import scala.concurrent.{ExecutionContext, Future}

class RemoveItemController @Inject()(
      override val messagesApi: MessagesApi,
      sessionRepository: SessionRepository,
      navigator: Navigator,
      identify: IdentifierAction,
      getData: DataRetrievalAction,
      requireData: DataRequiredAction,
      formProvider: RemoveItemFormProvider,
      val controllerComponents: MessagesControllerComponents,
      view: RemoveItemView
)(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  val form: Form[Boolean] = formProvider()

  private def backLink: Call = routes.PurchaseImportSummaryController.onPageLoad

  private def extractSummaryData(userAnswers: models.UserAnswers)(implicit messages: Messages): (String, String) = {
    ("Purchase", "€53")
  }

  def onPageLoad: Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
    implicit val messages: Messages = messagesApi.preferred(request)

    val(itemType, vatClaim) = extractSummaryData(request.userAnswers)

    Ok(view(form, itemType, vatClaim, backLink))
  }

  def onSubmit: Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    implicit val messages: Messages = messagesApi.preferred(request)

    form
       .bindFromRequest()
       .fold(
         formWithErrors => {
           val (itemType, vatClaim) = extractSummaryData(request.userAnswers)
           Future.successful(BadRequest(view(formWithErrors, itemType, vatClaim, backLink)))
         },
         value =>
           val isLastItem = false

           for {
             answersWithChoice <- Future.fromTry(request.userAnswers.set(RemoveItemPage, value))
             updatedAnswers    <- Future.fromTry(answersWithChoice.set(IsLastItemQuery, isLastItem))
             _                 <- sessionRepository.set(updatedAnswers)
           } yield Redirect(navigator.nextPage(RemoveItemPage, NormalMode, updatedAnswers))
       )
  }
  }

