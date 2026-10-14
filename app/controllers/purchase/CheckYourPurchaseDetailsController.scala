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

import controllers.actions.{DataRequiredAction, DataRetrievalAction, IdentifierAction}
import models.requests.UpdatePurchaseRequest
import models.responses.{AddPurchaseResponse, PurchaseDetailResponse}
import models.{CheckMode, InvoiceType, Mode, PurchaseOrImportType, SupplierAddress, UserAnswers}
import play.api.mvc.Call
import pages.PurchaseImportSummaryPage
import pages.purchase.*
import play.api.Logging
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.{ClaimApplicationResponseQuery, InvoiceNumberFlagQuery, PurchaseDetailResponseQuery, PurchaseDetailRoute, PurchaseDetailRouteQuery, Settable, UpdateSequenceNumberQuery}
import repositories.SessionRepository
import services.EuVatRefundsService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import utils.ControllerHelpers.setIfPresent
import utils.{ConfigPurchaseOrImportMapping, CountryCode, CurrencyConfig}
import viewmodels.checkAnswers.CheckYourPurchaseDetailsSummary
import views.html.purchase.CheckYourPurchaseDetailsView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CheckYourPurchaseDetailsController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  val controllerComponents: MessagesControllerComponents,
  view: CheckYourPurchaseDetailsView,
  currencyConfig: CurrencyConfig,
  configPurchaseMapping: ConfigPurchaseOrImportMapping,
  sessionRepository: SessionRepository,
  euVatRefundsService: EuVatRefundsService
)(using ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  private def formAction(mode: Mode): Call = controllers.purchase.routes.CheckYourPurchaseDetailsController.onSubmit()

  private def currentPurchaseDetails(userAnswers: UserAnswers): PurchaseDetailResponse = {
    val purchaseSubType = userAnswers.get(PurchaseSubTypePage)
    val purchaseSubCategory = userAnswers.get(PurchaseSubCategoryPage)

    val goodsDescriptionSubCode: Option[String] = {
      if (purchaseSubType.contains(ConfigPurchaseOrImportMapping.NoneValue) && purchaseSubCategory.contains(ConfigPurchaseOrImportMapping.NoneValue))
        None
      else if (purchaseSubCategory.exists(v => v != ConfigPurchaseOrImportMapping.NoneValue)) purchaseSubCategory
      else if (purchaseSubType.exists(v => v != ConfigPurchaseOrImportMapping.NoneValue)) purchaseSubType
      else None
    }

    val goodsDescriptionCode = userAnswers
      .get(PurchaseTypePage)
      .map(pt => PurchaseOrImportType.codes.getOrElse(pt, ""))

    val goodsDescriptionText = userAnswers.get(DescribeItemsOnInvoicePage) match {
      case Some(text) if text.trim.nonEmpty && text != ConfigPurchaseOrImportMapping.NoneValue => Some(text)
      case _                                                                                   => None
    }

    val simplifiedInvoiceIndicator = userAnswers.get(InvoiceTypePage) match {
      case Some(InvoiceType.StandardInvoice)   => None
      case Some(InvoiceType.SimplifiedInvoice) => userAnswers.get(SupplierVatRegCheckPage).map(_.toString).orElse(Some("true"))
      case None                                => userAnswers.get(SupplierVatRegCheckPage).map(_.toString)
    }

    val supplierAddress = userAnswers.get(SupplierAddressPage)

    PurchaseDetailResponse(
      goodsDescriptionCode       = goodsDescriptionCode,
      goodsDescriptionSubCode    = goodsDescriptionSubCode,
      goodsDescriptionText       = goodsDescriptionText,
      simplifiedInvoiceIndicator = simplifiedInvoiceIndicator,
      supplierName               = userAnswers.get(SuppliersNamePage),
      supplierAddressLine1       = supplierAddress.map(_.line1),
      supplierAddressLine2       = supplierAddress.flatMap(_.line2),
      supplierAddressLine3       = supplierAddress.flatMap(_.line3),
      supplierVatNumber          = userAnswers.get(SupplierVatRegistrationNumberPage),
      supplierTaxIdentifier      = userAnswers.get(SupplierTaxIdentifierNumberPage),
      invoiceDate                = userAnswers.get(InvoiceDatePage).map(_.atStartOfDay()),
      invoiceNumber              = userAnswers.get(InvoiceNumberPage),
      currencyCode               = userAnswers.get(RefundingCurrencyPage),
      taxableAmount              = userAnswers.get(TotalPurchaseAmountBeforeVatPage),
      vatAmount                  = userAnswers.get(TotalVatPaidPage),
      deductibleVatAmount        = userAnswers.get(TotalVatClaimPage),
      updateSequenceNumber = userAnswers
        .get(PurchaseDetailResponseQuery)
        .flatMap(_.updateSequenceNumber)
        .orElse(userAnswers.get(AddPurchaseResponsePage).map(_.updateSequenceNumber))
    )
  }

  private def isAmendedPurchase(userAnswers: UserAnswers): Boolean =
    userAnswers.get(PurchaseDetailRouteQuery).isDefined &&
      userAnswers.get(PurchaseDetailResponseQuery).exists(_ != currentPurchaseDetails(userAnswers))

  private def clearPurchaseAnswers(userAnswers: UserAnswers): Future[UserAnswers] = {
    var answers = Future.successful(userAnswers)
    answers = removeAnswer(answers, PurchaseTypePage)
    answers = removeAnswer(answers, PurchaseSubTypePage)
    answers = removeAnswer(answers, PurchaseSubCategoryPage)
    answers = removeAnswer(answers, DescribeItemsOnInvoicePage)
    answers = removeAnswer(answers, InvoiceTypePage)
    answers = removeAnswer(answers, InvoiceNumberPage)
    answers = removeAnswer(answers, InvoiceDatePage)
    answers = removeAnswer(answers, SuppliersNamePage)
    answers = removeAnswer(answers, SupplierAddressPage)
    answers = removeAnswer(answers, SupplierVatRegCheckPage)
    answers = removeAnswer(answers, SupplierVatRegistrationNumberPage)
    answers = removeAnswer(answers, SupplierTaxIdentifierNumberPage)
    answers = removeAnswer(answers, RefundingCurrencyPage)
    answers = removeAnswer(answers, TotalPurchaseAmountBeforeVatPage)
    answers = removeAnswer(answers, TotalVatPaidPage)
    answers = removeAnswer(answers, TotalVatClaimPage)
    answers = removeAnswer(answers, AddPurchaseResponsePage)
    answers
  }

  private def removeAnswer[A](answers: Future[UserAnswers], page: Settable[A]): Future[UserAnswers] =
    answers.flatMap(userAnswers => Future.fromTry(userAnswers.remove(page)))

  def onPageLoad(): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    implicit val msgs: Messages = messagesApi.preferred(request)

    request.userAnswers.get(PurchaseDetailRouteQuery) match
      case Some(route) =>
        Future.successful(
          Redirect(controllers.purchase.routes.CheckYourPurchaseDetailsController.changeYourAnswers(route.applicationId, route.itemNumber))
        )
      case None =>
        for {
          answers <- Future.fromTry(request.userAnswers.set(InvoiceNumberFlagQuery, false))
          _       <- sessionRepository.set(answers)
        } yield {
          lazy val currencyList = CountryCode
            .findCountryCode(request.userAnswers)
            .map(currencyConfig.currencyConfig(_))
            .getOrElse(currencyConfig.default)

          val (maybeCurrencyDisplayName, maybeCurrencySymbol): (Option[String], Option[String]) =
            request.userAnswers
              .get(RefundingCurrencyPage)
              .flatMap(code => currencyList.find(_.code == code))
              .map(currency => Some(msgs(s"refundingCurrency.${currency.name}", currency.symbol)) -> Some(currency.symbol))
              .orElse(Option.when(currencyList.lengthCompare(1) > 0)(Some(msgs("site.notProvided")) -> None))
              .getOrElse(None -> None)

          Ok(
            view(
              CheckYourPurchaseDetailsSummary.sections(
                request.userAnswers,
                maybeCurrencyDisplayName,
                maybeCurrencySymbol,
                configPurchaseMapping,
                currencyList.size > 1
              ),
              formAction(CheckMode),
              "checkYourPurchaseDetails",
              isPostSubmission = false,
              isAmended        = false
            )
          )
        }
  }

  def onSubmit(): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    val maybeAddResp = request.userAnswers.get(AddPurchaseResponsePage)
    val maybeDetailRoute = request.userAnswers.get(PurchaseDetailRouteQuery)
    val maybeAppId = maybeDetailRoute.map(_.applicationId).orElse(request.userAnswers.get(ClaimApplicationResponseQuery).map(_.applicationId))
    val maybeItemNumber = maybeDetailRoute.map(_.itemNumber).orElse(maybeAddResp.map(_.itemNumber))
    val maybeUpdateSequenceNumber = maybeDetailRoute
      .flatMap(_ => request.userAnswers.get(PurchaseDetailResponseQuery).flatMap(_.updateSequenceNumber))
      .orElse(maybeAddResp.map(_.updateSequenceNumber))

    (maybeAppId, maybeItemNumber, maybeUpdateSequenceNumber) match {
      case (Some(appId), Some(itemNumber), Some(updateSequenceNumber)) =>
        val currentDetails = currentPurchaseDetails(request.userAnswers)
        val updateReq = UpdatePurchaseRequest(
          applicationId               = appId,
          itemNumber                  = itemNumber,
          goodsDescriptionCategory    = currentDetails.goodsDescriptionCode.getOrElse(""),
          goodsDescriptionSubCategory = currentDetails.goodsDescriptionSubCode,
          goodsDescriptionText        = currentDetails.goodsDescriptionText,
          simplifiedInvoiceIndicator  = currentDetails.simplifiedInvoiceIndicator,
          supplierName                = currentDetails.supplierName,
          supplierAddress1            = currentDetails.supplierAddressLine1,
          supplierAddress2            = currentDetails.supplierAddressLine2,
          supplierAddress3            = currentDetails.supplierAddressLine3,
          supplierVatRegNumber        = currentDetails.supplierVatNumber,
          supplierTaxIdentifier       = currentDetails.supplierTaxIdentifier,
          invoiceDate                 = currentDetails.invoiceDate,
          invoiceNumber               = currentDetails.invoiceNumber,
          currencyCode                = currentDetails.currencyCode,
          taxableAmount               = currentDetails.taxableAmount,
          vatAmount                   = currentDetails.vatAmount,
          deductibleVatAmount         = currentDetails.deductibleVatAmount,
          updateSequenceNumber        = updateSequenceNumber
        )

        euVatRefundsService
          .updatePurchase(updateReq)
          .flatMap { resp =>
            val updatedAddResp = AddPurchaseResponse(itemNumber = itemNumber, updateSequenceNumber = resp.updateSequenceNumber)
            for {
              updatedAnswers1 <- Future.fromTry(request.userAnswers.set(AddPurchaseResponsePage, updatedAddResp))
              updatedAnswers2 <- Future.fromTry(updatedAnswers1.set(UpdateSequenceNumberQuery, resp.updateSequenceNumber))
              updatedAnswers3 <- Future.fromTry(updatedAnswers2.remove(PurchaseImportSummaryPage))
              updatedAnswers4 <- Future.fromTry(updatedAnswers3.remove(queries.PurchaseDetailResponseQuery))
              updatedAnswers5 <- Future.fromTry(updatedAnswers4.remove(queries.PurchaseDetailRouteQuery))
              _               <- sessionRepository.set(updatedAnswers5)
            } yield Redirect(controllers.routes.PurchaseImportSummaryController.onPageLoad)
          }
          .recover { case ex =>
            logger.error("Error updating purchase details", ex)
            Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
          }

      case _ =>
        logger.warn("Missing applicationId or itemNumber for update-purchase-details")
        Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
    }
  }

  def changeYourAnswers(applicationId: Long, itemNumber: Int): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

      request.userAnswers.get(queries.PurchaseDetailResponseQuery) match
        case Some(_) if request.userAnswers.get(PurchaseDetailRouteQuery).contains(PurchaseDetailRoute(applicationId, itemNumber)) =>
          implicit val msgs = messagesApi.preferred(request)

          lazy val currencyList = CountryCode
            .findCountryCode(request.userAnswers)
            .flatMap(code => currencyConfig.currencyConfig.get(code))
            .getOrElse(currencyConfig.default)

          val (maybeCurrencyDisplayName, maybeCurrencySymbol): (Option[String], Option[String]) =
            request.userAnswers
              .get(RefundingCurrencyPage)
              .flatMap(code => currencyList.find(_.code == code))
              .map(currency => Some(msgs(s"refundingCurrency.${currency.name}", currency.symbol)) -> Some(currency.symbol))
              .orElse(Option.when(currencyList.lengthCompare(1) > 0)(Some(msgs("site.notProvided")) -> None))
              .getOrElse(None -> None)

          val showCurrencyRow = CountryCode.findCountryCode(request.userAnswers).exists(cc => currencyConfig.requiresCurrencySelection(cc))
          val isAmended = isAmendedPurchase(request.userAnswers)
          val sections = CheckYourPurchaseDetailsSummary.sections(request.userAnswers,
                                                                  maybeCurrencyDisplayName,
                                                                  maybeCurrencySymbol,
                                                                  configPurchaseMapping,
                                                                  showCurrencyRow
                                                                 )(msgs, request)
          Future.successful(
            Ok(view(sections, formAction(CheckMode), "changeYourPurchaseDetails", isPostSubmission = false, isAmended = isAmended))
          )

        case _ =>
          euVatRefundsService
            .getPurchaseDetails(applicationId, itemNumber)
            .flatMap { resp =>
              implicit val msgs = messagesApi.preferred(request)

              val countryCodeOpt = CountryCode.findCountryCode(request.userAnswers)

              val currencyList = countryCodeOpt.flatMap(code => currencyConfig.currencyConfig.get(code)).getOrElse(currencyConfig.default)

              val purchaseTypeOpt: Option[models.PurchaseOrImportType] = resp.goodsDescriptionCode.flatMap { code =>
                PurchaseOrImportType.codes.find(_._2 == code).map(_._1)
              }

              val invoiceTypeOpt: Option[InvoiceType] =
                Some(if (resp.simplifiedInvoiceIndicator.isDefined) InvoiceType.SimplifiedInvoice else InvoiceType.StandardInvoice)
              val supplierVatRegCheckOpt: Option[Boolean] =
                resp.simplifiedInvoiceIndicator.map(_.equalsIgnoreCase("true"))

              val supplierAddressOpt: Option[SupplierAddress] =
                resp.supplierAddressLine1.map(l1 => SupplierAddress(l1, resp.supplierAddressLine2, resp.supplierAddressLine3))

              val mappedUAF: Future[UserAnswers] = clearPurchaseAnswers(request.userAnswers).flatMap { clearedAnswers =>
                var f: Future[UserAnswers] = Future.successful(clearedAnswers)

                f = setIfPresent(f, purchaseTypeOpt, PurchaseTypePage)
                f = setIfPresent(f, resp.goodsDescriptionSubCode, PurchaseSubTypePage)
                f = setIfPresent(f, resp.goodsDescriptionText, DescribeItemsOnInvoicePage)
                f = setIfPresent(f, invoiceTypeOpt, InvoiceTypePage)
                f = setIfPresent(f, resp.invoiceNumber, InvoiceNumberPage)
                f = setIfPresent(f, resp.invoiceDate.map(_.toLocalDate), InvoiceDatePage)
                f = setIfPresent(f, resp.supplierName, SuppliersNamePage)
                f = setIfPresent(f, supplierAddressOpt, SupplierAddressPage)
                f = f.flatMap(ua => Future.fromTry(ua.remove(SupplierVatRegCheckPage)))
                f = setIfPresent(f, supplierVatRegCheckOpt, SupplierVatRegCheckPage)
                f = setIfPresent(f, resp.supplierVatNumber.flatMap(s => Option(s).filter(_.nonEmpty)), SupplierVatRegistrationNumberPage)
                f = setIfPresent(f, resp.supplierTaxIdentifier.flatMap(s => Option(s).filter(_.nonEmpty)), SupplierTaxIdentifierNumberPage)
                f = setIfPresent(f, resp.currencyCode, RefundingCurrencyPage)
                f = setIfPresent(f, resp.taxableAmount, TotalPurchaseAmountBeforeVatPage)
                f = setIfPresent(f, resp.vatAmount, TotalVatPaidPage)
                f = setIfPresent(f, resp.deductibleVatAmount, TotalVatClaimPage)
                f = f.flatMap(ua =>
                  Future.fromTry(ua.set(AddPurchaseResponsePage, AddPurchaseResponse(itemNumber, resp.updateSequenceNumber.getOrElse(1))))
                )

                f = f.flatMap(ua => Future.fromTry(ua.set(queries.PurchaseDetailResponseQuery, resp)))
                f = f.flatMap(ua => Future.fromTry(ua.set(queries.PurchaseDetailRouteQuery, PurchaseDetailRoute(applicationId, itemNumber))))

                f
              }

              mappedUAF.flatMap { ua =>
                implicit val reqHeader = request

                sessionRepository.set(ua).map { _ =>
                  lazy val currencyListFromUA = CountryCode
                    .findCountryCode(ua)
                    .flatMap(code => currencyConfig.currencyConfig.get(code))
                    .getOrElse(currencyConfig.default)

                  val (maybeCurrencyDisplayName, maybeCurrencySymbol): (Option[String], Option[String]) =
                    ua
                      .get(RefundingCurrencyPage)
                      .flatMap(code => currencyListFromUA.find(_.code == code))
                      .map(currency => Some(msgs(s"refundingCurrency.${currency.name}", currency.symbol)) -> Some(currency.symbol))
                      .orElse(Option.when(currencyListFromUA.lengthCompare(1) > 0)(Some(msgs("site.notProvided")) -> None))
                      .getOrElse(None -> None)

                  val showCurrencyRow = CountryCode.findCountryCode(ua).exists(cc => currencyConfig.requiresCurrencySelection(cc))
                  val isAmended = isAmendedPurchase(ua)
                  val sections = CheckYourPurchaseDetailsSummary
                    .sections(ua, maybeCurrencyDisplayName, maybeCurrencySymbol, configPurchaseMapping, showCurrencyRow)(msgs, request)
                  Ok(
                    view(sections, formAction(CheckMode), "changeYourPurchaseDetails", isPostSubmission = false, isAmended = isAmended)
                  )
                }
              }
            }
            .recover { case ex =>
              logger.error("Error fetching purchase details", ex)
              Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
            }
  }
}
