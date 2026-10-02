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
import controllers.actions._
import forms.imports.SupplierAddressFormProvider
import models.Mode
import pages.RefundingCountryPage
import pages.imports.SupplierAddressPage
import play.api.i18n.I18nSupport
import play.api.mvc._
import repositories.SessionRepository
import views.html.imports.SupplierAddressView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SupplierAddressController @Inject() (
                                            identify: IdentifierAction,
                                            getData: DataRetrievalAction,
                                            requireData: DataRequiredAction,
                                            formProvider: SupplierAddressFormProvider,
                                            sessionRepository: SessionRepository,
                                            config: FrontendAppConfig,
                                            val controllerComponents: MessagesControllerComponents,
                                            view: SupplierAddressView
                                          )(implicit ec: ExecutionContext)
  extends BaseController
    with I18nSupport {

  private val form = formProvider()

  val backLink =
    Call("GET", "/import/supplier-name")
  // TODO: Replace with:
  // controllers.imports.routes.ImportSuppliersNameController.onPageLoad()

  def onPageLoad(mode: Mode): Action[AnyContent] =
    (identify andThen getData andThen requireData) { implicit request =>
      val preparedForm =
        request.userAnswers.get(SupplierAddressPage) match {
          case None        => form
          case Some(value) => form.fill(value)
        }

      Ok(
        view(
          preparedForm,
          config.supplierCountries,
          mode,
          backLink
        )
      )
    }

  def onSubmit(mode: Mode): Action[AnyContent] =
    (identify andThen getData andThen requireData).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            Future.successful(
              BadRequest(
                view(
                  formWithErrors,
                  config.supplierCountries,
                  mode,
                  backLink
                )
              )
            ),
          value =>
            val nextPage =
              request.userAnswers.get(RefundingCountryPage) match {
                case Some("EE") =>
                  controllers.imports.routes.ImportCurrencyController.onPageLoad(mode)

                // TODO: Check temporary redirect once dependent DTR-8186 import journey PR is merged.
//                 case Some(_) =>
//                   controllers.imports.routes.TotalAmountWithoutVatController.onPageLoad(mode)

                case _ =>
                  controllers.routes.JourneyRecoveryController.onPageLoad()
              }
              
            for {
              updatedAnswers <- Future.fromTry(
                request.userAnswers.set(SupplierAddressPage, value)
              )
              _ <- sessionRepository.set(updatedAnswers)
            } yield Redirect(nextPage)
        )
    }
}
