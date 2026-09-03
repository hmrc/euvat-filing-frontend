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
import models.requests.{AddPurchaseRequest, DataRequest}
import models.{Mode, PurchaseType, UserAnswers}
import navigation.Navigator
import pages.*
import play.api.Logging
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.*
import queries.ClaimApplicationResponseQuery
import repositories.SessionRepository
import services.EuVatRefundsService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import utils.{CheckModeShortCircuit, ConfigPurchaseMapping, CountryCode, MountPrefix}
import views.html.purchase.PurchaseTypeView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class PurchaseTypeController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  config: ConfigPurchaseMapping,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: PurchaseTypeFormProvider,
  val controllerComponents: MessagesControllerComponents,
  euVatRefundsService: EuVatRefundsService,
  view: PurchaseTypeView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  val form: Form[PurchaseType] = formProvider()

  private def backLink(mode: Mode)(implicit request: DataRequest[?]) = controllers.routes.BeforeYouStartPurchaseController.onPageLoad()

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    if (request.userAnswers.get(CountryChangedPage).contains(true)) {
      val clearedTry = for {
        afterRemovedPurchaseType        <- request.userAnswers.remove(PurchaseTypePage)
        afterRemovedPurchaseSubType     <- afterRemovedPurchaseType.remove(PurchaseSubTypePage)
        afterRemovedPurchaseSubTypeLbl  <- afterRemovedPurchaseSubType.remove(PurchaseSubTypeLabelPage)
        afterRemovedPurchaseSubCategory <- afterRemovedPurchaseSubTypeLbl.remove(PurchaseSubCategoryPage)
        afterRemovedPurchaseSubCatLbl   <- afterRemovedPurchaseSubCategory.remove(PurchaseSubCategoryLabelPage)
        afterClearedFlag                <- afterRemovedPurchaseSubCatLbl.remove(CountryChangedPage)
      } yield afterClearedFlag

      Future
        .fromTry(clearedTry)
        .flatMap(updated =>
          sessionRepository
            .set(updated)
            .map(_ => {
              val preparedForm = updated.get(PurchaseTypePage).fold(form)(form.fill)
              Ok(view(preparedForm, mode, backLink(mode)))
            })
        )
    } else {
      val preparedForm = request.userAnswers.get(PurchaseTypePage).fold(form)(form.fill)
      Future.successful(Ok(view(preparedForm, mode, backLink(mode))))
    }
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    form
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, mode, backLink(mode)))),
        value => {
          val previous = request.userAnswers.get(PurchaseTypePage)
          if (mode == models.CheckMode && previous.contains(value)) {
            val arrivedFromDescribe = request.userAnswers.get(pages.DescribeItemsArrivedFromCheckYourAnswersPage).contains(true)
            val arrivedFromSubTypeOrCategory = request.userAnswers
              .get(pages.PurchaseSubTypeArrivedFromCheckYourAnswersPage)
              .contains(true) || request.userAnswers.get(pages.PurchaseSubCategoryArrivedFromCheckYourAnswersPage).contains(true)

            lazy val describePresent = request.userAnswers.get(DescribeItemsOnInvoicePage).exists(_.trim.nonEmpty)
            lazy val hasMeaningfulSubcodes =
              CountryCode
                .findCountryCode(request.userAnswers)
                .forall(
                  config.subcodesFor(_, value.toString).exists { case (code, _) =>
                    !code.split("\\.").lastOption.contains("99")
                  }
                )
            if (arrivedFromDescribe && !arrivedFromSubTypeOrCategory && (describePresent || hasMeaningfulSubcodes)) {
              val removedTry = request.userAnswers.remove(pages.DescribeItemsArrivedFromCheckYourAnswersPage)
              Future.fromTry(removedTry).flatMap { ua =>
                sessionRepository.set(ua).map { _ =>
                  val call = routes.DescribeItemsOnInvoiceController.onPageLoad(models.CheckMode)
                  val prefix = MountPrefix.getFromRequest
                  if prefix.isEmpty || call.url.startsWith(prefix)
                  then Redirect(call)
                  else Redirect(Call(call.method, s"$prefix${call.url}"))
                }
              }
            } else {
              CheckModeShortCircuit.shortCircuitIfUnchanged(
                PurchaseTypePage,
                value,
                mode,
                request.userAnswers,
                routes.CheckYourPurchaseDetailsController.onPageLoad()
              ) match {
                case Some(res) => Future.successful(res)
                case None =>
                  Future.failed(new IllegalStateException("Expected short-circuit result for unchanged CheckMode submission"))
              }
            }
          } else {
            CheckModeShortCircuit.shortCircuitIfUnchanged(
              PurchaseTypePage,
              value,
              mode,
              request.userAnswers,
              routes.CheckYourPurchaseDetailsController.onPageLoad()
            ) match {
              case Some(res) => Future.successful(res)
              case None =>
                val userAnswersTry = request.userAnswers.get(PurchaseTypePage) match {
                  case Some(prev) if prev != value => buildUpdatedTryForPurchaseTypeChange(value)
                  case _                           => request.userAnswers.set(PurchaseTypePage, value)
                }

                persistAndHandleSaved(userAnswersTry, value, mode)
            }
          }
        }
      )
  }

  private def buildUpdatedTryForPurchaseTypeChange(value: PurchaseType)(implicit request: DataRequest[?]): Try[UserAnswers] =
    for {
      afterRemovedSubType        <- request.userAnswers.remove(PurchaseSubTypePage)
      afterRemovedSubTypeLabel   <- afterRemovedSubType.remove(PurchaseSubTypeLabelPage)
      afterRemovedSubCategory    <- afterRemovedSubTypeLabel.remove(PurchaseSubCategoryPage)
      afterRemovedSubCategoryLbl <- afterRemovedSubCategory.remove(PurchaseSubCategoryLabelPage)
      afterRemovedDescribe       <- afterRemovedSubCategoryLbl.remove(DescribeItemsOnInvoicePage)
      afterSetPurchaseType       <- afterRemovedDescribe.set(PurchaseTypePage, value)
    } yield afterSetPurchaseType

  private def persistAndHandleSaved(userAnswersTry: Try[UserAnswers], value: PurchaseType, mode: Mode)(implicit
    request: DataRequest[?]
  ): Future[Result] =
    Future.fromTry(userAnswersTry).flatMap { persistedAnswers =>
      sessionRepository.set(persistedAnswers).flatMap { _ =>
        if (persistedAnswers.get(AddPurchaseResponsePage).isEmpty && persistedAnswers.get(queries.ClaimApplicationResponseQuery).isDefined)
          addPurchaseAndPersist(persistedAnswers, value, mode)
        else if (mode == models.CheckMode)
          handleCheckModePostPersist(persistedAnswers, value)
        else
          handleNormalModeRedirect(persistedAnswers, mode)
      }
    }

  private def handleCheckModePostPersist(updatedAnswers: UserAnswers, value: PurchaseType)(implicit request: DataRequest[?]): Future[Result] = {
    val countryOpt = CountryCode.findCountryCode(updatedAnswers)

    val hasSubcodes = countryOpt
      .flatMap { c =>
        try Some(config.subcodesFor(c, value.toString).nonEmpty)
        catch { case _: Throwable => None }
      }
      .getOrElse(true)

    if (updatedAnswers.get(pages.DescribeItemsArrivedFromCheckYourAnswersPage).contains(true)) {
      val removeTry = updatedAnswers.remove(pages.DescribeItemsArrivedFromCheckYourAnswersPage)
      Future.fromTry(removeTry).flatMap { ua =>
        sessionRepository.set(ua).map { _ =>
          val call = routes.DescribeItemsOnInvoiceController.onPageLoad(models.CheckMode)
          val prefix = MountPrefix.getFromRequest
          if prefix.isEmpty || call.url.startsWith(prefix)
          then Redirect(call)
          else Redirect(Call(call.method, s"$prefix${call.url}"))
        }
      }
    } else if (updatedAnswers.get(pages.PurchaseSubTypeArrivedFromCheckYourAnswersPage).contains(true)) {
      val removeTry = updatedAnswers.remove(pages.PurchaseSubTypeArrivedFromCheckYourAnswersPage)
      Future.fromTry(removeTry).flatMap { ua =>
        sessionRepository.set(ua).map { _ =>
          val call = routes.PurchaseSubTypeController.onPageLoad(PurchaseType.urlSlugForPurchaseType(value), models.CheckMode)
          val prefix = MountPrefix.getFromRequest
          if prefix.isEmpty || call.url.startsWith(prefix)
          then Redirect(call)
          else Redirect(Call(call.method, s"$prefix${call.url}"))
        }
      }
    } else if (updatedAnswers.get(pages.PurchaseSubCategoryArrivedFromCheckYourAnswersPage).contains(true)) {
      val removeTry = updatedAnswers.remove(pages.PurchaseSubCategoryArrivedFromCheckYourAnswersPage)
      Future.fromTry(removeTry).flatMap { ua =>
        sessionRepository.set(ua).map { _ =>
          val call = routes.PurchaseSubTypeController.onPageLoad(PurchaseType.urlSlugForPurchaseType(value), models.CheckMode)
          val prefix = MountPrefix.getFromRequest
          if prefix.isEmpty || call.url.startsWith(prefix)
          then Redirect(call)
          else Redirect(Call(call.method, s"$prefix${call.url}"))
        }
      }
    } else if (!hasSubcodes) Future.successful(Redirect(routes.CheckYourPurchaseDetailsController.onPageLoad()))
    else {
      val slug = PurchaseType.urlSlugForPurchaseType(value)
      val prefix = MountPrefix.getFromRequest
      val changePath = s"${if (prefix.isEmpty) "" else prefix}/change-$slug"
      Future.successful(Redirect(Call("GET", changePath)))
    }
  }

  private def handleNormalModeRedirect(updatedAnswers: UserAnswers, mode: Mode)(implicit request: DataRequest[?]): Future[Result] = {
    val call = navigator.nextPage(PurchaseTypePage, mode, updatedAnswers)
    val prefix = MountPrefix.getFromRequest
    if (prefix.isEmpty || call.url.startsWith(prefix)) Future.successful(Redirect(call))
    else Future.successful(Redirect(Call(call.method, s"$prefix${call.url}")))
  }

  private def addPurchaseAndPersist(
    answers: UserAnswers,
    purchaseType: PurchaseType,
    mode: Mode
  )(implicit request: DataRequest[?]): Future[Result] = {
    implicit val hc: HeaderCarrier =
      HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    answers
      .get(ClaimApplicationResponseQuery)
      .fold {
        logger.warn("Missing applicationId for addPurchase")
        Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
      } { claimResponse =>
        val purchaseRequest = AddPurchaseRequest(
          applicationId            = claimResponse.applicationId,
          goodsDescriptionCategory = PurchaseType.codes(purchaseType),
          updateSequenceNumber     = claimResponse.updateSeqNumber
        )

        euVatRefundsService
          .addPurchase(purchaseRequest)
          .flatMap { response =>
            for {
              updatedAnswers <- Future.fromTry(answers.set(AddPurchaseResponsePage, response))
              _              <- sessionRepository.set(updatedAnswers)
            } yield {
              val call = navigator.nextPage(PurchaseTypePage, mode, updatedAnswers)
              val prefix = MountPrefix.getFromRequest
              if prefix.isEmpty || call.url.startsWith(prefix)
              then Redirect(call)
              else Redirect(Call(call.method, s"$prefix${call.url}"))
            }
          }
          .recover { case ex =>
            logger.error("Error while adding the purchase", ex)
            Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
          }
      }
  }
}
