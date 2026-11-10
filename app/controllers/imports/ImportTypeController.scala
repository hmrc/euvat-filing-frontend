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

import com.google.inject.Inject
import controllers.actions.{DataRequiredAction, DataRetrievalAction, IdentifierAction}
import forms.ImportTypeFormProvider
import models.requests.{AddImportRequest, DataRequest}
import models.responses.{AddImportResponse, ImportDetailsResponse}
import models.{Mode, NormalMode, PurchaseOrImport, PurchaseOrImportType, UserAnswers}
import navigation.Navigator
import pages.PurchaseOrImportPage
import pages.imports.*
import play.api.Logging
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import queries.*
import repositories.SessionRepository
import services.EuVatRefundsService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import utils.ControllerHelpers.saveAnswerAndResetDependents
import views.html.purchasesOrImports.PurchaseOrImportTypeView

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

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

  // Answers that only make sense for the selected import type, so they are cleared when the type changes.
  private val dependentAnswers: List[Settable[?]] =
    List(ImportSubCodePage, ImportSubTypeLabelQuery, ImportSubCategoryPage, ImportSubCategoryLabelQuery, DescribeItemsOnImportDocPage)

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
    val preparedForm = request.userAnswers.get(ImportTypePage) match {
      case None        => form
      case Some(value) => form.fill(value)
    }

    Ok(view(preparedForm, mode, routes.ImportTypeController.onSubmit(mode), "importType", "import.caption", false, legendKey = None))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    form
      .bindFromRequest()
      .fold(
        formWithErrors =>
          Future.successful(
            BadRequest(
              view(formWithErrors, mode, routes.ImportTypeController.onSubmit(mode), "importType", "import.caption", false, legendKey = None)
            )
          ),
        value =>
          saveAnswerAndResetDependents(
            page              = ImportTypePage,
            newValue          = value,
            userAnswers       = request.userAnswers,
            dependents        = dependentAnswers,
            sessionRepository = sessionRepository
          )(updated => addImportIfRequired(updated, value, mode))
      )
  }

  private def addImportIfRequired(answers: UserAnswers, importType: PurchaseOrImportType, mode: Mode)(implicit
    request: DataRequest[?]
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

  def addDetails(applicationId: Long, itemNumber: Int): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

      val ua = request.userAnswers

      euVatRefundsService
        .getImportDetails(applicationId, itemNumber)
        .flatMap { response =>
          for {
            importType     <- Future.fromTry(resolveImportType(response))
            clearedAnswers <- Future.fromTry(clearStaleAnswers(ua, pagesToRemoveAnswers))
            loadedAnswers  <- Future.fromTry(loadImport(clearedAnswers, response, importType, itemNumber))
            _              <- sessionRepository.set(loadedAnswers)
          } yield Ok(
            view(
              form.fill(importType),
              NormalMode,
              routes.ImportTypeController.onSubmit(NormalMode),
              "importType",
              "import.caption",
              false,
              legendKey = None
            )
          )

        }
        .recover { case ex =>
          logger.error(s"Error fetching import details for item $itemNumber", ex)
          Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
        }
  }

  private val pagesToRemoveAnswers: List[Settable[_]] = List(
    DescribeItemsOnImportDocPage,
    ImportCurrencyPage,
    ImportDatePage,
    ImportDetailsInfoPage,
    ImportSubCategoryPage,
    ImportSubCodePage,
    ImportSupplierAddressPage,
    ImportSuppliersNamePage,
    ImportTotalVatClaimPage,
    SadReferenceCheckPage,
    SadReferenceNumberPage,
    TotalAmountWithoutVatPage,
    TotalVatPaidImportPage,
    ImportSubCategoryLabelQuery,
    ImportSubTypeLabelQuery
  )

  private def clearStaleAnswers(userAnswers: UserAnswers, pagesToRemoveAnswers: List[Settable[_]]): Try[UserAnswers] =
    pagesToRemoveAnswers.foldLeft(Try(userAnswers)) { (acc, page) =>
      acc.flatMap(_.remove(page))
    }

  private def resolveImportType(response: ImportDetailsResponse): Try[PurchaseOrImportType] =
    PurchaseOrImportType.codes.collectFirst { case (importType, code) if code == response.goodsDescriptionCategory.trim => importType } match {
      case None =>
        Failure(new IllegalStateException(s"Unknown import category ${response.goodsDescriptionCategory} for item ${response.itemNumber}"))

      case Some(importType) =>
        Success(importType)
    }

  private def loadImport(ua: UserAnswers, response: ImportDetailsResponse, importType: PurchaseOrImportType, itemNumber: Int): Try[UserAnswers] =
    ua
      .set(PurchaseOrImportPage, PurchaseOrImport.Import)
      .flatMap(_.set(ImportTypePage, importType))
      .flatMap(_.set(AddImportResponsePage, AddImportResponse(itemNumber, response.updateSequenceNumber)))

}
