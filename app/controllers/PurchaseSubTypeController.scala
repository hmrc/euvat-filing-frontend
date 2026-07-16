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

import controllers.actions.*
import forms.PurchaseSubTypeFormProvider
import navigation.Navigator
import pages.{PurchaseSubTypePage, PurchaseTypePage, RefundingCountryNamePage, RefundingCountryPage, PurchaseSubTypeLabelPage, PurchaseSubCategoryPage, PurchaseSubCategoryLabelPage, CountryChangedPage}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.ConfigPurchaseMapping
import views.html.PurchaseSubTypeView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import play.api.mvc.Call
import models.PurchaseSubCategoryType
import scala.util.Try

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
    with play.api.Logging {

  val form = formProvider()

  private def resolveCountryCode(userAnswers: models.UserAnswers): Option[String] =
    userAnswers.get(RefundingCountryPage).orElse {
      userAnswers.get(RefundingCountryNamePage).map { stored =>
        // `RefundingCountryNamePage` may be stored as "CODE,Name" or "Name,CODE".
        // Prefer the token after the comma when present since it is often the
        // ISO code; fall back to the whole stored value otherwise.
        val parts = stored.split(",", 2).map(_.trim)
        if (parts.length > 1) parts.last else stored
      }
    }

  def onPageLoad(purchaseTypeSlug: String, mode: models.Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    // If the country was changed, clear the previously stored sub-type selection
    if (request.userAnswers.get(pages.CountryChangedPage).contains(true)) {
      val cleared = for {
        a1 <- request.userAnswers.remove(PurchaseSubTypePage)
        a2 <- a1.remove(PurchaseSubTypeLabelPage)
        a3 <- a2.remove(pages.CountryChangedPage)
      } yield a3

      Future.fromTry(cleared).flatMap(updated => sessionRepository.set(updated).map(_ => Redirect(controllers.routes.PurchaseSubTypeController.onPageLoad(purchaseTypeSlug, mode))))
    } else {
      val maybeParent = models.PurchaseType.fromSlug(purchaseTypeSlug).map(_.toString).orElse(request.userAnswers.get(PurchaseTypePage).map(_.toString))
      val maybeCountry = resolveCountryCode(request.userAnswers)

      (maybeParent, maybeCountry) match {
        case (Some(parentKey), Some(country)) =>
          val options = config.subcodesFor(country, parentKey)
          if (options.isEmpty) Future.successful(Redirect(controllers.routes.InvoiceTypeController.onPageLoad(mode)))
          else {
            val items = config.buildRadioItems(options, messagesApi.preferred(request))

            val parentHeading = parentKey match {
              case "fuel"         => messagesApi.preferred(request)("purchase.sub.fuel.heading")
              case "transport"    => messagesApi.preferred(request)("purchase.sub.transport.heading")
              case "foodAndDrink" => messagesApi.preferred(request)("purchase.sub.foodAndDrink.heading")
              case "luxuries"     => messagesApi.preferred(request)("purchase.sub.luxuries.heading")
              case "other"        => messagesApi.preferred(request)("purchase.sub.other.heading")
              case _               => parentKey
            }

            val preparedForm = request.userAnswers.get(PurchaseSubTypePage).fold(form)(form.fill)
            val resolvedSlug = models.PurchaseType.values.find(_.toString == parentKey).map(models.PurchaseType.slugOf).getOrElse(purchaseTypeSlug)

            val formAction = routes.PurchaseSubTypeController.onSubmit(resolvedSlug, mode)
            val backUrl = routes.PurchaseTypeController.onPageLoad(mode).url

            Future.successful(Ok(view(preparedForm, items, parentHeading, parentHeading, formAction, backUrl)))
          }

        case _ => Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
      }
    }
  }

  def onSubmit(purchaseTypeSlug: String, mode: models.Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    val maybeParent = models.PurchaseType.fromSlug(purchaseTypeSlug).map(_.toString).orElse(request.userAnswers.get(PurchaseTypePage).map(_.toString))
    val maybeCountry = resolveCountryCode(request.userAnswers)

    (maybeParent, maybeCountry) match {
      case (Some(parentKey), Some(country)) =>
        val options = config.subcodesFor(country, parentKey)
        if (options.isEmpty) Future.successful(Redirect(controllers.routes.InvoiceTypeController.onPageLoad(mode)))
        else {
          val items = config.buildRadioItems(options, messagesApi.preferred(request))
          val parentHeading = parentKey match {
            case "fuel"         => messagesApi.preferred(request)("purchase.sub.fuel.heading")
            case "transport"    => messagesApi.preferred(request)("purchase.sub.transport.heading")
            case "foodAndDrink" => messagesApi.preferred(request)("purchase.sub.foodAndDrink.heading")
            case "luxuries"     => messagesApi.preferred(request)("purchase.sub.luxuries.heading")
            case "other"        => messagesApi.preferred(request)("purchase.sub.other.heading")
            case _               => parentKey
          }

          val resolvedSlug = models.PurchaseType.values.find(_.toString == parentKey).map(models.PurchaseType.slugOf).getOrElse(purchaseTypeSlug)

          form
            .bindFromRequest()
            .fold(
              formWithErrors => {
                val formAction = routes.PurchaseSubTypeController.onSubmit(resolvedSlug, mode)
                val backUrl = routes.PurchaseTypeController.onPageLoad(mode).url
                Future.successful(BadRequest(view(formWithErrors, items, parentHeading, parentHeading, formAction, backUrl)))
              },
              value =>
                {
                  val labelKeyOpt = config.subcodesFor(country, parentKey).find(_._1 == value).map(_._2)
                  val label = labelKeyOpt.map(k => messagesApi.preferred(request)(k)).getOrElse(value)

                  val saved = request.userAnswers.get(PurchaseSubTypePage) match {
                    case Some(prev) if prev != value =>
                      for {
                        r1 <- request.userAnswers.remove(PurchaseSubCategoryPage)
                        r2 <- r1.remove(PurchaseSubCategoryLabelPage)
                        a1 <- r2.set(PurchaseSubTypePage, value)
                        a2 <- a1.set(PurchaseSubTypeLabelPage, label)
                        a3 <- (request.userAnswers.get(PurchaseTypePage) match {
                                case Some(_) => scala.util.Success(a2)
                                case None =>
                                  models.PurchaseType.values.find(_.toString == parentKey) match {
                                    case Some(pt) => a2.set(PurchaseTypePage, pt)
                                    case None     => scala.util.Success(a2)
                                  }
                              })
                      } yield a3

                    case _ =>
                      request.userAnswers.set(PurchaseSubTypePage, value).flatMap { a1 =>
                        a1.set(PurchaseSubTypeLabelPage, label).flatMap { a2 =>
                          request.userAnswers.get(PurchaseTypePage) match {
                            case Some(_) => scala.util.Success(a2)
                            case None =>
                              models.PurchaseType.values.find(_.toString == parentKey) match {
                                case Some(pt) => a2.set(PurchaseTypePage, pt)
                                case None     => scala.util.Success(a2)
                              }
                          }
                        }
                      }
                  }

                  for {
                    updatedAnswers <- Future.fromTry(saved)
                    _              <- sessionRepository.set(updatedAnswers)
                  } yield {
                    val children = config.subcategoriesFor(country, parentKey, value)

                    if (children.nonEmpty) {
                      try logger.info(s"PurchaseSubTypeController.onSubmit - children=${children.map(_._1)} value=$value resolvedSlug=$resolvedSlug parentKey=$parentKey") catch { case _: Throwable => }
                      // Determine a safe parent candidate to route to. Avoid using
                      // an actual child code (which may include multiple segments)
                      // as the parent parameter. Prefer an explicit slug mapping
                      // when available, otherwise use the base segment of the
                      // selected value.
                      // Use the selected sub-type value itself as the parent code
                      // when navigating to the sub-category page (e.g. "1.3").
                      val routeParentCodeCandidate = value

                      val candidates = Seq(routeParentCodeCandidate).distinct

                      val maybeCall = candidates.iterator.map { c =>
                        try Some(controllers.routes.PurchaseSubCategoryController.onPageLoad(resolvedSlug, c, mode))
                        catch {
                          case _: Throwable =>
                            // Fallback: use the friendly slug path without query
                            // params. The controller will read the canonical
                            // parent code from session (we persisted it above).
                            val slug = PurchaseSubCategoryType.pathFor(parentKey, c)
                            val prefix = request.path.lastIndexOf('/') match {
                              case i if i > 0 => request.path.substring(0, i)
                              case _           => ""
                            }
                            Some(Call("GET", s"$prefix/$slug"))
                        }
                      }.collectFirst { case Some(call) => call }

                      try logger.info(s"PurchaseSubTypeController.onSubmit - candidates=${candidates.mkString(",")} maybeCall=${maybeCall.isDefined}") catch { case _: Throwable => }

                      Redirect(maybeCall.getOrElse(controllers.routes.InvoiceTypeController.onPageLoad(mode)))
                    } else {
                      // No child subcategories exist. If this is the `Other` purchase type and
                      // the selected sub-type is the 'none of these / give more details' code
                      // (commonly `99` as the last segment) then route to the Describe Items
                      // on Invoice page so the user can provide free-text details.
                      val lastSeg = value.split("\\.").lastOption.getOrElse(value)
                      val isOtherPurchaseType = models.PurchaseType.fromSlug(resolvedSlug).contains(models.PurchaseType.Other)

                      try logger.info(s"PurchaseSubTypeController.onSubmit - no children, value=$value isOther=$isOtherPurchaseType lastSeg=$lastSeg") catch { case _: Throwable => }

                      if (isOtherPurchaseType && lastSeg == "99") Redirect(controllers.routes.DescribeItemsOnInvoiceController.onPageLoad(mode))
                      else Redirect(controllers.routes.InvoiceTypeController.onPageLoad(mode))
                    }
                  }
                }
            )
        }

      case _ => Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
    }
  }
}
