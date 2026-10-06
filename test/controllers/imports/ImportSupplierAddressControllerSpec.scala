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
import config.FrontendAppConfig
import forms.imports.ImportSupplierAddressFormProvider
import models.{ImportSupplierAddress, NormalMode}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{times, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.RefundingCountryPage
import pages.imports.ImportSupplierAddressPage
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import navigation.{FakeNavigator, Navigator}
import repositories.SessionRepository
import views.html.imports.ImportSupplierAddressView

import scala.concurrent.Future

class ImportSupplierAddressControllerSpec extends SpecBase with MockitoSugar {

  private val onwardRoute = Call("GET", "/foo")

  private lazy val pageLoadRoute =
    routes.ImportSupplierAddressController.onPageLoad().url

  private lazy val submitRoute =
    routes.ImportSupplierAddressController.onSubmit().url

  // TODO:Temporary until ImportSuppliersNameController PR is merged
  private val backLink: Call =
    Call("GET", "/file-eu-vat/import/supplier-name")

  private val validFormData = Map(
    "addressLine1" -> "1 High Street",
    "addressLine2" -> "Apartment 3",
    "addressLine3" -> "London",
    "country"      -> "AF"
  )

  private val supplierAddress = ImportSupplierAddress(
    addressLine1 = "1 High Street",
    addressLine2 = Some("Apartment 3"),
    addressLine3 = Some("London"),
    country      = "AF"
  )

  "Import SupplierAddress Controller" - {

    "must return OK and the correct view for a GET" in {

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {

        val request =
          FakeRequest(GET, pageLoadRoute)

        val result =
          route(application, request).value

        val view =
          application.injector.instanceOf[ImportSupplierAddressView]

        val formProvider =
          application.injector.instanceOf[ImportSupplierAddressFormProvider]

        val config =
          application.injector.instanceOf[FrontendAppConfig]

        val form =
          formProvider()

        status(result) mustEqual OK

        normalizeHtml(contentAsString(result)) mustEqual normalizeHtml(
          view(
            form,
            config.supplierCountries,
            NormalMode,
            backLink
          )(request, messages(application)).toString
        )
      }
    }

    "must redirect to Journey Recovery when no existing data is found on GET" in {

      val application =
        applicationBuilder(userAnswers = None).build()

      running(application) {

        val request =
          FakeRequest(GET, pageLoadRoute)

        val result =
          route(application, request).value

        status(result) mustEqual SEE_OTHER

        redirectLocation(result).value mustEqual
          controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must populate the view with the saved value on GET" in {

      val userAnswers =
        emptyUserAnswers
          .set(ImportSupplierAddressPage, supplierAddress)
          .success
          .value

      val application =
        applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {

        val request =
          FakeRequest(GET, pageLoadRoute)

        val result =
          route(application, request).value

        val view =
          application.injector.instanceOf[ImportSupplierAddressView]

        val formProvider =
          application.injector.instanceOf[ImportSupplierAddressFormProvider]

        val config =
          application.injector.instanceOf[FrontendAppConfig]

        val form =
          formProvider().fill(supplierAddress)

        status(result) mustEqual OK

        normalizeHtml(contentAsString(result)) mustEqual normalizeHtml(
          view(
            form,
            config.supplierCountries,
            NormalMode,
            backLink
          )(request, messages(application)).toString
        )
      }
    }

    "must persist the answer and redirect to import currency when the refunding country is Estonia" in {

      val userAnswers =
        emptyUserAnswers
          .set(RefundingCountryPage, "EE")
          .success
          .value

      val mockSessionRepository =
        mock[SessionRepository]

      when(mockSessionRepository.set(any()))
        .thenReturn(Future.successful(true))

      val application =
        applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {

        val request =
          FakeRequest(POST, submitRoute)
            .withFormUrlEncodedBody(validFormData.toSeq*)

        val result =
          route(application, request).value

        status(result) mustEqual SEE_OTHER

        redirectLocation(result).value mustEqual
          controllers.imports.routes.ImportCurrencyController
            .onPageLoad(NormalMode)
            .url

        verify(mockSessionRepository, times(1))
          .set(any())
      }
    }

    "must persist the answer and redirect to total amount without VAT when the refunding country is not Estonia" in {

      val userAnswers =
        emptyUserAnswers
          .set(RefundingCountryPage, "FR")
          .success
          .value

      val mockSessionRepository =
        mock[SessionRepository]

      when(mockSessionRepository.set(any()))
        .thenReturn(Future.successful(true))

      val application =
        applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {

        val request =
          FakeRequest(POST, submitRoute)
            .withFormUrlEncodedBody(validFormData.toSeq*)

        val result =
          route(application, request).value

        status(result) mustEqual SEE_OTHER

//      TODO: Recheck once actual TotalAmountWithoutVatController route is available

        redirectLocation(result).value mustEqual
          controllers.imports.routes.TotalAmountWithoutVatController
            .onPageLoad()
            .url

        verify(mockSessionRepository, times(1))
          .set(any())
      }
    }

    "must redirect to Journey Recovery when the refunding country is not available" in {

      val mockSessionRepository =
        mock[SessionRepository]

      when(mockSessionRepository.set(any()))
        .thenReturn(Future.successful(true))

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {

        val request =
          FakeRequest(POST, submitRoute)
            .withFormUrlEncodedBody(validFormData.toSeq*)

        val result =
          route(application, request).value

        status(result) mustEqual SEE_OTHER

        redirectLocation(result).value mustEqual
          controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must persist the answer and redirect to the next page when valid data is submitted" in {

      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any()))
        .thenReturn(Future.successful(true))

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {

        val request =
          FakeRequest(POST, submitRoute)
            .withFormUrlEncodedBody(validFormData.toSeq*)

        val result =
          route(application, request).value

        status(result) mustEqual SEE_OTHER

        redirectLocation(result).value mustEqual onwardRoute.url

        verify(mockSessionRepository, times(1))
          .set(any())
      }
    }

    "must persist and redirect when only address line 1, country and optional lines omitted are submitted" in {

      val userAnswers =
        emptyUserAnswers
          .set(RefundingCountryPage, "EE")
          .success
          .value

      val mockSessionRepository =
        mock[SessionRepository]

      when(mockSessionRepository.set(any()))
        .thenReturn(Future.successful(true))

      val application =
        applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {

        val request =
          FakeRequest(POST, submitRoute)
            .withFormUrlEncodedBody(
              "addressLine1" -> "1 High Street",
              "country"      -> "AF"
            )

        val result =
          route(application, request).value

        status(result) mustEqual SEE_OTHER

        redirectLocation(result).value mustEqual
          controllers.imports.routes.ImportCurrencyController
            .onPageLoad(NormalMode)
            .url

        verify(mockSessionRepository, times(1))
          .set(any())
      }
    }

    "must return Bad Request and the required error when line1 is missing" in {

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {

        val request =
          FakeRequest(POST, submitRoute)
            .withFormUrlEncodedBody(
              "addressLine1" -> "",
              "country"      -> "AF"
            )

        val result =
          route(application, request).value

        status(result) mustEqual BAD_REQUEST

        contentAsString(result) must include(
          messages(application)("supplierAddress.error.line1.required")
        )
      }
    }

    "must return Bad Request and the max-length error when line1 is longer than 35 characters" in {

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {

        val tooLong =
          "a" * 36

        val request =
          FakeRequest(POST, submitRoute)
            .withFormUrlEncodedBody(
              "addressLine1" -> tooLong,
              "country"      -> "AF"
            )

        val result =
          route(application, request).value

        status(result) mustEqual BAD_REQUEST

        contentAsString(result) must include(
          messages(application)(
            "supplierAddress.error.line1.maxLength",
            messages(application)("supplierAddress.line1.label"),
            messages(application)("supplierAddress.error.maxLength")
          )
        )
      }
    }

    "must return Bad Request when country is missing" in {

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {

        val request =
          FakeRequest(POST, submitRoute)
            .withFormUrlEncodedBody(
              "addressLine1" -> "1 High Street",
              "country"      -> ""
            )

        val result =
          route(application, request).value

        status(result) mustEqual BAD_REQUEST

        contentAsString(result) must include(
          messages(application)("supplierAddress.error.country.required")
        )
      }
    }

    "must return Bad Request when country is not in the supplier country list" in {

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {

        val request =
          FakeRequest(POST, submitRoute)
            .withFormUrlEncodedBody(
              "addressLine1" -> "1 High Street",
              "country"      -> "ZZ"
            )

        val result =
          route(application, request).value

        status(result) mustEqual BAD_REQUEST

        contentAsString(result) must include(
          messages(application)("supplierAddress.error.country.invalid")
        )
      }
    }

    "must redirect to Journey Recovery when no existing data is found on POST" in {

      val application =
        applicationBuilder(userAnswers = None).build()

      running(application) {

        val request =
          FakeRequest(POST, submitRoute)
            .withFormUrlEncodedBody(validFormData.toSeq*)

        val result =
          route(application, request).value

        status(result) mustEqual SEE_OTHER

        redirectLocation(result).value mustEqual
          controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
