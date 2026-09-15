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
import forms.PurchaseOrImportFormProvider

import javax.inject.Inject
import models.{Mode, NormalMode, PurchaseOrImport, UserAnswers}
import navigation.Navigator
import pages.*
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.Settable
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.PurchaseOrImportView

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class PurchaseOrImportController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: PurchaseOrImportFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: PurchaseOrImportView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  val form: Form[PurchaseOrImport] = formProvider()

  private def backLink = routes.BeforeYouStartController.onPageLoad()

  def onPageLoad: Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    for {
      cleared <- Future.fromTry(clearPurchaseJourney(request.userAnswers))
      _       <- sessionRepository.set(cleared)
    } yield {
      val preparedForm = cleared.get(PurchaseOrImportPage).fold(form)(form.fill)
      Ok(view(preparedForm, backLink))
    }
  }

  def onSubmit: Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>

    form
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, backLink))),
        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(PurchaseOrImportPage, value))
            _              <- sessionRepository.set(updatedAnswers)
          } yield Redirect(navigator.nextPage(PurchaseOrImportPage, NormalMode, updatedAnswers))
      )
  }

  private val purchaseJourneyPages: List[Settable[_]] = List(
    PurchaseTypePage,
    PurchaseSubTypePage,
    PurchaseSubTypeLabelPage,
    PurchaseSubCategoryPage,
    PurchaseSubCategoryLabelPage,
    DescribeItemsOnInvoicePage,
    InvoiceTypePage,
    InvoiceNumberPage,
    InvoiceDatePage,
    RefundingCurrencyPage,
    SimplifiedInvoiceVatRegCheckPage,
    SuppliersNamePage,
    SupplierAddressPage,
    SupplierVatRegistrationNumberPage,
    SupplierTaxIdentifierNumberPage,
    SupplierTaxNumberPage,
    TotalPurchaseAmountBeforeVatPage,
    TotalVatPaidPage,
    TotalVatClaimPage
  )

  private def clearPurchaseJourney(userAnswers: UserAnswers): Try[UserAnswers] =
    purchaseJourneyPages.foldLeft(Try(userAnswers)) { (acc, page) =>
      acc.flatMap(_.remove(page))
    }
}
