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
import forms.imports.ImportDetailsInfoFormProvider
import models.requests.DataRequest
import javax.inject.Inject
import models.{Mode, UserAnswers}
import queries.ClaimApplicationResponseQuery
import pages.purchase.AddPurchaseResponsePage
import navigation.Navigator
import services.EuVatRefundsService
import controllers.warning.routes as warningRoutes
import models.requests.DuplicateReferenceInformationCountRequest
import controllers.routes.JourneyRecoveryController
import pages.imports.{ImportDetailsInfoPage, SadReferenceCheckPage, SadReferenceNumberPage}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.Logging
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.imports.ImportDetailsInfoView
import play.api.data.Form

import scala.concurrent.{ExecutionContext, Future}

class ImportDetailsInfoController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: ImportDetailsInfoFormProvider,
  euVatRefundsService: EuVatRefundsService,
  val controllerComponents: MessagesControllerComponents,
  view: ImportDetailsInfoView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  val form: Form[String] = formProvider()

  private def backLink(mode: Mode)(implicit request: DataRequest[?]) =
    request.userAnswers.get(SadReferenceCheckPage) match {
      case Some(true) =>
        request.userAnswers.get(SadReferenceNumberPage) match {
          case Some(_) => controllers.imports.routes.SadReferenceNumberController.onPageLoad(mode)
          case None    => controllers.imports.routes.SadReferenceCheckController.onPageLoad(mode)
        }
      case _ => controllers.imports.routes.SadReferenceCheckController.onPageLoad(mode)
    }

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>

    val preparedForm = request.userAnswers.get(ImportDetailsInfoPage) match {
      case None        => form
      case Some(value) => form.fill(value)
    }

    Ok(view(preparedForm, mode, backLink(mode)))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>

    form
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, mode, backLink(mode)))),
        value =>
          buildDuplicateReferenceInformationCountRequest(request.userAnswers) match {
            case Some(countRequest) =>
              euVatRefundsService
                .getDuplicateReferenceInformationCount(countRequest)
                .flatMap { response =>
                  for {
                    updatedAnswers <- Future.fromTry(request.userAnswers.set(ImportDetailsInfoPage, value))
                    _              <- sessionRepository.set(updatedAnswers)
                  } yield {
                    if (response.count > 0) {
                      Redirect(warningRoutes.ImportDetailsWarningController.onPageLoad(mode))
                    } else {
                      Redirect(navigator.nextPage(ImportDetailsInfoPage, mode, updatedAnswers))
                    }
                  }
                }
                .recover { case ex =>
                  logger.error("error while retrieving duplicate reference information count", ex)
                  Redirect(JourneyRecoveryController.onPageLoad())
                }
            case _ =>
              logger.warn("Missing session data")
              Future.successful(Redirect(JourneyRecoveryController.onPageLoad()))
          }
      )
  }

  private def buildDuplicateReferenceInformationCountRequest(answers: UserAnswers): Option[DuplicateReferenceInformationCountRequest] =
    for {
      applicationId <- answers.get(ClaimApplicationResponseQuery).map(_.applicationId)
      itemNumber    <- answers.get(AddPurchaseResponsePage).map(_.itemNumber)
    } yield DuplicateReferenceInformationCountRequest(applicationId, itemNumber)


}
