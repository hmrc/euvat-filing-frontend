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

package controllers.bankdetails

import base.SpecBase
import config.InternationalCurrencyProvider
import controllers.routes
import forms.bankdetails.BankDetailsFormProvider
import models.{BankDetails, NormalMode, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.bankdetails.BankDetailsPage
import play.api.inject.bind
import play.api.libs.json.Json
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import views.html.bankdetails.BankDetailsView

import scala.concurrent.Future

class BankDetailsControllerSpec extends SpecBase with MockitoSugar {

  private val onwardRoute: Call =
    Call("GET", "/foo")

  private val formProvider =
    new BankDetailsFormProvider()

  private val form =
    formProvider()

  private val validBankDetails =
    BankDetails(
      NameOnTheAccount  = "John Smith",
      BIC               = "NWBKGB2L",
      accountNumber     = "GB29NWBK60161331926819",
      CurrencyOfAccount = "GBP"
    )

  private val validFormData =
    Map(
      "NameOnTheAccount"  -> validBankDetails.NameOnTheAccount,
      "BIC"               -> validBankDetails.BIC,
      "accountNumber"     -> validBankDetails.accountNumber,
      "CurrencyOfAccount" -> validBankDetails.CurrencyOfAccount
    )

  private val userAnswers =
    UserAnswers(
      userAnswersId,
      Json.obj(
        BankDetailsPage.toString -> Json.toJson(validBankDetails)
      )
    )

  private lazy val bankDetailsRoute =
    controllers.bankdetails.routes.BankDetailsController
      .onPageLoad(NormalMode)
      .url

  private lazy val submitBankDetailsRoute =
    controllers.bankdetails.routes.BankDetailsController
      .onSubmit(NormalMode)
      .url

  "BankDetailsController" - {

    "must return OK and the correct view for a GET" in {

      val application =
        applicationBuilder(
          userAnswers = Some(emptyUserAnswers)
        ).build()

      running(application) {
        val request =
          FakeRequest(GET, bankDetailsRoute)

        val view =
          application.injector.instanceOf[BankDetailsView]

        val internationalCurrencyProvider =
          application.injector.instanceOf[InternationalCurrencyProvider]

        val result =
          route(application, request).value

        status(result) mustEqual OK

        contentAsString(result) mustEqual
          view(
            form,
            NormalMode,
            internationalCurrencyProvider.currencies
          )(
            request,
            messages(application)
          ).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val application =
        applicationBuilder(
          userAnswers = Some(userAnswers)
        ).build()

      running(application) {
        val request =
          FakeRequest(GET, bankDetailsRoute)

        val view =
          application.injector.instanceOf[BankDetailsView]

        val internationalCurrencyProvider =
          application.injector.instanceOf[InternationalCurrencyProvider]

        val result =
          route(application, request).value

        status(result) mustEqual OK

        contentAsString(result) mustEqual
          view(
            form.fill(validBankDetails),
            NormalMode,
            internationalCurrencyProvider.currencies
          )(
            request,
            messages(application)
          ).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {

      val mockSessionRepository =
        mock[SessionRepository]

      when(mockSessionRepository.set(any[UserAnswers]()))
        .thenReturn(Future.successful(true))

      val application =
        applicationBuilder(
          userAnswers = Some(emptyUserAnswers)
        )
          .overrides(
            bind[Navigator].toInstance(
              new FakeNavigator(onwardRoute)
            ),
            bind[SessionRepository].toInstance(
              mockSessionRepository
            )
          )
          .build()

      running(application) {
        val request =
          FakeRequest(
            POST,
            submitBankDetailsRoute
          ).withFormUrlEncodedBody(
            validFormData.toSeq*
          )

        val result =
          route(application, request).value

        status(result) mustEqual SEE_OTHER

        redirectLocation(result).value mustEqual
          onwardRoute.url

        verify(mockSessionRepository)
          .set(any[UserAnswers]())
      }
    }

    "must save submitted bank details in UserAnswers" in {

      val mockSessionRepository =
        mock[SessionRepository]

      when(mockSessionRepository.set(any[UserAnswers]()))
        .thenReturn(Future.successful(true))

      val application =
        applicationBuilder(
          userAnswers = Some(emptyUserAnswers)
        )
          .overrides(
            bind[Navigator].toInstance(
              new FakeNavigator(onwardRoute)
            ),
            bind[SessionRepository].toInstance(
              mockSessionRepository
            )
          )
          .build()

      running(application) {

        val request =
          FakeRequest(
            POST,
            submitBankDetailsRoute
          ).withFormUrlEncodedBody(
            validFormData.toSeq*
          )

        val result =
          route(application, request).value

        status(result) mustEqual SEE_OTHER

        val userAnswersCaptor =
          org.mockito.ArgumentCaptor.forClass(classOf[UserAnswers])

        verify(mockSessionRepository)
          .set(userAnswersCaptor.capture())

        val savedUserAnswers =
          userAnswersCaptor.getValue

        savedUserAnswers.get(BankDetailsPage) mustEqual
          Some(validBankDetails)
      }
    }

    "must return a Bad Request and the correct view when invalid data is submitted" in {

      val invalidFormData =
        Map(
          "NameOnTheAccount"  -> "",
          "BIC"               -> "",
          "accountNumber"     -> "",
          "CurrencyOfAccount" -> ""
        )

      val application =
        applicationBuilder(
          userAnswers = Some(emptyUserAnswers)
        ).build()

      running(application) {
        val request =
          FakeRequest(
            POST,
            submitBankDetailsRoute
          ).withFormUrlEncodedBody(
            invalidFormData.toSeq*
          )

        val boundForm =
          form.bind(invalidFormData)

        val view =
          application.injector.instanceOf[BankDetailsView]

        val internationalCurrencyProvider =
          application.injector.instanceOf[InternationalCurrencyProvider]

        val result =
          route(application, request).value

        status(result) mustEqual BAD_REQUEST

        contentAsString(result) mustEqual
          view(
            boundForm,
            NormalMode,
            internationalCurrencyProvider.currencies
          )(
            request,
            messages(application)
          ).toString
      }
    }

    "must not save answers when invalid data is submitted" in {

      val mockSessionRepository =
        mock[SessionRepository]

      val application =
        applicationBuilder(
          userAnswers = Some(emptyUserAnswers)
        )
          .overrides(
            bind[SessionRepository].toInstance(
              mockSessionRepository
            )
          )
          .build()

      running(application) {
        val request =
          FakeRequest(
            POST,
            submitBankDetailsRoute
          ).withFormUrlEncodedBody(
            "NameOnTheAccount"  -> "",
            "BIC"               -> "",
            "accountNumber"     -> "",
            "CurrencyOfAccount" -> ""
          )

        val result =
          route(application, request).value

        status(result) mustEqual BAD_REQUEST

        org.mockito.Mockito
          .verifyNoInteractions(mockSessionRepository)
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application =
        applicationBuilder(
          userAnswers = None
        ).build()

      running(application) {
        val request =
          FakeRequest(GET, bankDetailsRoute)

        val result =
          route(application, request).value

        status(result) mustEqual SEE_OTHER

        redirectLocation(result).value mustEqual
          routes.JourneyRecoveryController
            .onPageLoad()
            .url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application =
        applicationBuilder(
          userAnswers = None
        ).build()

      running(application) {
        val request =
          FakeRequest(
            POST,
            submitBankDetailsRoute
          ).withFormUrlEncodedBody(
            validFormData.toSeq*
          )

        val result =
          route(application, request).value

        status(result) mustEqual SEE_OTHER

        redirectLocation(result).value mustEqual
          routes.JourneyRecoveryController
            .onPageLoad()
            .url
      }
    }
  }
}
