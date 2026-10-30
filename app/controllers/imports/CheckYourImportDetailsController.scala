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
import models.requests.{DataRequest, UpdateImportDetailsRequest}
import models.responses.{AddImportResponse, ImportDetailsResponse}
import models.{ImportSupplierAddress, NormalMode, Other, PurchaseOrImport, PurchaseOrImportType, UserAnswers}
import pages.{PurchaseImportSummaryPage, PurchaseOrImportPage}
import pages.imports.*
import play.api.Logging
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.libs.json.Writes
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import queries.{ClaimApplicationResponseQuery, ImportDetailsResponseQuery, ImportDetailsRoute, ImportDetailsRouteQuery, ImportSubCategoryLabelQuery, ImportSubTypeLabelQuery, Settable, UpdateSequenceNumberQuery}
import repositories.SessionRepository
import services.EuVatRefundsService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import utils.{ConfigPurchaseOrImportMapping, CountryCode, CurrencyConfig, PurchaseOrImportHelpers}
import viewmodels.checkAnswers.CheckYourImportDetailsSummary
import views.html.imports.CheckYourImportDetailsView
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class CheckYourImportDetailsController @Inject() (
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
                                                   euVatRefundsService: EuVatRefundsService
                                                 )(implicit ec: ExecutionContext)
  extends FrontendBaseController
    with I18nSupport
    with Logging {

  private val checkPageType  = "checkYourImportDetails"
  private val changePageType = "changeYourImportDetails"

  def onPageLoad(): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
    request.userAnswers.get(ImportDetailsRouteQuery) match {
      // a saved import is being changed, so CheckMode pages coming back here return to change your answers
      case Some(route) =>
        Redirect(routes.CheckYourImportDetailsController.changeYourAnswers(route.applicationId, route.itemNumber))

      case None =>
        Ok(
          view(
            sections(request.userAnswers),
            routes.CheckYourImportDetailsController.onSubmit(),
            routes.TotalVatClaimController.onPageLoad(NormalMode),
            checkPageType,
            isAmended = false
          )
        )
    }
  }

  // RA3.2.1 Change link for a saved import: load it once, then show it as change your answers
  def changeYourAnswers(applicationId: Long, itemNumber: Int): Action[AnyContent] =
    (identify andThen getData andThen requireData).async { implicit request =>
      implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

      val answers = request.userAnswers
      val route   = ImportDetailsRoute(applicationId, itemNumber)

      if (!answers.get(ClaimApplicationResponseQuery).map(_.applicationId).contains(applicationId)) {
        logger.warn(s"Change your answers requested for application $applicationId which is not the claim in session")
        Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
      } else if (answers.get(ImportDetailsRouteQuery).contains(route) && answers.get(ImportDetailsResponseQuery).isDefined) {
        Future.successful(changeView(answers))
      } else {
        euVatRefundsService
          .getImportDetails(applicationId, itemNumber)
          .flatMap { response =>
            for {
              loaded    <- Future.fromTry(loadImport(answers, response))
              withRoute <- Future.fromTry(loaded.set(ImportDetailsRouteQuery, route).flatMap(_.set(ImportDetailsResponseQuery, response)))
              _         <- sessionRepository.set(withRoute)
            } yield changeView(withRoute)
          }
          .recover { case ex =>
            logger.error(s"Error fetching import details for item $itemNumber", ex)
            Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
          }
      }
    }

  def onSubmit(): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    saveImport(request.userAnswers)
  }

  private def sections(answers: UserAnswers)(implicit request: DataRequest[?]) =
    CheckYourImportDetailsSummary.sections(answers, configPurchaseOrImportMapping, currencyConfig, appConfig.supplierCountries)

  private def changeView(answers: UserAnswers)(implicit request: DataRequest[?]): Result =
    Ok(
      view(
        sections(answers),
        routes.CheckYourImportDetailsController.onSubmit(),
        controllers.routes.PurchaseImportSummaryController.onPageLoad,
        changePageType,
        isAmended(answers)
      )
    )

  private def saveImport(answers: UserAnswers)(implicit request: DataRequest[?]): Future[Result] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    (answers.get(ClaimApplicationResponseQuery), answers.get(AddImportResponsePage), answers.get(ImportTypePage)) match {
      case (Some(claim), Some(addResponse), Some(importType)) =>
        euVatRefundsService
          .updateImport(buildUpdateRequest(answers, claim.applicationId, addResponse, importType))
          .flatMap { response =>
            for {
              withResponse <-
                Future.fromTry(answers.set(AddImportResponsePage, addResponse.copy(updateSequenceNumber = response.updateSequenceNumber)))
              withSequence <- Future.fromTry(withResponse.set(UpdateSequenceNumberQuery, response.updateSequenceNumber))
              withoutRoute <- Future.fromTry(withSequence.remove(ImportDetailsRouteQuery).flatMap(_.remove(ImportDetailsResponseQuery)))
              completed    <- Future.fromTry(withoutRoute.remove(PurchaseImportSummaryPage))
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

  private def updateRequest(answers: UserAnswers): Option[UpdateImportDetailsRequest] =
    for {
      claim       <- answers.get(ClaimApplicationResponseQuery)
      addResponse <- answers.get(AddImportResponsePage)
      importType  <- answers.get(ImportTypePage)
    } yield buildUpdateRequest(answers, claim.applicationId, addResponse, importType)

  private def isAmended(answers: UserAnswers)(implicit messages: Messages): Boolean =
    answers.get(ImportDetailsResponseQuery).exists { original =>
      val before = loadImport(answers, original).toOption.flatMap(updateRequest).map(_.copy(updateSequenceNumber = 0))
      val now    = updateRequest(answers).map(_.copy(updateSequenceNumber = 0))
      before != now
    }

  private val importAnswerPages: List[Settable[?]] = List(
    ImportTypePage,
    ImportSubCodePage,
    ImportSubTypeLabelQuery,
    ImportSubCategoryPage,
    ImportSubCategoryLabelQuery,
    DescribeItemsOnImportDocPage,
    SadReferenceCheckPage,
    SadReferenceNumberPage,
    ImportDetailsInfoPage,
    ImportDatePage,
    ImportSuppliersNamePage,
    ImportSupplierAddressPage,
    ImportCurrencyPage,
    TotalAmountWithoutVatPage,
    TotalVatPaidImportPage,
    ImportTotalVatClaimPage,
    AddImportResponsePage
  )

  private def loadImport(answers: UserAnswers, response: ImportDetailsResponse)(implicit messages: Messages): Try[UserAnswers] =
    PurchaseOrImportType.codes.collectFirst { case (importType, code) if code == response.goodsDescriptionCategory.trim => importType } match {
      case None =>
        Failure(new IllegalStateException(s"Unknown import category ${response.goodsDescriptionCategory} for item ${response.itemNumber}"))

      case Some(importType) =>
        importAnswerPages.foldLeft(Try(answers))((acc, page) => acc.flatMap(_.remove(page))).flatMap { cleared =>
          val country  = CountryCode.findCountryCode(cleared)
          val currency = response.currencyCode.filter(_ => country.exists(currencyConfig.requiresCurrencySelection))
          val hasSad = response.hasSadReferenceNumber.map(_.trim.toLowerCase).collect {
            case "true" | "y"  => true
            case "false" | "n" => false
          }
          val address = for {
            line1       <- text(response.supplierAddressLine1)
            countryCode <- text(response.supplierCountryCode)
          } yield ImportSupplierAddress(line1, text(response.supplierAddressLine2), text(response.supplierAddressLine3), countryCode)

          cleared
            .set(ImportTypePage, importType)
            .flatMap(restoreImportCategoryAnswers(_, importType, text(response.goodsDescriptionSubcategory), country))
            .flatMap(setIfPresent(_, DescribeItemsOnImportDocPage, text(response.goodsDescriptionText)))
            .flatMap(setIfPresent(_, SadReferenceCheckPage, hasSad))
            .flatMap(setIfPresent(_, SadReferenceNumberPage, text(response.sadReferenceNumber)))
            .flatMap(setIfPresent(_, ImportDetailsInfoPage, text(response.referenceInformation)))
            .flatMap(setIfPresent(_, ImportDatePage, response.issuingDate.map(_.toLocalDate)))
            .flatMap(setIfPresent(_, ImportSuppliersNamePage, text(response.supplierName)))
            .flatMap(setIfPresent(_, ImportSupplierAddressPage, address))
            .flatMap(setIfPresent(_, ImportCurrencyPage, currency))
            .flatMap(setIfPresent(_, TotalAmountWithoutVatPage, response.taxableAmount))
            .flatMap(setIfPresent(_, TotalVatPaidImportPage, response.vatAmount))
            .flatMap(setIfPresent(_, ImportTotalVatClaimPage, response.deductibleVatAmount))
            .flatMap(_.set(AddImportResponsePage, AddImportResponse(response.itemNumber, response.updateSequenceNumber)))
            .flatMap(_.set(UpdateSequenceNumberQuery, response.updateSequenceNumber))
            .flatMap(_.set(PurchaseOrImportPage, PurchaseOrImport.Import))
        }
    }

  private def restoreImportCategoryAnswers(
                                            answers: UserAnswers,
                                            importType: PurchaseOrImportType,
                                            subCode: Option[String],
                                            country: Option[String]
                                          )(implicit messages: Messages): Try[UserAnswers] =
    (subCode, country) match {
      case (Some(selectedCode), Some(countryCode)) =>
        val parentKey = importType.toString
        val subtypes  = configPurchaseOrImportMapping.subcodesFor(countryCode, parentKey)
        val matchingParent = subtypes
          .filter { case (code, _) => selectedCode == code || selectedCode.startsWith(s"$code.") }
          .maxByOption(_._1.length)

        matchingParent match {
          case Some((parentCode, _)) =>
            val withSubtype = PurchaseOrImportHelpers.setSelection(
              answers,
              ImportSubCodePage,
              ImportSubTypeLabelQuery,
              parentCode,
              PurchaseOrImportHelpers.labelFor(parentCode, subtypes)
            )
            val categoryOptions = configPurchaseOrImportMapping.subcategoriesFor(countryCode, parentKey, parentCode)

            categoryOptions.find(_._1 == selectedCode) match {
              case Some((categoryCode, _)) =>
                withSubtype.flatMap(
                  PurchaseOrImportHelpers.setSelection(
                    _,
                    ImportSubCategoryPage,
                    ImportSubCategoryLabelQuery,
                    categoryCode,
                    PurchaseOrImportHelpers.labelFor(categoryCode, categoryOptions)
                  )
                )
              case None => withSubtype
            }

          case None => answers.set(ImportSubCodePage, selectedCode)
        }

      case (None, Some(countryCode)) if importType == Other && configPurchaseOrImportMapping.selectableSubcodes(countryCode, "other").isDefined =>
        answers.set(ImportSubCodePage, ConfigPurchaseOrImportMapping.NoneValue)

      case _ => Success(answers)
    }

  private def text(value: Option[String]): Option[String] =
    value.map(_.trim).filter(_.nonEmpty)

  private def setIfPresent[A](answers: UserAnswers, page: Settable[A], value: Option[A])(implicit writes: Writes[A]): Try[UserAnswers] =
    value.fold(Try(answers))(answers.set(page, _))

  private def buildUpdateRequest(
                                  answers: UserAnswers,
                                  applicationId: Long,
                                  addResponse: AddImportResponse,
                                  importType: PurchaseOrImportType
                                ): UpdateImportDetailsRequest = {
    def real(value: Option[String]): Option[String] =
      value.filter(v => v.trim.nonEmpty && v != ConfigPurchaseOrImportMapping.NoneValue)

    val subCategory = real(answers.get(ImportSubCategoryPage)).orElse(real(answers.get(ImportSubCodePage)))

    val hasSad = answers.get(SadReferenceCheckPage)
    val address = answers.get(ImportSupplierAddressPage)

    val currencyCode = answers.get(ImportCurrencyPage).orElse {
      CountryCode
        .findCountryCode(answers)
        .filterNot(currencyConfig.requiresCurrencySelection)
        .flatMap(country => currencyConfig.currencyConfig.getOrElse(country, currencyConfig.default).headOption)
        .map(_.code)
    }

    UpdateImportDetailsRequest(
      applicationId               = applicationId,
      itemNumber                  = addResponse.itemNumber,
      goodsDescriptionCategory    = PurchaseOrImportType.codes(importType),
      goodsDescriptionSubCategory = subCategory,
      goodsDescriptionText        = real(answers.get(DescribeItemsOnImportDocPage)),
      hasSadReferenceNumber       = hasSad.map(_.toString),
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
