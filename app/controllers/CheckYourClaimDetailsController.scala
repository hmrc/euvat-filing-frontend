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

import com.google.inject.Inject
import controllers.actions.{DataRequiredAction, DataRetrievalAction, IdentifierAction}
import models.UserAnswers
import models.requests.ApplicationRequest
import models.responses.ApplicationResponse
import pages.{BusinessActivityCodePage, BusinessActivityCodeThreePage, BusinessActivityCodeTwoPage, ClaimApplicationResponsePage, ClaimDetailsCompletedPage, ContactDetailsPage, RefundPeriodPage, RefundingCountryPage, RefundingCurrencyPage, RefundingLanguagePage}
import play.api.Logging
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import services.EuVatRefundsService
import uk.gov.hmrc.govukfrontend.views.Aliases.SummaryList
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.checkAnswers.CheckYourClaimDetailsSummary
import utils.ConfigLanguageMapping
import views.html.CheckYourClaimDetailsView
import viewmodels.govuk.summarylist.*

import scala.concurrent.ExecutionContext

class CheckYourClaimDetailsController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  val controllerComponents: MessagesControllerComponents,
  view: CheckYourClaimDetailsView,
  sessionRepository: SessionRepository,
  configLanguageMapping: ConfigLanguageMapping,
  service: EuVatRefundsService
)(using ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad(): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
    val summaryList = buildSummaryList(request.userAnswers)

    Ok(view(summaryList))
  }

  def onSubmit(): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>

    val userAnswers = request.userAnswers
    for {
      updatedAnswer1 <- userAnswers.set(ClaimDetailsCompletedPage, true).getOrElse(userAnswers)
      appRequest     <- buildAppRequest(updatedAnswer1)
      claimResponse  <- service.createApplication(appRequest)
      updatedAnswers <- updatedAnswer1.set(ClaimApplicationResponsePage, claimResponse)
      _              <- sessionRepository.set(updatedAnswers)
    } yield {
      if (claimResponse.applicationId > 0) {
        Redirect(controllers.routes.TaskListDashboardController.onPageLoad())
      } else {
        Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
      }
    }

  }

  private def getChangeUrl(rowOpt: SummaryListRow): Option[String] =
    rowOpt.actions.flatMap(_.items.headOption.map(_.href))

  private def buildSummaryList(answers: UserAnswers)(implicit messages: Messages): Seq[(String, Option[String], SummaryList)] =
    Seq(
      (
        "checkYourClaimDetails.refundingCountry.label",
        getChangeUrl(CheckYourClaimDetailsSummary.rowCountryLabel()),
        SummaryListViewModel(Seq(CheckYourClaimDetailsSummary.rowCountry(answers)).flatten)
      )
    ) ++ {
      val maybeCountryCode = answers.get(pages.RefundingCountryPage).orElse {
        answers.get(pages.RefundingCountryNamePage).map { stored =>
          stored.split(",", 2).headOption.getOrElse(stored)
        }
      }

      maybeCountryCode match {
        case Some(code) if configLanguageMapping.languagesFor(code).size > 1 =>
          Seq(
            (
              "checkYourClaimDetails.refundingLanguage.label",
              getChangeUrl(CheckYourClaimDetailsSummary.rowLanguageLabel()),
              SummaryListViewModel(Seq(CheckYourClaimDetailsSummary.rowLanguage(answers)).flatten)
            )
          )
        case _ => Seq.empty
      }
    } ++ Seq(
      (
        "checkYourClaimDetails.refundingPeriod.label",
        getChangeUrl(CheckYourClaimDetailsSummary.rowRefundPeriodLabel()),
        SummaryListViewModel(Seq(CheckYourClaimDetailsSummary.rowRefundStart(answers), CheckYourClaimDetailsSummary.rowRefundEnd(answers)).flatten)
      ),
      (
        "checkYourClaimDetails.contactDetails.label",
        getChangeUrl(CheckYourClaimDetailsSummary.rowContactLabel()),
        SummaryListViewModel(
          Seq(CheckYourClaimDetailsSummary.rowContactEmail(answers), CheckYourClaimDetailsSummary.rowContactPhone(answers)).flatten
        )
      ),
      (
        "checkYourClaimDetails.businessActivity.label",
        getChangeUrl(CheckYourClaimDetailsSummary.rowBusinessActivityLabel(answers)),
        SummaryListViewModel(
          Seq(
            CheckYourClaimDetailsSummary.rowBusinessActivity(answers),
            CheckYourClaimDetailsSummary.rowBusinessActivity2(answers),
            CheckYourClaimDetailsSummary.rowBusinessActivity3(answers)
          ).flatten
        )
      )
    )

  private def buildAppRequest(userAnswers: UserAnswers): ApplicationRequest = {
    val countryCode = userAnswers
      .get(RefundingCountryPage)
      .getOrElse(throw new RuntimeException("Country code missing"))
    val currencyCode = userAnswers
      .get(RefundingCurrencyPage)
      .getOrElse(throw new RuntimeException("Currency code missing"))
    val languageCode = userAnswers
      .get(RefundingLanguagePage)
      .getOrElse(throw new RuntimeException("Language code missing"))
    val refundStartDate = userAnswers
      .get(RefundPeriodPage)
      .map(_.startDate)
      .getOrElse(throw new RuntimeException("RefundPeriodPage startDate missing"))
    val refundEndDate = userAnswers
      .get(RefundPeriodPage)
      .map(_.endDate)
      .getOrElse(throw new RuntimeException("RefundPeriodPage endDate missing"))
    val email = userAnswers
      .get(ContactDetailsPage)
      .map(_.email)
      .getOrElse(throw new RuntimeException("Email contact detail missing"))
    val telephone = userAnswers
      .get(ContactDetailsPage)
      .map(_.email)
      .getOrElse(throw new RuntimeException("Telephone contact detail missing"))
    val businessActivityCode1 = userAnswers
      .get(BusinessActivityCodePage)
      .getOrElse(throw new RuntimeException("Telephone contact detail missing"))
    val businessActivityCode2 = userAnswers.get(BusinessActivityCodeTwoPage).getOrElse("")
    val businessActivityCode3 = userAnswers.get(BusinessActivityCodeThreePage).getOrElse("")

    ApplicationRequest(
      applicantVatRegNumber    = "",
      applicationLanguage      = Some(languageCode),
      applicantEmailAddress    = Some(email),
      applicantTelephoneNumber = Some(telephone),
      refundingCountryCode     = Some(countryCode),
      periodStartDate          = Some(refundStartDate),
      periodEndDate            = Some(refundEndDate),
      businessActivityCode1    = Some(businessActivityCode1),
      businessActivityCode2    = Some(businessActivityCode2),
      businessActivityCode3    = Some(businessActivityCode3)
    )
  }

}
