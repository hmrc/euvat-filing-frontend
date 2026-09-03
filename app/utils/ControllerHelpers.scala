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

package utils

import models.requests.DataRequest
import play.api.data.Form
import play.api.libs.json.{Format, Reads}
import play.api.mvc.{Call, Result}
import queries.Gettable
import pages.QuestionPage
import repositories.SessionRepository
import scala.concurrent.{ExecutionContext, Future}
import play.api.mvc.Results.*
import models.{CheckMode, Mode, UserAnswers}
import scala.util.Try
import scala.util.Success
import scala.util.Failure

object ControllerHelpers {

  // Combine two Option values into a tuple when both are defined.
  def bothDefined[A, B](first: Option[A], second: Option[B]): Option[(A, B)] =
    for {
      a <- first
      b <- second
    } yield (a, b)

  def currencyNameAndPrefix(userAnswers: models.UserAnswers, configCurrencyMapping: Map[String, Seq[Currency]])(implicit
    request: DataRequest[?]
  ): (String, String) = CurrencyResolver.currencyNameAndPrefix(userAnswers, configCurrencyMapping)

  def currencySymbolFromSession(userAnswers: models.UserAnswers, configCurrencyMapping: Map[String, Seq[Currency]])(implicit
    request: DataRequest[?]
  ): String = {
    val (_, symbol) = currencyNameAndPrefix(userAnswers, configCurrencyMapping)
    if (symbol.isEmpty) "€" else symbol
  }

  // Generic helper to compare a submitted `value` against a BigDecimal stored
  // on another page in `UserAnswers` using a provided comparator function.
  def compareWithPage(value: BigDecimal, page: pages.QuestionPage[BigDecimal], updated: models.UserAnswers)(
    cmp: (BigDecimal, BigDecimal) => Boolean
  ): Boolean =
    updated.get(page).exists(stored => cmp(value, stored))

  def pathForSlug(slug: String, mode: Mode, prefix: String): String =
    if (mode == models.CheckMode) {
      if (prefix.isEmpty) s"/change-$slug" else s"$prefix/change-$slug"
    } else {
      if (prefix.isEmpty) s"/$slug" else s"$prefix/$slug"
    }

  def redirectToInvoiceTypeOrCYA(mode: Mode): Result = {
    if (mode == models.CheckMode) {
      Redirect(controllers.purchase.routes.CheckYourPurchaseDetailsController.onPageLoad())
    } else {
      Redirect(controllers.routes.InvoiceTypeController.onPageLoad(mode))
    }
  }

  def shortCircuit[T](
    page: QuestionPage[T],
    newValue: T,
    mode: Mode,
    userAnswers: UserAnswers,
    navigatorNext: Call,
    purchaseCya: Call,
    sessionRepositoryOpt: Option[SessionRepository]
  )(onSaved: UserAnswers => Future[Result])(implicit fmt: Format[T], ec: ExecutionContext): Future[Result] = {
    val unchangedRedirect: Call =
      if (mode == CheckMode && userAnswers.get(pages.PurchaseTypePage).isDefined) purchaseCya
      else navigatorNext

    applyShortCircuit(page, newValue, mode, userAnswers, sessionRepositoryOpt, unchangedRedirect, onSaved)
  }

  private def applyShortCircuit[T](
    page: QuestionPage[T],
    newValue: T,
    mode: Mode,
    userAnswers: UserAnswers,
    sessionRepositoryOpt: Option[SessionRepository],
    unchangedRedirect: Call,
    onSaved: UserAnswers => Future[Result]
  )(implicit fmt: Format[T], ec: ExecutionContext): Future[Result] = {
    if (mode == CheckMode && userAnswers.isAnswerUnchanged(page, newValue)) {
      Future.successful(Redirect(unchangedRedirect))
    } else {
      userAnswers.set(page, newValue) match {
        case Success(updated) =>
          sessionRepositoryOpt match {
            case Some(repo) =>
              repo.set(updated).flatMap(_ => onSaved(updated)).recover { case _ => InternalServerError("Failed to persist session") }
            case None =>
              onSaved(updated).recover { case _ => InternalServerError("Failed during onSaved") }
          }
        case Failure(_) =>
          Future.successful(InternalServerError("Failed to build UserAnswers"))
      }
    }
  }

  def saveTryAndRedirect(userAnswersTry: Try[UserAnswers], sessionRepository: SessionRepository, nextPage: Call)(implicit
    ec: ExecutionContext
  ): Future[Result] =
    userAnswersTry match {
      case Success(userAnswers) =>
        sessionRepository
          .set(userAnswers)
          .map(_ => Redirect(nextPage))
          .recover { case _ => InternalServerError("Failed to persist session") }
      case Failure(_) =>
        Future.successful(InternalServerError("Failed to build UserAnswers"))
    }

  // If running in CheckMode and the arrival flag page is not set, set it and persist the updated `UserAnswers`.
  // Otherwise call `render` with the existing `UserAnswers`.
  def markArrivalAndRender(
    page: QuestionPage[Boolean],
    mode: Mode,
    userAnswers: UserAnswers,
    sessionRepository: SessionRepository
  )(
    render: UserAnswers => Future[Result]
  )(implicit ec: ExecutionContext, request: DataRequest[?]): Future[Result] = {
    val shouldMarkArrival =
      mode == CheckMode && userAnswers.get(page).forall(!_)

    if (shouldMarkArrival) {
      for {
        marked <- Future.fromTry(userAnswers.set(page, true))
        _      <- sessionRepository.set(marked)
        result <- render(marked)
      } yield result
    } else {
      render(userAnswers)
    }
  }

}
