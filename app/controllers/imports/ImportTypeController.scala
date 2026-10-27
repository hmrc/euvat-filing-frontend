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

import pages.imports.*
import queries.*
import scala.util.Success

import com.google.inject.Inject
import controllers.actions.{DataRequiredAction, DataRetrievalAction, IdentifierAction}
import controllers.imports.routes as importRoutes
import forms.ImportTypeFormProvider
import models.requests.{AddImportRequest, DataRequest}
import models.{CheckMode, Mode, NormalMode, PurchaseOrImportType, UserAnswers}
import navigation.Navigator
import pages.imports.ImportTypePage
import play.api.Logging
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import repositories.SessionRepository
import services.EuVatRefundsService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import views.html.purchasesOrImports.PurchaseOrImportTypeView

import scala.concurrent.{ExecutionContext, Future}

class ImportTypeController @Inject() (
                                       override val messagesApi: MessagesApi,
                                       sessionRepository: SessionRepository,
                                       navigator: Navigator,
                                       identify: IdentifierAction,
                                       getData: DataRetrievalAction,
                                       requireData: DataRequiredAction,
                                       formProvider: ImportTypeFormProvider,
                                       val controllerComponents: MessagesControllerComponents,
                                       view: PurchaseOrImportTypeView,
                                       euVatRefundsService: EuVatRefundsService
                                     )(implicit ec: ExecutionContext)
  extends FrontendBaseController
    with I18nSupport
    with Logging {

  val form: Form[PurchaseOrImportType] = formProvider()

  private def backLink(mode: Mode)(implicit request: DataRequest[?]) =
    if (mode == CheckMode) importRoutes.CheckYourImportDetailsController.onPageLoad()
    else controllers.routes.PurchaseOrImportController.onPageLoad

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
    val preparedForm = request.userAnswers.get(ImportTypePage) match {
      case None        => form
      case Some(value) => form.fill(value)
    }

    Ok(view(preparedForm, mode, backLink(mode), routes.ImportTypeController.onSubmit(mode), "importType", "import.caption", false, legendKey = None))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    form
      .bindFromRequest()
      .fold(
        formWithErrors =>
          Future.successful(
            BadRequest(
              view(formWithErrors,
                mode,
                backLink(mode),
                routes.ImportTypeController.onSubmit(mode),
                "importType",
                "import.caption",
                false,
                legendKey = None
              )
            )
          ),
        value => {
          val changed = !request.userAnswers.isAnswerUnchanged(ImportTypePage, value)
          val cleared: scala.util.Try[models.UserAnswers] =
            if (changed)
              request.userAnswers
                .remove(ImportSubCodePage)
                .flatMap(_.remove(ImportSubTypeLabelQuery))
                .flatMap(_.remove(ImportSubCategoryPage))
                .flatMap(_.remove(ImportSubCategoryLabelQuery))
                .flatMap(_.remove(DescribeItemsOnImportDocPage))
            else Success(request.userAnswers)
          for {
            base    <- Future.fromTry(cleared)
            updated <- Future.fromTry(base.set(ImportTypePage, value).flatMap(_.set(ImportTypeChangedPage, changed)))
            _       <- sessionRepository.set(updated)
            result  <- addImportIfRequired(updated, value, mode)
          } yield result
        }
      )
  }

  private def addImportIfRequired(answers: UserAnswers, importType: PurchaseOrImportType, mode: Mode)(implicit request: DataRequest[?]
  ): Future[Result] = {
    def onward(ua: UserAnswers): Result = Redirect(navigator.nextPage(ImportTypePage, mode, ua))

    (mode, answers.get(ClaimApplicationResponseQuery), answers.get(AddImportResponsePage)) match {
      case (NormalMode, Some(claim), None) =>
        implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
        val addRequest = AddImportRequest(
          applicationId            = claim.applicationId,
          goodsDescriptionCategory = PurchaseOrImportType.codes(importType),
          updateSequenceNumber     = answers.get(UpdateSequenceNumberQuery).getOrElse(claim.updateSeqNumber)
        )
        euVatRefundsService
          .addImport(addRequest)
          .flatMap { response =>
            for {
              withResponse <- Future.fromTry(answers.set(AddImportResponsePage, response))
              withSequence <- Future.fromTry(withResponse.set(UpdateSequenceNumberQuery, response.updateSequenceNumber))
              _            <- sessionRepository.set(withSequence)
            } yield onward(withSequence)
          }
          .recover { case ex =>
            logger.error("Error while adding the import", ex)
            Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
          }
      case _ =>
        Future.successful(onward(answers))
    }
  }
}
