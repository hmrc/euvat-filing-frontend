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
import pages.{PurchaseSubTypePage, PurchaseTypePage, PurchaseSubCategoryPage, PurchaseSubCategoryLabelPage, PurchaseSubTypeLabelPage, CountryChangedPage}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.ConfigPurchaseMapping
import views.html.PurchaseSubTypeView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class PurchaseSubCategoryController @Inject() (
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
    userAnswers.get(pages.RefundingCountryPage).orElse {
      userAnswers.get(pages.RefundingCountryNamePage).map { stored =>
        // `RefundingCountryNamePage` may contain "CODE,Name" or "Name,CODE".
        // Use the last token after a comma when present to prefer the ISO code.
        val parts = stored.split(",", 2).map(_.trim)
        if (parts.length > 1) parts.last else stored
      }
    }

  def onPageLoad(purchaseTypeSlug: String, parentCode: String, mode: models.Mode): Action[AnyContent] =
    (identify andThen getData andThen requireData).async { implicit request =>

      // If the country was changed, clear dependent child selection and reload
      if (request.userAnswers.get(pages.CountryChangedPage).contains(true)) {
        val cleared = for {
          a1 <- request.userAnswers.remove(PurchaseSubCategoryPage)
          a2 <- a1.remove(PurchaseSubCategoryLabelPage)
          a3 <- a2.remove(pages.CountryChangedPage)
        } yield a3

        Future.fromTry(cleared).flatMap(updated => sessionRepository.set(updated).map(_ => Redirect(controllers.routes.PurchaseSubCategoryController.onPageLoad(purchaseTypeSlug, parentCode, mode))))
      } else {
        val maybeParent = models.PurchaseType.fromSlug(purchaseTypeSlug).map(_.toString)
        val maybeCountry = resolveCountryCode(request.userAnswers)

        (maybeParent, maybeCountry) match {
          case (Some(parentKey), Some(country)) =>
            // Prefer the parent code stored in session (PurchaseSubTypePage) when
            // present. This lets us route to slug-only paths (e.g. /fuel-type)
            // without needing to pass the dotted parent code as a query param.
            val parentFromSessionOpt = request.userAnswers.get(pages.PurchaseSubTypePage)
            val effectiveParentCode = parentFromSessionOpt.getOrElse(parentCode)

            // determine candidate options for the given parent code
            val initialOptions: Seq[(String, String)] = config.subcategoriesFor(country, parentKey, effectiveParentCode)

            def findByLastSegment(seg: String): Option[String] =
              config.subcodesFor(country, parentKey).map(_._1).find(code => code.split("\\.").lastOption.contains(seg))

            val (resolvedParentCode, options) = if (initialOptions.nonEmpty) {
              (effectiveParentCode, initialOptions)
            } else {
              val alt = effectiveParentCode.split("\\.").drop(1).mkString(".")
              val altOptions = if (alt.nonEmpty) config.subcategoriesFor(country, parentKey, alt) else Seq.empty
              if (altOptions.nonEmpty) (alt, altOptions)
              else findByLastSegment(parentCode).map(found => (found, config.subcategoriesFor(country, parentKey, found))).getOrElse((parentCode, initialOptions))
            }

            if (options.isEmpty) Future.successful(Redirect(controllers.routes.InvoiceTypeController.onPageLoad(mode)))
            else {
              val msgs = messagesApi.preferred(request)
              val items = config.buildRadioItems(options, msgs)

              def stripLeadingNumeric(key: String): String = {
                val parts = key.split("\\.")
                if (parts.length >= 5 && parts.head == "purchase" && parts(1) == "sub") (parts.take(3) ++ parts.drop(4)).mkString(".")
                else key
              }

              def titleForLabelKey(labelKey: String): Option[String] = {
                val original = s"${labelKey}.title"
                val stripped = s"${stripLeadingNumeric(labelKey)}.title"
                Seq(original, stripped).collectFirst { case k if msgs.isDefinedAt(k) => msgs(k) }
              }

              val childTitleOpt = options.to(LazyList).flatMap { case (_, labelKey) => titleForLabelKey(labelKey) }.headOption

              def parentDerivedTitle(): Option[String] = {
                val asIs = s"purchase.sub.${parentKey}.${resolvedParentCode}.title"
                val dropLeading = {
                  val parts = resolvedParentCode.split("\\.")
                  if (parts.length > 1) s"purchase.sub.${parentKey}.${parts.drop(1).mkString(".")}.title" else asIs
                }
                val lastSeg = resolvedParentCode.split("\\.").lastOption.map(s => s"purchase.sub.${parentKey}.${s}.title").getOrElse(asIs)
                Seq(asIs, dropLeading, lastSeg).collectFirst { case k: String if msgs.isDefinedAt(k) => msgs(k) }
              }

              val parentLabelKeyOpt = config.subcodesFor(country, parentKey).find(_._1 == resolvedParentCode).map(_._2)
              val parentHeading = msgs(s"purchase.sub.${parentKey}.heading")
              val heading = childTitleOpt.orElse(parentDerivedTitle()).getOrElse(parentHeading)
              val pageTitle = heading

              val preparedForm = request.userAnswers.get(PurchaseSubCategoryPage).fold(form)(form.fill)

              def tryReverseParent(candidate: String) = Try(routes.PurchaseSubCategoryController.onSubmit(purchaseTypeSlug, candidate, mode)).toOption.orElse {
                try {
                  // Fallback: use the friendly slug path without query params.
                  val slug = models.PurchaseSubCategoryType.pathFor(parentKey, candidate)
                  val prefix = request.path.lastIndexOf('/') match {
                    case i if i > 0 => request.path.substring(0, i)
                    case _           => ""
                  }
                  Some(play.api.mvc.Call("POST", s"$prefix/$slug"))
                } catch {
                  case _: Throwable => None
                }
              }

              val head = resolvedParentCode.split("\\.").headOption.getOrElse(resolvedParentCode)
              val last = resolvedParentCode.split("\\.").lastOption.getOrElse(resolvedParentCode)
              val candidates = Seq(resolvedParentCode, last, head).distinct

              val formAction = candidates.iterator.flatMap(c => tryReverseParent(c)).find(_ => true).getOrElse(routes.PurchaseSubTypeController.onSubmit(purchaseTypeSlug, mode))
              val backUrl = routes.PurchaseSubTypeController.onPageLoad(purchaseTypeSlug, mode).url

              val parentBase = resolvedParentCode.split("\\.").headOption.getOrElse(resolvedParentCode)

              // Determine the canonical value to persist for PurchaseSubTypePage.
              // If the resolved parent code already appears detailed (contains a dot)
              // then persist that. Otherwise fall back to the first available
              // child's code so tests and existing expectations remain stable.
              val childToPersist = if (resolvedParentCode.contains(".")) resolvedParentCode else options.headOption.map(_._1).getOrElse(resolvedParentCode)

              request.userAnswers.get(pages.PurchaseSubTypePage) match {
                case Some(existing) if existing.split("\\.").headOption.contains(parentBase) =>
                  Future.successful(Ok(view(preparedForm, items, pageTitle, heading, formAction, backUrl)))
                case _ =>
                  val labelForParent = parentLabelKeyOpt.flatMap(k => Some(messagesApi.preferred(request)(k))).getOrElse(childToPersist)
                  val saved = for {
                    a1 <- request.userAnswers.set(pages.PurchaseSubTypePage, childToPersist)
                    a2 <- a1.set(pages.PurchaseSubTypeLabelPage, labelForParent)
                  } yield a2

                  Future.fromTry(saved).flatMap(finalAnswers => sessionRepository.set(finalAnswers).map(_ => Ok(view(preparedForm, items, pageTitle, heading, formAction, backUrl))))
              }
            }

          case _ => Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
        }
      }
  }

  def onSubmit(purchaseTypeSlug: String, parentCode: String, mode: models.Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    val maybeParent = models.PurchaseType.fromSlug(purchaseTypeSlug).map(_.toString)
    val maybeCountry = resolveCountryCode(request.userAnswers)

    (maybeParent, maybeCountry) match {
      case (Some(parentKey), Some(country)) =>
        val parentFromSessionOpt = request.userAnswers.get(pages.PurchaseSubTypePage)
        val effectiveParentCode = parentFromSessionOpt.getOrElse(parentCode)

        val initialOptions = config.subcategoriesFor(country, parentKey, effectiveParentCode)

        def findByLastSegment(seg: String): Option[String] =
          config.subcodesFor(country, parentKey).map(_._1).find(code => code.split("\\.").lastOption.contains(seg))

        val (resolvedParentCode, options) = if (initialOptions.nonEmpty) {
          (effectiveParentCode, initialOptions)
        } else {
          val alt = effectiveParentCode.split("\\.").drop(1).mkString(".")
          val altOptions = if (alt.nonEmpty) config.subcategoriesFor(country, parentKey, alt) else Seq.empty
          if (altOptions.nonEmpty) (alt, altOptions)
          else findByLastSegment(parentCode).map(found => (found, config.subcategoriesFor(country, parentKey, found))).getOrElse((parentCode, initialOptions))
        }

        if (options.isEmpty) Future.successful(Redirect(controllers.routes.InvoiceTypeController.onPageLoad(mode)))
        else {
          val msgs = messagesApi.preferred(request)
          val items = config.buildRadioItems(options, msgs)

          def stripLeadingNumeric(key: String): String = {
            val parts = key.split("\\.")
            if (parts.length >= 5 && parts.head == "purchase" && parts(1) == "sub") (parts.take(3) ++ parts.drop(4)).mkString(".")
            else key
          }

          def titleForLabelKey(labelKey: String): Option[String] = {
            val original = s"${labelKey}.title"
            val stripped = s"${stripLeadingNumeric(labelKey)}.title"
            Seq(original, stripped).collectFirst { case k if msgs.isDefinedAt(k) => msgs(k) }
          }

          val childTitleOpt = options.to(LazyList).flatMap { case (_, labelKey) => titleForLabelKey(labelKey) }.headOption

          def parentDerivedTitle(): Option[String] = {
            val asIs = s"purchase.sub.${parentKey}.${resolvedParentCode}.title"
            val dropLeading = {
              val parts = resolvedParentCode.split("\\.")
              if (parts.length > 1) s"purchase.sub.${parentKey}.${parts.drop(1).mkString(".")}.title" else asIs
            }
            val lastSeg = resolvedParentCode.split("\\.").lastOption.map(s => s"purchase.sub.${parentKey}.${s}.title").getOrElse(asIs)
            Seq(asIs, dropLeading, lastSeg).collectFirst { case k if msgs.isDefinedAt(k) => msgs(k) }
          }

          val parentLabelKeyOpt = config.subcodesFor(country, parentKey).find(_._1 == resolvedParentCode).map(_._2)
          val parentHeading = msgs(s"purchase.sub.${parentKey}.heading")
          val heading = childTitleOpt.orElse(parentDerivedTitle()).getOrElse(parentHeading)
          val pageTitle = heading

              def tryReverseParent(candidate: String) = Try(routes.PurchaseSubCategoryController.onSubmit(purchaseTypeSlug, candidate, mode)).toOption.orElse {
                try {
                  val slug = models.PurchaseSubCategoryType.pathFor(parentKey, candidate)
                  val prefix = request.path.lastIndexOf('/') match {
                    case i if i > 0 => request.path.substring(0, i)
                    case _           => ""
                  }
                  Some(play.api.mvc.Call("POST", s"$prefix/$slug"))
                } catch {
                  case _: Throwable => None
                }
              }

          val head = resolvedParentCode.split("\\.").headOption.getOrElse(resolvedParentCode)
          val last = resolvedParentCode.split("\\.").lastOption.getOrElse(resolvedParentCode)
          val candidates = Seq(resolvedParentCode, last, head).distinct
          val formAction = candidates.iterator.flatMap(c => tryReverseParent(c)).find(_ => true).getOrElse(routes.PurchaseSubTypeController.onSubmit(purchaseTypeSlug, mode))
          val backUrl = routes.PurchaseSubTypeController.onPageLoad(purchaseTypeSlug, mode).url

          form
            .bindFromRequest()
            .fold(
              formWithErrors => Future.successful(BadRequest(view(formWithErrors, items, pageTitle, heading, formAction, backUrl))),
              value =>
                {
                  val labelKeyOpt = config.subcategoriesFor(country, parentKey, resolvedParentCode).find(_._1 == value).map(_._2)
                  val label = labelKeyOpt.map(k => messagesApi.preferred(request)(k)).getOrElse(value)

                  val saved = for {
                    a1 <- request.userAnswers.set(PurchaseSubCategoryPage, value)
                    a2 <- a1.set(PurchaseSubCategoryLabelPage, label)
                  } yield a2

                  for {
                    updatedAnswers <- Future.fromTry(saved)
                    _              <- sessionRepository.set(updatedAnswers)
                  } yield {
                    // Always continue to Invoice Type from PurchaseSubCategory. The
                    // behaviour that routes to Describe Items for `Other`+`99` is
                    // handled from the PurchaseSubType flow when no subcategories
                    // exist, so this page should consistently go to InvoiceType.
                    try logger.info(s"PurchaseSubCategoryController.onSubmit - persisted child=$value label=$label resolvedParent=$resolvedParentCode parentKey=$parentKey") catch { case _: Throwable => }
                    Redirect(controllers.routes.InvoiceTypeController.onPageLoad(mode))
                  }
                }
            )
        }

      case _ => Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
    }
  }
}
