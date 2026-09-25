package controllers.imports

import controllers.actions.*
import forms.RefundingCurrencyFormProvider
import models.requests.DataRequest
import models.{Mode, RefundingCurrency}
import navigation.Navigator
import pages.ImportCurrencyPage
import play.api.Logger
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.*
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.*
import views.html.RefundingCurrencyView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ImportCurrencyController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: RefundingCurrencyFormProvider,
  currencyConfig: CurrencyConfig,
  val controllerComponents: MessagesControllerComponents,
  view: RefundingCurrencyView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  private val messagePrefix = "import.refundingCurrency"
  val form: Form[RefundingCurrency] = formProvider(messagePrefix)
  private val logger = Logger(getClass)

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
    CountryCode.findCountryCode(request.userAnswers) match {
      case None =>
        logger.warn("ImportCurrencyController.onPageLoad - no refunding country in session, redirecting to JourneyRecovery")
        Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
      case Some(countryCode) =>
        val currencies = currencyConfig.currencyConfig(countryCode)
        val preparedForm = request.userAnswers
          .get(ImportCurrencyPage)
          .flatMap(storedCode => currencies.find(_.code == storedCode))
          .flatMap(c => RefundingCurrency.values.find(_.toString.equalsIgnoreCase(c.name)))
          .fold(form)(form.fill)

        Ok(renderView(preparedForm, currencies, mode))
    }
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    CountryCode.findCountryCode(request.userAnswers) match {
      case None =>
        logger.warn("ImportCurrencyController.onSubmit - no refunding country in session, redirecting to JourneyRecovery")
        Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
      case Some(countryCode) =>
        val currencies = currencyConfig.currencyConfig(countryCode)
        form
          .bindFromRequest()
          .fold(
            formWithErrors => Future.successful(BadRequest(renderView(formWithErrors, currencies, mode))),
            value =>
              currencies.collectFirst { case c if c.name.equalsIgnoreCase(value.toString) => c.code } match {
                case None =>
                  logger.warn(s"ImportCurrencyController.onSubmit - could not find currency code for ${value.toString}")
                  Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
                case Some(currencyCode) =>
                  for {
                    updatedAnswers <- Future.fromTry(request.userAnswers.set(ImportCurrencyPage, currencyCode))
                    _              <- sessionRepository.set(updatedAnswers)
                  } yield Redirect(navigator.nextPage(ImportCurrencyPage, mode, updatedAnswers))
              }
          )
    }
  }

  private def renderView(form: Form[?], currencies: Seq[Currency], mode: Mode)(implicit request: DataRequest[?]) =
    view(
      form,
      PurchaseOrImportHelpers.currencyRadioItems(currencies, messagesApi.preferred(request)),
      routes.ImportCurrencyController.onSubmit(mode),
      messagePrefix,
      "import.caption"
    )
}
