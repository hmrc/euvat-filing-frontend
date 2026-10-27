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

import config.FrontendAppConfig
import controllers.actions.{DataRequiredAction, DataRetrievalAction, IdentifierAction}
import models.requests.{DataRequest, UpdateImportRequest}
import models.responses.AddImportResponse
import models.{NormalMode, PurchaseOrImportType, UserAnswers}
import navigation.ImportNavigator
import pages.PurchaseImportSummaryPage
import pages.imports.*
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import queries.{ClaimApplicationResponseQuery, UpdateSequenceNumberQuery}
import repositories.SessionRepository
import services.EuVatRefundsService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import utils.{ConfigPurchaseOrImportMapping, CountryCode, CurrencyConfig}
import viewmodels.checkAnswers.CheckYourImportDetailsSummary
import views.html.imports.CheckYourImportDetailsView
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CheckYourImportDetailsController @Inject()(
                                                  override val messagesApi: MessagesApi,
                                                  identify: IdentifierAction,
                                                  getData: DataRetrievalAction,
                                                  requireData: DataRequiredAction,
                                                  val controllerComponents: MessagesControllerComponents,
                                                  view: CheckYourImportDetailsView,
                                                  configPurchaseOrImportMapping: ConfigPurchaseOrImportMapping,
                                                  currencyConfig: CurrencyConfig,
                                                  appConfig: FrontendAppConfig,
                                                  sessionRepository: SessionRepository,
                                                  importNavigator: ImportNavigator,
                                                  euVatRefundsService: EuVatRefundsService
                                                )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Logging {

  def onPageLoad(): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    val answers = request.userAnswers
    val reset = if (answers.get(ImportMissingAnswersJourneyPage).contains(true)) {
      for {
        updated <- Future.fromTry(answers.remove(ImportMissingAnswersJourneyPage))
        _ <- sessionRepository.set(updated)
      } yield updated
    } else Future.successful(answers)

    reset.map { updated =>
      Ok(view(
        CheckYourImportDetailsSummary.sections(updated, configPurchaseOrImportMapping, currencyConfig, appConfig.supplierCountries),
        routes.CheckYourImportDetailsController.onSubmit(),
        routes.TotalVatClaimController.onPageLoad(NormalMode)
      ))
    }
  }

  def onSubmit(): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    for {
      flagged <- Future.fromTry(request.userAnswers.set(ImportMissingAnswersJourneyPage, true))
      _       <- sessionRepository.set(flagged)
      result  <- importNavigator.firstMissingImportPage(flagged) match {
        case Some(missingPage) => Future.successful(Redirect(missingPage))
        case None              => saveImport(flagged)
      }
    } yield result
  }

  /** Everything is answered: send the import details to euvat-refunds (update-importation-details), keep the new
   * updateSequenceNumber and go to the purchases and imports summary.
   */
  private def saveImport(answers: UserAnswers)(implicit request: DataRequest[?]): Future[Result] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    (answers.get(ClaimApplicationResponseQuery), answers.get(AddImportResponsePage), answers.get(ImportTypePage)) match {
      case (Some(claim), Some(addResponse), Some(importType)) =>
        euVatRefundsService
          .updateImport(buildUpdateRequest(answers, claim.applicationId, addResponse, importType))
          .flatMap { response =>
            for {
              withResponse <- Future.fromTry(answers.set(AddImportResponsePage, addResponse.copy(updateSequenceNumber = response.updateSequenceNumber)))
              withSequence <- Future.fromTry(withResponse.set(UpdateSequenceNumberQuery, response.updateSequenceNumber))
              withoutList  <- Future.fromTry(withSequence.remove(PurchaseImportSummaryPage))
              completed    <- Future.fromTry(withoutList.remove(ImportMissingAnswersJourneyPage))
              _            <- sessionRepository.set(completed)
            } yield Redirect(controllers.routes.PurchaseImportSummaryController.onPageLoad)
          }
          .recover { case ex =>
            logger.error("Error updating import details", ex)
            Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
          }

      case _ =>
        logger.warn("Missing applicationId, itemNumber or import type for update-importation-details")
        Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
    }
  }

  private def buildUpdateRequest(
                                  answers: UserAnswers,
                                  applicationId: Long,
                                  addResponse: AddImportResponse,
                                  importType: PurchaseOrImportType
                                ): UpdateImportRequest = {
    def real(value: Option[String]): Option[String] =
      value.filter(v => v.trim.nonEmpty && v != ConfigPurchaseOrImportMapping.NoneValue)

    // RA5.1.2 sub code and RA5.1.3 sub-category both go in goodsDescriptionSubCategory; the more specific one wins
    val subCategory = real(answers.get(ImportSubCategoryPage)).orElse(real(answers.get(ImportSubCodePage)))

    // RA5.2.1: only the answer that belongs to the chosen route is sent, so an old answer cannot leak through
    val hasSad = answers.get(SadReferenceCheckPage)
    val address = answers.get(ImportSupplierAddressPage)

    // single-currency countries never show the currency page, so fall back to that country's only currency
    val currencyCode = answers.get(ImportCurrencyPage).orElse {
      CountryCode
        .findCountryCode(answers)
        .filterNot(currencyConfig.requiresCurrencySelection)
        .flatMap(country => currencyConfig.currencyConfig.getOrElse(country, currencyConfig.default).headOption)
        .map(_.code)
    }

    UpdateImportRequest(
      applicationId               = applicationId,
      itemNumber                  = addResponse.itemNumber,
      goodsDescriptionCategory    = PurchaseOrImportType.codes(importType),
      goodsDescriptionSubCategory = subCategory,
      goodsDescriptionText        = real(answers.get(DescribeItemsOnImportDocPage)),
      hasSadReferenceNumber       = hasSad.map(answer => if (answer) "Y" else "N"),
      sadReferenceNumber          = if (hasSad.contains(true)) real(answers.get(SadReferenceNumberPage)) else None,
      referenceInformation        = if (hasSad.contains(false)) real(answers.get(ImportDetailsInfoPage)) else None,
      supplierName                = answers.get(ImportSuppliersNamePage),
      supplierAddressLine1        = address.map(_.addressLine1),
      supplierAddressLine2        = address.flatMap(_.addressLine2),
      supplierAddressLine3        = address.flatMap(_.addressLine3),
      supplierCountryCode         = address.map(_.country),
      issuingDate                 = answers.get(ImportDatePage).map(_.atStartOfDay()),
      currencyCode                = currencyCode,
      taxableAmount               = answers.get(TotalAmountWithoutVatPage),
      vatAmount                   = answers.get(TotalVatPaidImportPage),
      deductibleVatAmount         = answers.get(ImportTotalVatClaimPage),
      updateSequenceNumber        = answers.get(UpdateSequenceNumberQuery).getOrElse(addResponse.updateSequenceNumber)
    )
  }
}