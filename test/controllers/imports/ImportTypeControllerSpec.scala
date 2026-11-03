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
import models.PurchaseOrImport.{Import, Purchase}
import models.{CheckMode, Fuel, NormalMode, PurchaseOrImportType, Transport, UserAnswers}
import models.responses.{AddImportResponse, ApplicationResponse, ImportDetailsResponse}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{never, times, verify, verifyNoMoreInteractions, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.PurchaseOrImportPage
import pages.imports.{AddImportResponsePage, ImportSubCodePage, ImportSuppliersNamePage, ImportTotalVatClaimPage, ImportTypePage, SadReferenceCheckPage, TotalAmountWithoutVatPage}
import play.api.data.Form
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.{ClaimApplicationResponseQuery, ImportSubTypeLabelQuery}
import repositories.SessionRepository
import uk.gov.hmrc.http.HeaderCarrier
import views.html.purchasesOrImports.PurchaseOrImportTypeView

import scala.concurrent.Future

class ImportTypeControllerSpec extends SpecBase with MockitoSugar {
  def onwardRoute: Call = Call("GET", "/foo")
  lazy val importTypeRoute: String = controllers.imports.routes.ImportTypeController.onPageLoad(NormalMode).url
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
          view(form, NormalMode, submitCall, "importType", "import.caption", showPurchaseIntro = false)(
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
          view(form.fill(Fuel), NormalMode, submitCall, "importType", "import.caption", showPurchaseIntro = false)(
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

    "must call addImport and persist the response when ClaimApplicationResponseQuery is present and no AddImportResponsePage exists" in {
      val mockSessionRepository = mock[SessionRepository]
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val addImportResponse = AddImportResponse(itemNumber = 7, updateSequenceNumber = 2)
      when(mockEuVatRefundsService.addImport(any())(any())) thenReturn Future.successful(addImportResponse)

      val userAnswers = emptyUserAnswers
        .set(ClaimApplicationResponseQuery, ApplicationResponse(134, "GB123134", 1))
        .success
        .value

      val application =
        applicationBuilder(userAnswers = Some(userAnswers))
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

        val captor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])
        verify(mockSessionRepository, times(2)).set(captor.capture())
        captor.getAllValues.get(1).get(AddImportResponsePage).value mustEqual addImportResponse
      }
    }

    "must not call addImport when AddImportResponsePage is already populated" in {
      val mockSessionRepository = mock[SessionRepository]
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val userAnswers = emptyUserAnswers
        .set(ClaimApplicationResponseQuery, ApplicationResponse(134, "GB123134", 1))
        .success
        .value
        .set(AddImportResponsePage, AddImportResponse(itemNumber = 1, updateSequenceNumber = 1))
        .success
        .value

      val application =
        applicationBuilder(userAnswers = Some(userAnswers))
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
        verify(mockEuVatRefundsService, org.mockito.Mockito.never()).addImport(any())(any())
      }
    }

    "must redirect to Journey Recovery when addImport fails" in {
      val mockSessionRepository = mock[SessionRepository]
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
      when(mockEuVatRefundsService.addImport(any())(any())) thenReturn Future.failed(new RuntimeException("boom"))

      val userAnswers = emptyUserAnswers
        .set(ClaimApplicationResponseQuery, ApplicationResponse(134, "GB123134", 1))
        .success
        .value

      val application =
        applicationBuilder(userAnswers = Some(userAnswers))
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
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
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
          view(boundForm, NormalMode, submitCall, "importType", "import.caption", showPurchaseIntro = false)(
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

    "must keep the sub-code when the import type is unchanged in CheckMode" in {
      val mockSessionRepository = mock[SessionRepository]
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
      val userAnswers = emptyUserAnswers
        .set(ImportTypePage, Fuel)
        .success
        .value
        .set(ImportSubCodePage, "1.2")
        .success
        .value

      val application =
        applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, controllers.imports.routes.ImportTypeController.onSubmit(CheckMode).url)
            .withFormUrlEncodedBody(("value", Fuel.toString))

        val result = route(application, request).value
        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url

        val captor = ArgumentCaptor.forClass(classOf[UserAnswers])
        verify(mockSessionRepository, times(1)).set(captor.capture())
        captor.getValue.get(ImportTypePage) mustBe Some(Fuel)
        captor.getValue.get(ImportSubCodePage) mustBe Some("1.2")
      }
    }

    "must clear the sub-code when the import type changes in CheckMode" in {
      val mockSessionRepository = mock[SessionRepository]
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
      val userAnswers = emptyUserAnswers
        .set(ImportTypePage, Fuel)
        .success
        .value
        .set(ImportSubCodePage, "1.2")
        .success
        .value

      val application =
        applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, controllers.imports.routes.ImportTypeController.onSubmit(CheckMode).url)
            .withFormUrlEncodedBody(("value", Transport.toString))

        val result = route(application, request).value
        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url

        val captor = ArgumentCaptor.forClass(classOf[UserAnswers])
        verify(mockSessionRepository, times(1)).set(captor.capture())
        captor.getValue.get(ImportTypePage) mustBe Some(Transport)
        captor.getValue.get(ImportSubCodePage) mustBe None
      }
    }

    "addDetails should fetch, map and persist purchase details" in {
      val savedCaptor: ArgumentCaptor[models.UserAnswers] = ArgumentCaptor.forClass(classOf[models.UserAnswers])
      when(mockSessionRepository.set(savedCaptor.capture())) thenReturn Future.successful(true)

      val importDetail = ImportDetailsResponse(
        applicationId               = 123L,
        itemNumber                  = 7,
        issuingDate                 = None,
        supplierName                = None,
        supplierAddressLine1        = None,
        supplierAddressLine2        = None,
        supplierAddressLine3        = None,
        supplierCountryCode         = None,
        goodsDescriptionCategory    = "1",
        goodsDescriptionSubcategory = None,
        goodsDescriptionText        = None,
        hasSadReferenceNumber       = None,
        sadReferenceNumber          = None,
        referenceInformation        = None,
        currencyCode                = None,
        taxableAmount               = None,
        vatAmount                   = None,
        deductibleVatAmount         = None,
        updateSequenceNumber        = 5
      )

      when(mockEuVatRefundsService.getImportDetails(org.mockito.ArgumentMatchers.eq(123L), org.mockito.ArgumentMatchers.eq(7))(any()))
        .thenReturn(Future.successful(importDetail))

      val ua = emptyUserAnswers
        .set(ClaimApplicationResponseQuery, ApplicationResponse(123, "App1", 5))
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(ua))
        .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
        .build()

      running(application) {
        val request = FakeRequest(GET, controllers.imports.routes.ImportTypeController.addDetails(123L, 7).url)
        val result = route(application, request).value
        val view = application.injector.instanceOf[PurchaseOrImportTypeView]

        status(result) mustEqual OK

        normalizeHtml(contentAsString(result)) mustEqual normalizeHtml(
          view(form.fill(Fuel), NormalMode, submitCall, "importType", "import.caption", showPurchaseIntro = false)(
            request,
            messages(application)
          ).toString
        )

        val saved = savedCaptor.getValue
        saved.get(PurchaseOrImportPage) mustBe Some(Import)
        saved.get(ImportTypePage) mustBe Some(Fuel)
        saved.get(AddImportResponsePage) mustBe Some(AddImportResponse(7, 5))
      }
    }

    "addDetails should clear and replace stale import details when fetch" in {
      val savedCaptor: ArgumentCaptor[models.UserAnswers] = ArgumentCaptor.forClass(classOf[models.UserAnswers])
      when(mockSessionRepository.set(savedCaptor.capture())) thenReturn Future.successful(true)

      val importDetail = ImportDetailsResponse(
        applicationId               = 123L,
        itemNumber                  = 7,
        issuingDate                 = None,
        supplierName                = None,
        supplierAddressLine1        = None,
        supplierAddressLine2        = None,
        supplierAddressLine3        = None,
        supplierCountryCode         = None,
        goodsDescriptionCategory    = "1",
        goodsDescriptionSubcategory = None,
        goodsDescriptionText        = None,
        hasSadReferenceNumber       = None,
        sadReferenceNumber          = None,
        referenceInformation        = None,
        currencyCode                = None,
        taxableAmount               = None,
        vatAmount                   = None,
        deductibleVatAmount         = None,
        updateSequenceNumber        = 5
      )

      when(mockEuVatRefundsService.getImportDetails(org.mockito.ArgumentMatchers.eq(123L), org.mockito.ArgumentMatchers.eq(7))(any()))
        .thenReturn(Future.successful(importDetail))

      val ua = emptyUserAnswers
        .set(ClaimApplicationResponseQuery, ApplicationResponse(123, "App1", 5))
        .success
        .value
        .set(PurchaseOrImportPage, Purchase)
        .success
        .value
        .set(ImportTypePage, Transport)
        .success
        .value
        .set(ImportSubCodePage, "3.2")
        .success
        .value
        .set(ImportSubTypeLabelQuery, "Transport cost")
        .success
        .value
        .set(ImportSuppliersNamePage, "Test Name")
        .success
        .value
        .set(SadReferenceCheckPage, false)
        .success
        .value
        .set(TotalAmountWithoutVatPage, 99)
        .success
        .value
        .set(ImportTotalVatClaimPage, 100)
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(ua))
        .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
        .build()

      running(application) {
        val request = FakeRequest(GET, controllers.imports.routes.ImportTypeController.addDetails(123L, 7).url)
        val result = route(application, request).value

        status(result) mustEqual OK

        val saved = savedCaptor.getValue

        saved.get(PurchaseOrImportPage) mustBe Some(Import)
        saved.get(ImportTypePage) mustBe Some(Fuel)

        saved.get(ImportSubCodePage) mustBe None
        saved.get(ImportSubTypeLabelQuery) mustBe None
        saved.get(ImportSuppliersNamePage) mustBe None
        saved.get(SadReferenceCheckPage) mustBe None
        saved.get(TotalAmountWithoutVatPage) mustBe None
        saved.get(ImportTotalVatClaimPage) mustBe None
      }
    }

    "addDetails should redirect to JourneyRecovery when fetching import details fails" in {
      when(mockEuVatRefundsService.getImportDetails(org.mockito.ArgumentMatchers.eq(123L), org.mockito.ArgumentMatchers.eq(7))(any()))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, controllers.imports.routes.ImportTypeController.addDetails(123L, 7).url)
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url

        verify(mockSessionRepository, never()).set(any[UserAnswers])
      }
    }

    "addDetails should redirect to JourneyRecovery when applicationId does not match the claim in session" in {
      val savedCaptor: ArgumentCaptor[models.UserAnswers] = ArgumentCaptor.forClass(classOf[models.UserAnswers])
      when(mockSessionRepository.set(savedCaptor.capture())) thenReturn Future.successful(true)

      val importDetail = ImportDetailsResponse(
        applicationId               = 123L,
        itemNumber                  = 7,
        issuingDate                 = None,
        supplierName                = None,
        supplierAddressLine1        = None,
        supplierAddressLine2        = None,
        supplierAddressLine3        = None,
        supplierCountryCode         = None,
        goodsDescriptionCategory    = "111",
        goodsDescriptionSubcategory = None,
        goodsDescriptionText        = None,
        hasSadReferenceNumber       = None,
        sadReferenceNumber          = None,
        referenceInformation        = None,
        currencyCode                = None,
        taxableAmount               = None,
        vatAmount                   = None,
        deductibleVatAmount         = None,
        updateSequenceNumber        = 5
      )

      when(mockEuVatRefundsService.getImportDetails(org.mockito.ArgumentMatchers.eq(123L), org.mockito.ArgumentMatchers.eq(7))(any()))
        .thenReturn(Future.successful(importDetail))

      val ua = emptyUserAnswers
        .set(ClaimApplicationResponseQuery, ApplicationResponse(555, "App1", 5))
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(ua))
        .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
        .build()

      running(application) {
        val request = FakeRequest(GET, controllers.imports.routes.ImportTypeController.addDetails(123L, 7).url)
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url

        verify(mockEuVatRefundsService, never()).getImportDetails(any, any)(any[HeaderCarrier])
        verify(mockSessionRepository, never()).set(any[UserAnswers])
        verifyNoMoreInteractions(mockEuVatRefundsService)
      }
    }

    "addDetails should redirect to JourneyRecovery when goodsDescriptionCategory does not match" in {
      val savedCaptor: ArgumentCaptor[models.UserAnswers] = ArgumentCaptor.forClass(classOf[models.UserAnswers])
      when(mockSessionRepository.set(savedCaptor.capture())) thenReturn Future.successful(true)

      val importDetail = ImportDetailsResponse(
        applicationId               = 123L,
        itemNumber                  = 7,
        issuingDate                 = None,
        supplierName                = None,
        supplierAddressLine1        = None,
        supplierAddressLine2        = None,
        supplierAddressLine3        = None,
        supplierCountryCode         = None,
        goodsDescriptionCategory    = "111",
        goodsDescriptionSubcategory = None,
        goodsDescriptionText        = None,
        hasSadReferenceNumber       = None,
        sadReferenceNumber          = None,
        referenceInformation        = None,
        currencyCode                = None,
        taxableAmount               = None,
        vatAmount                   = None,
        deductibleVatAmount         = None,
        updateSequenceNumber        = 5
      )

      when(mockEuVatRefundsService.getImportDetails(org.mockito.ArgumentMatchers.eq(123L), org.mockito.ArgumentMatchers.eq(7))(any()))
        .thenReturn(Future.successful(importDetail))

      val ua = emptyUserAnswers
        .set(ClaimApplicationResponseQuery, ApplicationResponse(123, "App1", 5))
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(ua))
        .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
        .build()

      running(application) {
        val request = FakeRequest(GET, controllers.imports.routes.ImportTypeController.addDetails(123L, 7).url)
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url

        verify(mockSessionRepository, never()).set(any[UserAnswers])
      }
    }

  }
}
