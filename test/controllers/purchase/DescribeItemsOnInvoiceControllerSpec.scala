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

package controllers.purchase

import base.SpecBase
import forms.DescribeItemsFormProvider
import models.*
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.mockito.{ArgumentCaptor, Mockito}
import org.scalatestplus.mockito.MockitoSugar
import pages.purchase
import pages.purchase.{DescribeItemsArrivedFromCheckYourAnswersPage, DescribeItemsOnInvoicePage}
import play.api.Application
import play.api.data.Form
import play.api.inject.bind
import play.api.mvc.{Call, Request}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import views.html.PurchaseOrImportDescribeItemsView

import scala.concurrent.Future

class DescribeItemsOnInvoiceControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute: Call = Call("GET", "/foo")

  lazy val describeItemsOnInvoiceRoute: String = routes.DescribeItemsOnInvoiceController.onPageLoad(NormalMode).url
  lazy val describeItemsOnInvoiceCheckModeRoute: String = routes.DescribeItemsOnInvoiceController.onPageLoad(CheckMode).url

  val formProvider = new DescribeItemsFormProvider()
  val form: Form[String] = formProvider("describeItemsOnInvoice")

  private def expectedView(application: Application, form: Form[String], mode: Mode, request: Request[?]): String = {
    val view = application.injector.instanceOf[PurchaseOrImportDescribeItemsView]
    view(
      form,
      routes.DescribeItemsOnInvoiceController.onSubmit(mode),
      routes.PurchaseSubTypeController.onPageLoad(PurchaseOrImportType.urlSlugForPurchaseType(Other), NormalMode),
      "describeItemsOnInvoice",
      "purchase.caption",
      Some(messages(application)("describeItemsOnInvoice.hint"))
    )(request, messages(application)).toString
  }

  "DescribeItemsOnInvoice Controller" - {

    "must return OK and the correct view for a GET" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, describeItemsOnInvoiceRoute)
        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual expectedView(application, form, NormalMode, request)
      }
    }

    "must show the purchase caption and hint" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val result = route(application, FakeRequest(GET, describeItemsOnInvoiceRoute)).value

        status(result) mustEqual OK
        val content = contentAsString(result)
        content must include(messages(application)("purchase.caption"))
        content must include(messages(application)("describeItemsOnInvoice.hint"))
      }
    }

    "must mark arrival and persist when opened in CheckMode and flag missing" in {
      val mockSessionRepository = mock[SessionRepository]
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
        .build()

      running(application) {
        val request = FakeRequest(GET, describeItemsOnInvoiceCheckModeRoute)
        val result = route(application, request).value

        status(result) mustEqual OK

        val captor = ArgumentCaptor.forClass(classOf[UserAnswers])
        Mockito.verify(mockSessionRepository, Mockito.times(1)).set(captor.capture())
        captor.getValue.get(DescribeItemsArrivedFromCheckYourAnswersPage).value mustBe true
      }
    }

    "must not persist when opened in CheckMode and flag already set" in {
      val mockSessionRepository = mock[SessionRepository]
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val userAnswers = emptyUserAnswers.set(purchase.DescribeItemsArrivedFromCheckYourAnswersPage, true).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
        .build()

      running(application) {
        val request = FakeRequest(GET, describeItemsOnInvoiceRoute)
        val result = route(application, request).value

        status(result) mustEqual OK
        Mockito.verify(mockSessionRepository, Mockito.times(0)).set(any())
      }
    }

    "must not persist arrival flag when opened in NormalMode" in {
      val mockSessionRepository = mock[SessionRepository]
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
        .build()

      running(application) {
        val request = FakeRequest(GET, describeItemsOnInvoiceRoute)
        val result = route(application, request).value

        status(result) mustEqual OK
        Mockito.verify(mockSessionRepository, Mockito.times(0)).set(any())
      }
    }

    "must short-circuit to purchase CYA in CheckMode when value unchanged" in {
      val mockSessionRepository = mock[SessionRepository]
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
      val userAnswers = emptyUserAnswers.set(DescribeItemsOnInvoicePage, "Fuel and transport costs").success.value

      val application =
        applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
          .build()

      running(application) {
        val controller = application.injector.instanceOf[DescribeItemsOnInvoiceController]
        val postRequest = FakeRequest(POST, "/").withFormUrlEncodedBody(("value", "Fuel and transport costs"))
        val result = controller.onSubmit(CheckMode).apply(postRequest)

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.CheckYourPurchaseDetailsController.onPageLoad().url
        Mockito.verify(mockSessionRepository, Mockito.times(1)).set(any())
      }
    }

    "must persist and redirect to next page in CheckMode when value changed" in {
      val mockSessionRepository = mock[SessionRepository]
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepository),
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute))
          )
          .build()

      running(application) {
        val controller = application.injector.instanceOf[DescribeItemsOnInvoiceController]
        val postRequest = FakeRequest(POST, "/").withFormUrlEncodedBody(("value", "Fuel and transport costs"))
        val result = controller.onSubmit(CheckMode).apply(postRequest)

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
        Mockito.verify(mockSessionRepository, Mockito.times(1)).set(any())
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {
      val userAnswers = UserAnswers(userAnswersId).set(DescribeItemsOnInvoicePage, "Fuel and transport costs").success.value
      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, describeItemsOnInvoiceRoute)
        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual expectedView(application, form.fill("Fuel and transport costs"), NormalMode, request)
      }
    }

    "must redirect to the next page when valid data is submitted" in {
      val mockSessionRepository = mock[SessionRepository]
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
        .build()

      running(application) {
        val request = FakeRequest(POST, describeItemsOnInvoiceRoute)
          .withFormUrlEncodedBody(("value", "Fuel and transport costs"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.InvoiceTypeController.onPageLoad(NormalMode).url
      }
    }

    "must return a Bad Request and errors when data exceeding the max length is submitted" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val tooLong = "a" * 256

        val request = FakeRequest(POST, describeItemsOnInvoiceRoute).withFormUrlEncodedBody(("value", tooLong))

        val boundForm = form.bind(Map("value" -> tooLong))
        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual expectedView(application, boundForm, NormalMode, request)
      }
    }

    "must redirect to PurchaseWarningController and persist an empty value when empty data is submitted in NormalMode" in {
      val mockSessionRepository = mock[SessionRepository]
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, describeItemsOnInvoiceRoute)
            .withFormUrlEncodedBody(("value", ""))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.warning.routes.PurchaseWarningController.onPageLoad(NormalMode).url

        val captor = ArgumentCaptor.forClass(classOf[UserAnswers])
        Mockito.verify(mockSessionRepository).set(captor.capture())
        captor.getValue.get(DescribeItemsOnInvoicePage).value mustEqual ""
      }
    }

    "must redirect to PurchaseWarningController and persist an empty value when empty data is submitted in CheckMode" in {
      val mockSessionRepository = mock[SessionRepository]
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
      val userAnswers = UserAnswers(userAnswersId).set(DescribeItemsOnInvoicePage, "Fuel and transport costs").success.value

      val application =
        applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, describeItemsOnInvoiceCheckModeRoute)
            .withFormUrlEncodedBody(("value", ""))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.warning.routes.PurchaseWarningController.onPageLoad(CheckMode).url

        val captor = ArgumentCaptor.forClass(classOf[UserAnswers])
        Mockito.verify(mockSessionRepository).set(captor.capture())
        captor.getValue.get(DescribeItemsOnInvoicePage).value mustEqual ""
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {
      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, describeItemsOnInvoiceRoute)
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "redirect to Journey Recovery for a POST if no existing data is found" in {
      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, describeItemsOnInvoiceRoute)
            .withFormUrlEncodedBody(("value", "Fuel and transport costs"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
