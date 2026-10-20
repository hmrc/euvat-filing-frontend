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

import base.SpecBase
import controllers.routes
import forms.ImportTypeFormProvider
import models.{CheckMode, Fuel, NormalMode, PurchaseOrImportType, Transport, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{never, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.ImportTypePage
import pages.imports.ImportTypeChangedPage
import play.api.data.Form
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import views.html.PurchaseOrImportTypeView

import scala.concurrent.Future

class ImportTypeControllerSpec extends SpecBase with MockitoSugar {
  def onwardRoute: Call = Call("GET", "/foo")
  lazy val importTypeRoute: String = controllers.imports.routes.ImportTypeController.onPageLoad(NormalMode).url
  lazy val backLinkCall: Call = controllers.routes.PurchaseOrImportController.onPageLoad
  lazy val submitCall: Call = controllers.imports.routes.ImportTypeController.onSubmit(NormalMode)
  val formProvider = new ImportTypeFormProvider()
  val form: Form[PurchaseOrImportType] = formProvider()

  "ImportType Controller" - {

    "must return OK and the correct view for a GET" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, importTypeRoute)
        val result = route(application, request).value
        val view = application.injector.instanceOf[PurchaseOrImportTypeView]

        status(result) mustEqual OK
        normalizeHtml(contentAsString(result)) mustEqual normalizeHtml(
          view(form, NormalMode, backLinkCall, submitCall, "importType", "import.caption", showPurchaseIntro = false)(
            request,
            messages(application)
          ).toString
        )
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {
      val userAnswers = UserAnswers(userAnswersId).set(ImportTypePage, Fuel).success.value
      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, importTypeRoute)
        val view = application.injector.instanceOf[PurchaseOrImportTypeView]
        val result = route(application, request).value

        status(result) mustEqual OK
        normalizeHtml(contentAsString(result)) mustEqual normalizeHtml(
          view(form.fill(Fuel), NormalMode, backLinkCall, submitCall, "importType", "import.caption", showPurchaseIntro = false)(
            request,
            messages(application)
          ).toString
        )
      }
    }

    "must redirect to the next page when valid data is submitted" in {
      val mockSessionRepository = mock[SessionRepository]
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, importTypeRoute)
            .withFormUrlEncodedBody(("value", Fuel.toString))

        val result = route(application, request).value
        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, importTypeRoute)
            .withFormUrlEncodedBody(("value", "invalid value"))

        val boundForm = form.bind(Map("value" -> "invalid value"))
        val view = application.injector.instanceOf[PurchaseOrImportTypeView]
        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        normalizeHtml(contentAsString(result)) mustEqual normalizeHtml(
          view(boundForm, NormalMode, backLinkCall, submitCall, "importType", "import.caption", showPurchaseIntro = false)(
            request,
            messages(application)
          ).toString
        )
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {
      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, importTypeRoute)
        val result = route(application, request).value
        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "redirect to Journey Recovery for a POST if no existing data is found" in {
      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, importTypeRoute)
            .withFormUrlEncodedBody(("value", Fuel.toString))

        val result = route(application, request).value
        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must save an unchanged Import type with a false flag and delegate in CheckMode" in {
      val answers = emptyUserAnswers
        .set(ImportTypePage, Fuel)
        .success.value

      val repository = mock[SessionRepository]

      when(repository.set(any[UserAnswers]))
        .thenReturn(Future.successful(true))

      val expectedAnswers = answers
        .set(ImportTypePage, Fuel)
        .flatMap(_.set(ImportTypeChangedPage, false))
        .success.value

      val application = applicationBuilder(userAnswers = Some(answers))
        .overrides(
          bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
          bind[SessionRepository].toInstance(repository)
        )
        .build()

      running(application) {
        val request = FakeRequest(
          POST,
          controllers.imports.routes.ImportTypeController.onSubmit(CheckMode).url
        ).withFormUrlEncodedBody("value" -> Fuel.toString)

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe onwardRoute.url

        verify(repository).set(expectedAnswers)
      }
    }

    "must save the changed Import type and use the navigator in CheckMode" in {
      val answers = emptyUserAnswers
        .set(ImportTypePage, Fuel)
        .success.value

      val repository = mock[SessionRepository]
      when(repository.set(any[UserAnswers]))
        .thenReturn(Future.successful(true))

      val expectedAnswers = answers
        .set(ImportTypePage, Transport)
        .success.value

      val application = applicationBuilder(userAnswers = Some(answers))
        .overrides(
          bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
          bind[SessionRepository].toInstance(repository)
        )
        .build()

      running(application) {
        val request = FakeRequest(
          POST,
          controllers.imports.routes.ImportTypeController.onSubmit(CheckMode).url
        ).withFormUrlEncodedBody("value" -> Transport.toString)

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe onwardRoute.url

        verify(repository).set(expectedAnswers)
      }
    }

    "must save and use the navigator when no previous Import type exists in CheckMode" in {
      val repository = mock[SessionRepository]
      when(repository.set(any[UserAnswers]))
        .thenReturn(Future.successful(true))

      val initialAnswers = emptyUserAnswers

      val expectedAnswers = initialAnswers
        .set(ImportTypePage, Fuel)
        .success.value

      val application = applicationBuilder(userAnswers = Some(initialAnswers))
        .overrides(
          bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
          bind[SessionRepository].toInstance(repository)
        )
        .build()

      running(application) {
        val request = FakeRequest(
          POST,
          controllers.imports.routes.ImportTypeController.onSubmit(CheckMode).url
        ).withFormUrlEncodedBody("value" -> Fuel.toString)

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe onwardRoute.url

        verify(repository).set(expectedAnswers)
      }
    }

    "must continue through the normal journey when Import type is unchanged in NormalMode" in {
      val answers = emptyUserAnswers
        .set(ImportTypePage, Fuel)
        .success.value

      val repository = mock[SessionRepository]
      when(repository.set(any[UserAnswers]))
        .thenReturn(Future.successful(true))

      val expectedAnswers = answers
        .set(ImportTypePage, Fuel)
        .success.value

      val application = applicationBuilder(userAnswers = Some(answers))
        .overrides(
          bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
          bind[SessionRepository].toInstance(repository)
        )
        .build()

      running(application) {
        val request = FakeRequest(
          POST,
          controllers.imports.routes.ImportTypeController.onSubmit(NormalMode).url
        ).withFormUrlEncodedBody("value" -> Fuel.toString)

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe onwardRoute.url

        verify(repository).set(expectedAnswers)
      }
    }

    "must return Bad Request without saving when invalid data is submitted in CheckMode" in {
      val answers = emptyUserAnswers
        .set(ImportTypePage, Fuel)
        .success.value

      val repository = mock[SessionRepository]

      val application = applicationBuilder(userAnswers = Some(answers))
        .overrides(
          bind[SessionRepository].toInstance(repository)
        )
        .build()

      running(application) {
        val request = FakeRequest(
          POST,
          controllers.imports.routes.ImportTypeController.onSubmit(CheckMode).url
        ).withFormUrlEncodedBody("value" -> "invalid value")

        val result = route(application, request).value

        status(result) mustBe BAD_REQUEST
        verify(repository, never()).set(any[UserAnswers])
      }
    }
  }
}
