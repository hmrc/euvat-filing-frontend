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
import controllers.actions.*
import forms.imports.ImportSupplierAddressFormProvider
import models.Mode
import pages.imports.ImportSupplierAddressPage
import play.api.i18n.I18nSupport
import play.api.mvc.*
import navigation.Navigator
import repositories.SessionRepository
import views.html.imports.ImportSupplierAddressView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ImportSupplierAddressController @Inject() (
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: ImportSupplierAddressFormProvider,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  config: FrontendAppConfig,
  val controllerComponents: MessagesControllerComponents,
  view: ImportSupplierAddressView
)(implicit ec: ExecutionContext)
    extends BaseController
    with I18nSupport {

  private val form = formProvider()

  private val backLink: Call =
    Call("GET", "/file-eu-vat/import/supplier-name")
// TODO: Recheck after RA5.4 is merged and replace teh above line with the commented one below

//  private val backLink: Call =
//    controllers.imports.routes.ImportSuppliersNameController.onPageLoad()

  def onPageLoad(mode: Mode): Action[AnyContent] =
    (identify andThen getData andThen requireData) { implicit request =>
      val preparedForm =
        request.userAnswers.get(ImportSupplierAddressPage) match {
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
            for {
              updatedAnswers <- Future.fromTry(
                                  request.userAnswers.set(ImportSupplierAddressPage, value)
                                )
              _ <- sessionRepository.set(updatedAnswers)
            } yield Redirect(
              navigator.nextPage(
                ImportSupplierAddressPage,
                mode,
                updatedAnswers
              )
            )
        )
    }
}
