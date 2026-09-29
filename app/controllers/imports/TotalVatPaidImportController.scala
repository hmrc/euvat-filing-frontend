package controllers

import controllers.actions._
import forms.TotalVatPaidImportFormProvider
import javax.inject.Inject
import models.Mode
import navigation.Navigator
import pages.TotalVatPaidImportPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.TotalVatPaidImportView

import scala.concurrent.{ExecutionContext, Future}

class TotalVatPaidImportController @Inject()(
                                        override val messagesApi: MessagesApi,
                                        sessionRepository: SessionRepository,
                                        navigator: Navigator,
                                        identify: IdentifierAction,
                                        getData: DataRetrievalAction,
                                        requireData: DataRequiredAction,
                                        formProvider: TotalVatPaidImportFormProvider,
                                        val controllerComponents: MessagesControllerComponents,
                                        view: TotalVatPaidImportView
                                      )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  val form: Form[BigDecimal] = formProvider()

  private def backLink(mode: Mode) = if (mode == CheckMode) {
     routes.CheckYourPurchaseDetailsController.onPageLoad()
  } else {
     routes.TotalPurchaseAmountBeforeVatController.onPageLoad(NormalMode)
  }

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
      val preparedForm = request.userAnswers.get(TotalVatPaidImportPage).fold(form)(form.fill)
      val (currencyName, prefix) = currencyNameAndPrefix(request.userAnswers, currencyConfig.currencyConfig)
      Ok(view(preparedForm, mode, backLink(mode), prefix, currencyName))
    }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
     val (currencyName, prefix) = currencyNameAndPrefix(request.userAnswers, currencyConfig.currencyConfig)
      form.bindFromRequest().fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, mode, backLink(mode), prefix, currencyName))),
        value =>
          for {
            userAnswers <- Future.fromTry(request.userAnswers.set(TotalVatPaidImportPage, value))
            _              <- sessionRepository.set(userAnswers)
          } yield Redirect(navigator.nextPage(TotalVatPaidImportPage, mode, updatedAnswers))
      )
  }
}


         // } yield {
         //  val amountBeforeVat: BigDecimal = userAnswers.get(TotalPurchaseAmountBeforeVatPage).getOrElse(BigDecimal(0))
         //   if (value > amountBeforeVat) {
         //     Redirect(controllers.warning.routes.VatPaidWarningController.onPageLoad(mode))
         //  } else {
         //    Redirect(navigator.nextPage(TotalVatPaidPage, mode, userAnswers))
         //  }


