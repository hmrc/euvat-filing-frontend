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
import forms.purchase.InvoiceNumberFormProvider
import models.{CheckMode, Mode, NormalMode, UserAnswers}
import navigation.Navigator
import pages.{InvoiceNumberPage, VrnWarningFlowPage}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.*
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.CheckModeShortCircuit
import views.html.purchase.InvoiceNumberView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class InvoiceNumberController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: InvoiceNumberFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: InvoiceNumberView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  val form: Form[String] = formProvider()

  private def backLink(mode: Mode): Call = routes.InvoiceTypeController.onPageLoad(mode)

  private def badRequestView(formWithErrors: Form[?], mode: Mode)(implicit request: Request[AnyContent]) = {
    val html = view(formWithErrors, mode, routes.InvoiceTypeController.onPageLoad(mode))(request, messagesApi.preferred(request))
    Future.successful(BadRequest(html))
  }

  private def handleInvoiceNumberSave(value: String, mode: Mode, userAnswers: UserAnswers)(implicit
    request: Request[AnyContent]
  ): Future[Result] = {

    val changed = !userAnswers.get(InvoiceNumberPage).contains(value)

    val answersWithVrnFlag =
      if (userAnswers.get(VrnWarningFlowPage).isDefined && changed) {
        userAnswers.set(VrnWarningFlowPage, false).getOrElse(userAnswers)
      } else { userAnswers }

    val wasShown = answersWithVrnFlag.get(pages.SupplierTaxIdentifierWarningShownPage).contains(true)

    if (wasShown) {
      if (!changed) Future.successful(Redirect(controllers.warning.routes.SupplierTaxIdentifierWarningController.onPageLoad(mode)))
      else {
        val clearedTry = for {
          setVal  <- answersWithVrnFlag.set(InvoiceNumberPage, value)
          cleared <- setVal.remove(pages.SupplierTaxIdentifierWarningShownPage)
        } yield cleared

        Future.fromTry(clearedTry).flatMap { ua =>
          sessionRepository.set(ua).map(_ => Redirect(routes.SupplierTaxIdentifierNumberController.onPageLoad(mode)))
        }
      }
    } else {
      if (mode == CheckMode) {
        CheckModeShortCircuit.shortCircuitIfUnchanged(
          InvoiceNumberPage,
          value,
          mode,
          answersWithVrnFlag,
          routes.CheckYourPurchaseDetailsController.onPageLoad()
        ) match {
          case Some(res) => Future.successful(res)
          case None =>
            val isGermany = answersWithVrnFlag.get(pages.RefundingCountryPage).exists(_.equalsIgnoreCase("DE"))

            val userAnswersTry = if (isGermany) {
              for {
                setVal  <- answersWithVrnFlag.set(InvoiceNumberPage, value)
                marked1 <- setVal.set(pages.SupplierTaxIdentifierArrivedFromInvoicePage, true)
                marked2 <- marked1.set(pages.SupplierVatRegistrationArrivedFromInvoicePage, true)
              } yield marked2
            } else {
              answersWithVrnFlag.set(InvoiceNumberPage, value)
            }

            Future.fromTry(userAnswersTry).flatMap { updated =>
              sessionRepository.set(updated).flatMap { _ =>
                if (isGermany) {
                  updated.get(pages.SupplierVatRegistrationNumberPage) match {
                    case Some(_) => Future.successful(Redirect(routes.SupplierVatRegistrationNumberController.onPageLoad(mode)))
                    case None =>
                      updated.get(pages.SupplierTaxIdentifierNumberPage) match {
                        case Some(_) => Future.successful(Redirect(routes.SupplierTaxIdentifierNumberController.onPageLoad(mode)))
                        case None =>
                          updated.get(pages.SupplierTaxNumberPage) match {
                            case Some(models.SupplierTaxNumber.Vatregistrationnumber) =>
                              Future.successful(Redirect(routes.SupplierVatRegistrationNumberController.onPageLoad(mode)))
                            case Some(models.SupplierTaxNumber.Taxidentifiernumber) =>
                              Future.successful(Redirect(routes.SupplierTaxIdentifierNumberController.onPageLoad(mode)))
                            case _ => Future.successful(Redirect(routes.CheckYourPurchaseDetailsController.onPageLoad()))
                          }
                      }
                  }
                } else {
                  Future.successful(Redirect(routes.CheckYourPurchaseDetailsController.onPageLoad()))
                }
              }
            }
        }
      } else {
        CheckModeShortCircuit(
          InvoiceNumberPage,
          value,
          mode,
          answersWithVrnFlag,
          sessionRepository,
          navigator.nextPage(InvoiceNumberPage, mode, answersWithVrnFlag),
          updated => Future.successful(Redirect(navigator.nextPage(InvoiceNumberPage, mode, updated)))
        )
      }
    }
  }

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
    val preparedForm = request.userAnswers.get(InvoiceNumberPage) match {
      case None        => form // no saved value -> blank form
      case Some(value) => form.fill(value) // existing value -> pre-fill
    }

    Ok(view(preparedForm, mode, routes.InvoiceTypeController.onPageLoad(NormalMode)))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    form
      .bindFromRequest()
      .fold(
        formWithErrors => badRequestView(formWithErrors, mode),
        value => handleInvoiceNumberSave(value, mode, request.userAnswers)
      )
  }
}
