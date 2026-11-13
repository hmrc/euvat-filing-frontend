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

package controllers.purchase

import controllers.actions.*
import forms.purchase.PurchaseTypeFormProvider
import models.*
import models.requests.{AddPurchaseRequest, DataRequest}
import models.responses.{AddPurchaseResponse, PurchaseDetailsResponse}
import navigation.Navigator
import pages.PurchaseOrImportPage
import pages.purchase.*
import play.api.Logging
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.*
import play.twirl.api.HtmlFormat
import queries.*
import repositories.SessionRepository
import services.EuVatRefundsService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import utils.{ConfigPurchaseOrImportMapping, CountryCode, MountPrefix}
import views.html.purchasesOrImports.PurchaseOrImportTypeView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class PurchaseTypeController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  config: ConfigPurchaseOrImportMapping,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: PurchaseTypeFormProvider,
  val controllerComponents: MessagesControllerComponents,
  euVatRefundsService: EuVatRefundsService,
  view: PurchaseOrImportTypeView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  val form: Form[PurchaseOrImportType] = formProvider()

  private val arrivalFlags: List[Settable[?]] = List(
    DescribeItemsArrivedFromCheckYourAnswersQuery,
    PurchaseSubTypeArrivedFromCheckYourAnswersQuery,
    PurchaseSubCategoryArrivedFromCheckYourAnswersQuery
  )

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
    val preparedForm = request.userAnswers.get(PurchaseTypePage).fold(form)(form.fill)
    Ok(renderView(preparedForm, mode))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    form
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(renderView(formWithErrors, mode))),
        value => {
          val previous = request.userAnswers.get(PurchaseTypePage)
          if (mode == CheckMode && previous.contains(value)) {
            handleUnchangedCheckModeSubmission(value)
          } else {
            handleSubmissionWhenChangedOrNormal(value, mode)
          }
        }
      )
  }

  private def renderView(form: Form[PurchaseOrImportType], mode: Mode)(implicit request: DataRequest[?]): HtmlFormat.Appendable =
    view(form, mode, routes.PurchaseTypeController.onSubmit(mode), "purchaseType", "purchase.caption", true, legendKey = Some("purchaseType.h2"))

  private def redirectWithPrefix(call: Call)(implicit request: RequestHeader): Result = {
    val prefix = MountPrefix.getFromRequest
    if (prefix.isEmpty || call.url.startsWith(prefix)) {
      Redirect(call)
    } else {
      Redirect(Call(call.method, s"$prefix${call.url}"))
    }
  }

  private def arrivedFromDescribe(implicit request: DataRequest[?]): Boolean =
    request.userAnswers.get(DescribeItemsArrivedFromCheckYourAnswersQuery).contains(true)

  private def arrivedFromSubTypeOrCategory(implicit request: DataRequest[?]): Boolean =
    request.userAnswers.get(PurchaseSubTypeArrivedFromCheckYourAnswersQuery).contains(true) ||
      request.userAnswers.get(PurchaseSubCategoryArrivedFromCheckYourAnswersQuery).contains(true)

  private def describePresent(implicit request: DataRequest[?]): Boolean =
    request.userAnswers.get(DescribeItemsOnInvoicePage).exists(_.trim.nonEmpty)

  private def isNoneSubTypeSelection(subType: String): Boolean =
    subType == ConfigPurchaseOrImportMapping.NoneValue || subType.split("\\.").lastOption.contains("99")

  private def shouldReturnToDescribe(answers: UserAnswers, value: PurchaseOrImportType): Boolean =
    value == models.Other && answers.get(PurchaseSubTypePage).exists(isNoneSubTypeSelection)

  private def subcodesFor(answers: UserAnswers, value: PurchaseOrImportType): Option[Seq[(String, String)]] =
    CountryCode.findCountryCode(answers).flatMap(country => Try(config.subcodesFor(country, value.toString)).toOption)

  private def hasSubcodesFor(answers: UserAnswers, value: PurchaseOrImportType): Boolean =
    subcodesFor(answers, value).forall(_.nonEmpty)

  private def hasMeaningfulSubcodes(value: PurchaseOrImportType)(implicit request: DataRequest[?]): Boolean =
    subcodesFor(request.userAnswers, value).forall(_.exists { case (code, _) => !code.split("\\.").lastOption.contains("99") })

  private def handleUnchangedCheckModeSubmission(value: PurchaseOrImportType)(implicit request: DataRequest[?]): Future[Result] = {
    val returnToDescribe =
      arrivedFromDescribe && !arrivedFromSubTypeOrCategory &&
        shouldReturnToDescribe(request.userAnswers, value) &&
        (describePresent || hasMeaningfulSubcodes(value))

    if (returnToDescribe) {
      Future.fromTry(request.userAnswers.remove(DescribeItemsArrivedFromCheckYourAnswersQuery)).flatMap { ua =>
        sessionRepository.set(ua).map(_ => redirectWithPrefix(routes.DescribeItemsOnInvoiceController.onPageLoad(CheckMode)))
      }
    } else {
      Future.successful(Redirect(routes.CheckYourPurchaseDetailsController.onPageLoad()))
    }
  }

  private def handleSubmissionWhenChangedOrNormal(value: PurchaseOrImportType, mode: Mode)(implicit request: DataRequest[?]): Future[Result] = {
    val withValue = request.userAnswers.get(PurchaseTypePage) match {
      case Some(prev) if prev != value => buildUpdatedTryForPurchaseTypeChange(value)
      case _                           => request.userAnswers.set(PurchaseTypePage, value)
    }
    if (mode == CheckMode) saveCheckModeChange(withValue, value) else saveAndContinue(withValue, value)
  }

  private def buildUpdatedTryForPurchaseTypeChange(value: PurchaseOrImportType)(implicit request: DataRequest[?]): Try[UserAnswers] =
    for {
      afterRemovedSubType        <- request.userAnswers.remove(PurchaseSubTypePage)
      afterRemovedSubTypeLabel   <- afterRemovedSubType.remove(PurchaseSubTypeLabelPage)
      afterRemovedSubCategory    <- afterRemovedSubTypeLabel.remove(PurchaseSubCategoryPage)
      afterRemovedSubCategoryLbl <- afterRemovedSubCategory.remove(PurchaseSubCategoryLabelPage)
      afterRemovedDescribe       <- afterRemovedSubCategoryLbl.remove(DescribeItemsOnInvoicePage)
      afterSetPurchaseType       <- afterRemovedDescribe.set(PurchaseTypePage, value)
    } yield afterSetPurchaseType

  private def saveCheckModeChange(withValue: Try[UserAnswers], value: PurchaseOrImportType)(implicit request: DataRequest[?]): Future[Result] =
    for {
      answers <- Future.fromTry(withValue)
      destination = checkModeDestination(answers, value)
      cleared <- Future.fromTry(clearStaleAnswers(answers, arrivalFlags))
      _       <- sessionRepository.set(cleared)
    } yield redirectWithPrefix(destination)

  private def checkModeDestination(answers: UserAnswers, value: PurchaseOrImportType): Call =
    if (!hasSubcodesFor(answers, value)) {
      routes.CheckYourPurchaseDetailsController.onPageLoad()
    } else if (answers.get(DescribeItemsArrivedFromCheckYourAnswersQuery).contains(true) && shouldReturnToDescribe(answers, value)) {
      routes.DescribeItemsOnInvoiceController.onPageLoad(CheckMode)
    } else {
      routes.PurchaseSubTypeController.onPageLoad(PurchaseOrImportType.urlSlugForPurchaseType(value), CheckMode)
    }

  private def saveAndContinue(withValue: Try[UserAnswers], value: PurchaseOrImportType)(implicit request: DataRequest[?]): Future[Result] =
    for {
      answers <- Future.fromTry(withValue)
      _       <- sessionRepository.set(answers)
      result <-
        if (answers.get(ClaimApplicationResponseQuery).isDefined) addPurchaseAndPersist(answers, value, NormalMode)
        else handleNormalModeRedirect(answers, NormalMode)
    } yield result

  private def handleNormalModeRedirect(updatedAnswers: UserAnswers, mode: Mode)(implicit request: DataRequest[?]): Future[Result] =
    Future.successful(redirectWithPrefix(navigator.nextPage(PurchaseTypePage, mode, updatedAnswers)))

  private def addPurchaseAndPersist(
    answers: UserAnswers,
    purchaseType: PurchaseOrImportType,
    mode: Mode
  )(implicit request: DataRequest[?]): Future[Result] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    def onward(ua: UserAnswers): Result = Redirect(navigator.nextPage(PurchaseTypePage, mode, ua))

    (mode, answers.get(ClaimApplicationResponseQuery), answers.get(AddPurchaseResponsePage)) match {
      case (NormalMode, Some(claimResponse), None) =>
        euVatRefundsService
          .addPurchase(AddPurchaseRequest.from(answers, purchaseType, claimResponse))
          .flatMap(addPurchaseResponse => persistAddPurchaseResponseToSession(answers, addPurchaseResponse))
          .map(updatedAnswers => redirectWithPrefix(navigator.nextPage(PurchaseTypePage, mode, updatedAnswers)))
          .recover { case ex =>
            logger.error("Error while adding the purchase", ex)
            Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
          }
      case _ =>
        Future.successful(onward(answers))
    }
  }

  private def persistAddPurchaseResponseToSession(answers: UserAnswers, response: AddPurchaseResponse) =
    for {
      updatedAnswers1 <- Future.fromTry(answers.set(AddPurchaseResponsePage, response))
      updatedAnswers2 <- Future.fromTry(updatedAnswers1.set(UpdateSequenceNumberQuery, response.updateSequenceNumber))
      _               <- sessionRepository.set(updatedAnswers2)
    } yield updatedAnswers2

  def addDetails(applicationId: Long, itemNumber: Int): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

      val ua = request.userAnswers

      euVatRefundsService
        .getPurchaseDetails(applicationId, itemNumber)
        .flatMap { response =>
          for {
            purchaseType   <- Future.fromTry(resolvePurchaseType(response, itemNumber))
            clearedAnswers <- Future.fromTry(clearStaleAnswers(ua, pagesToRemoveAnswers))
            loadedAnswers  <- Future.fromTry(loadPurchase(clearedAnswers, response, purchaseType, itemNumber))
            _              <- sessionRepository.set(loadedAnswers)
          } yield Ok(renderView(form.fill(purchaseType), NormalMode))
        }
        .recover { case ex =>
          logger.error(s"Error fetching purchase details for item $itemNumber", ex)
          Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
        }
  }

  private val pagesToRemoveAnswers: List[Settable[_]] = List(
    CurrencyChangedPage,
    DescribeItemsArrivedFromCheckYourAnswersQuery,
    DescribeItemsOnInvoicePage,
    InvoiceDatePage,
    InvoiceNumberPage,
    InvoiceTypeChangedPage,
    InvoiceTypePage,
    PurchaseSubCategoryArrivedFromCheckYourAnswersQuery,
    PurchaseSubCategoryLabelPage,
    PurchaseSubCategoryPage,
    PurchaseSubTypeArrivedFromCheckYourAnswersQuery,
    PurchaseSubTypeLabelPage,
    PurchaseSubTypePage,
    RefundingCurrencyPage,
    SupplierAddressPage,
    SuppliersNamePage,
    SupplierTaxIdentifierNumberPage,
    SupplierTaxNumberCheckPage,
    SupplierVatRegCheckPage,
    SupplierVatRegistrationNumberPage,
    TotalPurchaseAmountBeforeVatPage,
    TotalVatClaimPage,
    TotalVatPaidPage,
    PurchaseDetailResponseQuery,
    PurchaseDetailRouteQuery,
    InvoiceNumberFlagQuery
  )

  private def clearStaleAnswers(userAnswers: UserAnswers, pagesToRemoveAnswers: List[Settable[_]]): Try[UserAnswers] =
    pagesToRemoveAnswers.foldLeft(Try(userAnswers)) { (acc, page) =>
      acc.flatMap(_.remove(page))
    }

  private def resolvePurchaseType(response: PurchaseDetailsResponse, itemNumber: Int): Try[PurchaseOrImportType] =
    PurchaseOrImportType.codes.collectFirst { case (importType, code) if code == response.goodsDescriptionCode.trim => importType } match {
      case None =>
        Failure(new IllegalStateException(s"Unknown import category ${response.goodsDescriptionCode} for item $itemNumber"))

      case Some(importType) =>
        Success(importType)
    }

  private def loadPurchase(ua: UserAnswers,
                           response: PurchaseDetailsResponse,
                           purchaseType: PurchaseOrImportType,
                           itemNumber: Int
                          ): Try[UserAnswers] =
    ua
      .set(PurchaseOrImportPage, PurchaseOrImport.Purchase)
      .flatMap(_.set(PurchaseTypePage, purchaseType))
      .flatMap(_.set(AddPurchaseResponsePage, AddPurchaseResponse(itemNumber, response.updateSequenceNumber)))
}
