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

import base.SpecBase
import forms.ContactDetailsFormProvider
import models.{ContactDetails, NormalMode, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.ContactDetailsPage
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import views.html.ContactDetailsView

import scala.concurrent.Future

class ContactDetailsControllerSpec extends SpecBase with MockitoSugar {

  private def onwardRoute = Call("GET", "/foo")

  private val formProvider = new ContactDetailsFormProvider()
  private val form         = formProvider()

  private lazy val contactDetailsRoute = routes.ContactDetailsController.onPageLoad(NormalMode).url

  private val validFormData = Map(
    "contactEmail"     -> "test@example.com",
    "contactFirstName" -> "Jane",
    "contactLastName"  -> "Doe",
    "contactTelephone" -> "07700900000"
  )

  private val contactDetails = ContactDetails(
    email     = "test@example.com",
    firstName = Some("Jane"),
    lastName  = Some("Doe"),
    telephone = Some("07700900000")
  )

  "ContactDetailsController" - {

    "onPageLoad" - {

      "must return OK and render the view with an empty form when no existing data" in {
        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, contactDetailsRoute)
          val result  = route(application, request).value
          val view    = application.injector.instanceOf[ContactDetailsView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(form, NormalMode)(request, messages(application)).toString
        }
      }

      "must pre-populate the form when existing data is present" in {
        val userAnswers = emptyUserAnswers.set(ContactDetailsPage, contactDetails).success.value
        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, contactDetailsRoute)
          val result  = route(application, request).value
          val view    = application.injector.instanceOf[ContactDetailsView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(form.fill(contactDetails), NormalMode)(request, messages(application)).toString
        }
      }

      "must redirect to Journey Recovery when no user answers exist" in {
        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val request = FakeRequest(GET, contactDetailsRoute)
          val result  = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }

    "onSubmit" - {

      "must save data and redirect on valid submission" in {
        val mockSessionRepository = mock[SessionRepository]
        when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

        running(application) {
          val request = FakeRequest(POST, contactDetailsRoute).withFormUrlEncodedBody(validFormData.toSeq*)
          val result  = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual onwardRoute.url
        }
      }

      "must return BAD_REQUEST and render the view with errors when email is missing" in {
        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

        running(application) {
          val request = FakeRequest(POST, contactDetailsRoute).withFormUrlEncodedBody("contactEmail" -> "")
          val result  = route(application, request).value
          val view    = application.injector.instanceOf[ContactDetailsView]

          status(result) mustEqual BAD_REQUEST
          contentAsString(result) mustEqual view(
            form.bind(Map("contactEmail" -> "")),
            NormalMode
          )(request, messages(application)).toString
        }
      }

      "must return BAD_REQUEST when email format is invalid" in {
        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

        running(application) {
          val request = FakeRequest(POST, contactDetailsRoute)
            .withFormUrlEncodedBody(validFormData.updated("contactEmail", "not-an-email").toSeq*)
          val result = route(application, request).value

          status(result) mustEqual BAD_REQUEST
        }
      }

      "must redirect to Journey Recovery when no user answers exist" in {
        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val request = FakeRequest(POST, contactDetailsRoute).withFormUrlEncodedBody(validFormData.toSeq*)
          val result  = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }
  }
}
