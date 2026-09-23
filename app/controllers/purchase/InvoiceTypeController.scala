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
import controllers.helpers.PurchaseBackLinkHelper
import forms.purchase.InvoiceTypeFormProvider
import models.requests.DataRequest
import models.{CheckMode, InvoiceType, Mode, NormalMode, Other, PurchaseType, UserAnswers}
import navigation.Navigator
import pages.*
import play.api.Logging
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.{ConfigPurchaseMapping, CountryCode}
import views.html.purchase.InvoiceTypeView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class InvoiceTypeController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  configPurchaseMapping: ConfigPurchaseMapping,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: InvoiceTypeFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: InvoiceTypeView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  val form: Form[InvoiceType] = formProvider()

  private def computeBackTarget(mode: Mode)(implicit request: DataRequest[?]): Call = {
    def parentIsNone = request.userAnswers.get(PurchaseSubTypePage).exists(v => v.split("\\.").lastOption.contains("99"))
    def childIsNone = request.userAnswers.get(PurchaseSubCategoryPage).exists(v => v.split("\\.").lastOption.contains("99"))
    def isOther = request.userAnswers.get(PurchaseTypePage).contains(Other)

    if (mode == CheckMode) {
      routes.CheckYourPurchaseDetailsController.onPageLoad()
    } else {
      if (!isOther) {
        PurchaseBackLinkHelper.computeBackTarget(NormalMode)
      } else if (parentIsNone || childIsNone) {
        routes.DescribeItemsOnInvoiceController.onPageLoad(NormalMode)
      } else {
        PurchaseBackLinkHelper.computeBackTarget(NormalMode)
      }
    }
  }

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    val preparedForm = request.userAnswers.get(InvoiceTypePage).fold(form)(form.fill)
    val back = computeBackTarget(mode)
    Future.successful(Ok(view(preparedForm, mode, back)))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    form
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, mode, computeBackTarget(mode))(request, messagesApi.preferred(request)))),
        value => {
          if (request.userAnswers.isAnswerUnchanged(InvoiceTypePage, value)) {
            for {
              answers <- Future.fromTry(request.userAnswers.set(InvoiceTypePage, value))
              _       <- sessionRepository.set(answers)
            } yield {
              if (mode == CheckMode) {
                Redirect(routes.CheckYourPurchaseDetailsController.onPageLoad())
              } else {
                Redirect(routes.InvoiceNumberController.onPageLoad(NormalMode))
              }
            }
          } else {
            for {
              answers  <- Future.fromTry(request.userAnswers.set(InvoiceTypePage, value))
              answers1 <- Future.fromTry(answers.remove(SimplifiedInvoiceVatRegCheckPage))
              answers2 <- Future.fromTry(answers1.remove(SupplierTaxNumberPage))
              _        <- sessionRepository.set(answers2)
            } yield postRedirect(mode, value, answers2)
          }
        }
      )
  }

  private def postRedirect(mode: Mode, value: InvoiceType, updatedAnswers: UserAnswers) = {
    if (mode == CheckMode) {
      val countryOpt = CountryCode.findCountryCode(updatedAnswers)
      countryOpt match {
        case Some("DE") => Redirect(routes.SupplierTaxNumberController.onPageLoad(CheckMode))
        case _ =>
          value match {
            case InvoiceType.StandardInvoice   => Redirect(routes.SupplierVatRegistrationNumberController.onPageLoad(CheckMode))
            case InvoiceType.SimplifiedInvoice => Redirect(routes.SimplifiedInvoiceVatRegCheckController.onPageLoad(CheckMode))
          }
      }
    } else {
      Redirect(navigator.nextPage(InvoiceTypePage, mode, updatedAnswers))
    }
  }

}
