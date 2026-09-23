package controllers

import controllers.actions._
import forms.ImportDateFormProvider
import javax.inject.Inject
import models.Mode
import navigation.Navigator
import pages.ImportDatePage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.ImportDateView

import scala.concurrent.{ExecutionContext, Future}

class ImportDateController @Inject()(
                                        override val messagesApi: MessagesApi,
                                        sessionRepository: SessionRepository,
                                        navigator: Navigator,
                                        identify: IdentifierAction,
                                        getData: DataRetrievalAction,
                                        requireData: DataRequiredAction,
                                        formProvider: ImportDateFormProvider,
                                        val controllerComponents: MessagesControllerComponents,
                                        view: ImportDateView
                                      )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

   private def form(implicit messages: Messages) = formProvider()
   private def backLink(mode: Mode) = if (mode == CheckMode) {
     routes.CheckYourPurchaseDetailsController.onPageLoad()
   } else {
     routes.ImportDetailsInfoController.onPageLoad(NormalMode)
   }

   def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
     val preparedForm = request.userAnswers.get(ImportDatePage).fold(form)(form.fill)
     Ok(view(preparedForm, mode, backLink(mode)))
   }

   def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
     form
       .bindFromRequest()
       .fold(
         formWithErrors => badRequestToImportDate(formWithErrors, mode),
         value =>
           val today = java.time.LocalDate.now()
           if (value.isAfter(today)) {
             val errorForm = form.bindFromRequest().withError("value", "importDate.error.past")
             badRequestToImportDate(errorForm, mode)
           } else {
             handleSubmission(value, mode)(request)
           }
       )
   }

   private def badRequestToImportDate(formWithErrors: Form[?], mode: Mode)(implicit
     request: Request[AnyContent]
   ): Future[play.api.mvc.Result] = {
     val html = view(formWithErrors, mode, backLink(mode))(request, messagesApi.preferred(request))
     Future.successful(BadRequest(html))
   }

   private def handleSubmission(value: LocalDate, mode: Mode)(implicit request: DataRequest[?]): Future[Result] = {
     if (mode == CheckMode && request.userAnswers.isAnswerUnchanged(ImportDatePage, value)) {
       Future.successful(Redirect(routes.CheckYourPurchaseDetailsController.onPageLoad()))
     } else {
       for {
         updatedAnswers <- Future.fromTry(request.userAnswers.set(ImportDatePage, value))
         _              <- sessionRepository.set(updatedAnswers)
       } yield {
         if (mode == CheckMode) {
           Redirect(routes.CheckYourPurchaseDetailsController.onPageLoad())
         } else {
           Redirect(navigator.nextPage(ImportDatePage, mode, updatedAnswers))
         }
       }
     }
   }
 }
