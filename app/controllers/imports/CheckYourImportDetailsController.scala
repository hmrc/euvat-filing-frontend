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
import controllers.actions.{DataRequiredAction, DataRetrievalAction, IdentifierAction}
import models.NormalMode
import navigation.ImportNavigator
import pages.imports.ImportMissingAnswersJourneyPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.{ConfigPurchaseOrImportMapping, CurrencyConfig}
import viewmodels.checkAnswers.CheckYourImportDetailsSummary
import views.html.imports.CheckYourImportDetailsView
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CheckYourImportDetailsController @Inject()(
                                                   override val messagesApi: MessagesApi,
                                                   identify: IdentifierAction,
                                                   getData: DataRetrievalAction,
                                                   requireData: DataRequiredAction,
                                                   val controllerComponents: MessagesControllerComponents,
                                                   view: CheckYourImportDetailsView,
                                                   configPurchaseOrImportMapping: ConfigPurchaseOrImportMapping,
                                                   currencyConfig: CurrencyConfig,
                                                   appConfig: FrontendAppConfig,
                                                   sessionRepository: SessionRepository,
                                                   importNavigator: ImportNavigator
                                                 )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad(): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    val answers = request.userAnswers
    val reset = if (answers.get(ImportMissingAnswersJourneyPage).contains(true)) {
      for {
        updated <- Future.fromTry(answers.remove(ImportMissingAnswersJourneyPage))
        _ <- sessionRepository.set(updated)
      } yield updated
    } else Future.successful(answers)

    reset.map { updated =>
      Ok(view(
        CheckYourImportDetailsSummary.sections(updated, configPurchaseOrImportMapping, currencyConfig, appConfig.supplierCountries),
        routes.CheckYourImportDetailsController.onSubmit(),
        routes.TotalVatClaimController.onPageLoad(NormalMode)
      ))
    }
  }

  def onSubmit(): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    for {
      updated <- Future.fromTry(request.userAnswers.set(ImportMissingAnswersJourneyPage, true))
      _ <- sessionRepository.set(updated)
    } yield Redirect(importNavigator.navigateFromCheckYourImportDetails(updated))
  }
}
