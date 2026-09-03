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
import forms.purchase.PurchaseSubTypeFormProvider
import models.{Mode, Other, PurchaseSubCategoryType, PurchaseType, UserAnswers}
import navigation.Navigator
import pages.*
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.*
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.{ConfigPurchaseMapping, ControllerHelpers, CountryCode, MountPrefix}
import views.html.purchase.PurchaseSubTypeView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PurchaseSubTypeController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: PurchaseSubTypeFormProvider,
  config: ConfigPurchaseMapping,
  val controllerComponents: MessagesControllerComponents,
  view: PurchaseSubTypeView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with play.api.Logging:

  private def resolveParentAndCountry(purchaseTypeSlug: String, userAnswers: UserAnswers): Option[(String, String)] =
    val parentKey =
      PurchaseType.valueFromUrlSlug
        .get(purchaseTypeSlug)
        .orElse(
          userAnswers
            .get(PurchaseTypePage)
            .map(_.toString)
        )
    val country = CountryCode.findCountryCode(userAnswers)

    (parentKey, country) match {
      case (Some(parentKey), Some(country)) => Some((parentKey, country))
      case _                                => None
    }

  private def prepareViewData(parentKey: String, country: String, purchaseTypeSlug: String, userAnswers: UserAnswers, mode: Mode)(implicit
    request: RequestHeader
  ) = {
    val options = config.subcodesFor(country, parentKey)
    val rawItems = config.buildRadioItems(options, messagesApi.preferred(request))
    val items = if (parentKey == "other") rawItems.filterNot(_.value.contains(ConfigPurchaseMapping.NoneValue)) else rawItems
    val parentHeading = parentHeadingFor(parentKey)
    val msgs = messagesApi.preferred(request)
    val requiredKeyCandidates = Seq(s"purchase.sub.$parentKey.error.required")
    val requiredKey = requiredKeyCandidates.find(k => msgs.isDefinedAt(k)).getOrElse("error.required")
    val preparedForm = userAnswers.get(PurchaseSubTypePage).fold(formProvider(requiredKey))(formProvider(requiredKey).fill)
    val resolvedSlug = resolvedSlugFor(parentKey, purchaseTypeSlug)
    val formAction = formActionFor(resolvedSlug, mode)

    (options, items, parentHeading, preparedForm, resolvedSlug, formAction)
  }

  private def persistSelection(currentAnswers: UserAnswers, parentKey: String, value: String, label: String): scala.util.Try[UserAnswers] =
    currentAnswers.get(PurchaseSubTypePage) match {
      case Some(previousSelection) if previousSelection != value =>
        for {
          removedSubCategory      <- currentAnswers.remove(PurchaseSubCategoryPage)
          removedSubCategoryLabel <- removedSubCategory.remove(PurchaseSubCategoryLabelPage)
          setSubType              <- removedSubCategoryLabel.set(PurchaseSubTypePage, value)
          setSubTypeLabel         <- setSubType.set(PurchaseSubTypeLabelPage, label)
          finalAnswers <- currentAnswers.get(PurchaseTypePage) match {
                            case Some(_) => scala.util.Success(setSubTypeLabel)
                            case None =>
                              PurchaseType.values.find(_.toString == parentKey) match {
                                case Some(pt) => setSubTypeLabel.set(PurchaseTypePage, pt)
                                case None     => scala.util.Success(setSubTypeLabel)
                              }
                          }
        } yield finalAnswers

      case _ =>
        for {
          setSubType      <- currentAnswers.set(PurchaseSubTypePage, value)
          setSubTypeLabel <- setSubType.set(PurchaseSubTypeLabelPage, label)
          finalAnswers <- currentAnswers.get(PurchaseTypePage) match {
                            case Some(_) => scala.util.Success(setSubTypeLabel)
                            case None =>
                              PurchaseType.values.find(_.toString == parentKey) match {
                                case Some(pt) => setSubTypeLabel.set(PurchaseTypePage, pt)
                                case None     => scala.util.Success(setSubTypeLabel)
                              }
                          }
        } yield finalAnswers
    }

  private def parentHeadingFor(parentKey: String)(implicit request: play.api.mvc.RequestHeader): String =
    parentKey match {
      case "fuel"         => messagesApi.preferred(request)("purchase.sub.fuel.heading")
      case "transport"    => messagesApi.preferred(request)("purchase.sub.transport.heading")
      case "foodAndDrink" => messagesApi.preferred(request)("purchase.sub.foodAndDrink.heading")
      case "luxuries"     => messagesApi.preferred(request)("purchase.sub.luxuries.heading")
      case "other"        => messagesApi.preferred(request)("purchase.sub.other.heading")
      case _              => parentKey
    }

  private def resolvedSlugFor(parentKey: String, fallback: String): String =
    PurchaseType.values.find(_.toString == parentKey).map(PurchaseType.urlSlugForPurchaseType).getOrElse(fallback)

  private def formActionFor(uri: String, mode: Mode)(implicit request: RequestHeader) = {
    val isChangeMode = if (mode == models.CheckMode) "change-" else ""
    Call("POST", s"${MountPrefix.getFromRequest}/$isChangeMode$uri")
  }

  private def backUrlFor(mode: Mode) = routes.PurchaseTypeController.onPageLoad(mode).url

  private def handleCountryChanged(userAnswers: UserAnswers)(implicit request: RequestHeader) =
    val updatedAnswers = for
      afterRemovedSubType      <- userAnswers.remove(PurchaseSubTypePage)
      afterRemovedSubTypeLabel <- afterRemovedSubType.remove(PurchaseSubTypeLabelPage)
      afterClearedFlag         <- afterRemovedSubTypeLabel.remove(pages.CountryChangedPage)
    yield afterClearedFlag
    Future.fromTry(updatedAnswers).map(sessionRepository.set)

  private def renderSubTypeView(preparedForm: play.api.data.Form[?],
                                items: Seq[uk.gov.hmrc.govukfrontend.views.viewmodels.radios.RadioItem],
                                heading: String,
                                formAction: Call,
                                mode: Mode
                               )(implicit request: Request[AnyContent]): Future[play.api.mvc.Result] = {
    val backUrl = backUrlFor(mode)
    Future.successful(Ok(view(preparedForm, items, heading, heading, formAction, backUrl)))
  }

  def onPageLoad(purchaseTypeSlug: String, mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async:
    implicit request =>
      if request.userAnswers.get(CountryChangedPage).contains(true) then
        handleCountryChanged(request.userAnswers)
          .map(_ => Redirect(Call("GET", s"${MountPrefix.getFromRequest}/$purchaseTypeSlug")))
      else {
        resolveParentAndCountry(purchaseTypeSlug, request.userAnswers) match
          case None => Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
          case Some((parentKey, country)) =>
            val (options, items, parentHeading, preparedForm, resolvedSlug, formAction) =
              prepareViewData(parentKey, country, purchaseTypeSlug, request.userAnswers, mode)(request)

            if (options.isEmpty) {
              Future.successful(
                if mode == models.CheckMode
                then Redirect(routes.CheckYourPurchaseDetailsController.onPageLoad())
                else Redirect(routes.InvoiceTypeController.onPageLoad(mode))
              )
            } else {
              lazy val singleCode = options.head._1
              lazy val lastSeg = singleCode.split("\\.").lastOption.getOrElse(singleCode)
              if (parentKey == "other" && options.size == 1 && lastSeg == "99") {
                val labelKey = options.head._2
                val label = if (labelKey != null && labelKey.nonEmpty) messagesApi.preferred(request)(labelKey) else singleCode
                val savedTry = persistSelection(request.userAnswers, parentKey, singleCode, label)

                ControllerHelpers.persistAndThen(savedTry, sessionRepository) { _ =>
                  Future.successful(Redirect(routes.DescribeItemsOnInvoiceController.onPageLoad(mode)))
                }
              } else {
                ControllerHelpers.markArrivalAndRender(
                  pages.PurchaseSubTypeArrivedFromCheckYourAnswersPage,
                  mode,
                  request.userAnswers,
                  sessionRepository
                )(_ => renderSubTypeView(preparedForm, items, parentHeading, formAction, mode))
              }
            }
      }

  def onSubmit(purchaseTypeSlug: String, mode: Mode): Action[AnyContent] =
    (identify andThen getData andThen requireData).async { implicit request =>
      resolveParentAndCountry(purchaseTypeSlug, request.userAnswers) match {
        case Some((parentKey, country)) =>
          val (options, items, parentHeading, preparedForm, resolvedSlug, _) =
            prepareViewData(parentKey, country, purchaseTypeSlug, request.userAnswers, mode)(request)

          if (options.isEmpty) {
            Future.successful(
              if mode == models.CheckMode
              then Redirect(routes.CheckYourPurchaseDetailsController.onPageLoad())
              else Redirect(routes.InvoiceTypeController.onPageLoad(mode))
            )
          } else {
            preparedForm
              .bindFromRequest()
              .fold(
                // Validation errors -> re-render form with errors
                formWithErrors => {
                  val formAction = formActionFor(resolvedSlug, mode)
                  val backUrl = backUrlFor(mode)
                  Future.successful(BadRequest(view(formWithErrors, items, parentHeading, parentHeading, formAction, backUrl)))
                },
                value => {
                  utils.CheckModeShortCircuit.shortCircuitIfUnchanged(
                    pages.PurchaseSubTypePage,
                    value,
                    mode,
                    request.userAnswers,
                    routes.CheckYourPurchaseDetailsController.onPageLoad()
                  ) match {
                    case Some(res) => Future.successful(res)
                    case None =>
                      if (value == ConfigPurchaseMapping.NoneValue) {
                        val noneLabel = ConfigPurchaseMapping.NoneValue
                        val savedTry = for {
                          a1 <- request.userAnswers.set(PurchaseSubTypePage, ConfigPurchaseMapping.NoneValue)
                          a2 <- a1.set(PurchaseSubTypeLabelPage, noneLabel)
                          a3 <- a2.remove(PurchaseSubCategoryPage)
                          a4 <- a3.remove(PurchaseSubCategoryLabelPage)
                        } yield a4

                        ControllerHelpers.persistAndThen(savedTry, sessionRepository) { _ =>
                          Future.successful(
                            if (mode == models.CheckMode) Redirect(routes.CheckYourPurchaseDetailsController.onPageLoad())
                            else Redirect(routes.InvoiceTypeController.onPageLoad(mode))
                          )
                        }

                      } else {
                        val labelKeyOpt = config.subcodesFor(country, parentKey).find(_._1 == value).map(_._2)
                        val label = labelKeyOpt.map(k => messagesApi.preferred(request)(k)).getOrElse(value)
                        val savedTry = persistSelection(request.userAnswers, parentKey, value, label)

                        ControllerHelpers.persistAndThen(savedTry, sessionRepository) { _ =>
                          val children = config.subcategoriesFor(country, parentKey, value)

                          if (children.nonEmpty) {
                            val routeParentCodeCandidate = value
                            val candidates = Seq(routeParentCodeCandidate).distinct
                            val maybeCall = candidates.iterator
                              .map { c =>
                                try {
                                  val slug = PurchaseSubCategoryType.pathFor(parentKey, c)
                                  val prefix = MountPrefix.getFromRequest
                                  val path =
                                    if (mode == models.CheckMode)
                                      if (prefix.isEmpty) s"/change-$slug" else s"$prefix/change-$slug"
                                    else if (prefix.isEmpty) s"/$slug"
                                    else s"$prefix/$slug"
                                  Some(Call("GET", path))
                                } catch {
                                  case _: Throwable => None
                                }
                              }
                              .collectFirst { case Some(call) => call }
                            Future.successful(maybeCall.fold(Redirect(routes.InvoiceTypeController.onPageLoad(mode)))(Redirect))
                          } else {
                            val lastSeg = value.split("\\.").lastOption.getOrElse(value)
                            val isOtherPurchaseType =
                              PurchaseType.values.find(pt => PurchaseType.urlSlugForPurchaseType(pt) == resolvedSlug).contains(Other)

                            Future.successful(
                              if (isOtherPurchaseType && lastSeg == "99")
                                Redirect(routes.DescribeItemsOnInvoiceController.onPageLoad(mode))
                              else if (mode == models.CheckMode) Redirect(routes.CheckYourPurchaseDetailsController.onPageLoad())
                              else Redirect(routes.InvoiceTypeController.onPageLoad(mode))
                            )
                          }
                        }
                      }
                  }
                }
              )
          }

        case None => Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
      }
    }
