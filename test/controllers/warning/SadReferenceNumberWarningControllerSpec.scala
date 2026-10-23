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

package controllers.warning

import base.SpecBase
import models.{CheckMode, NormalMode}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import views.html.warning.SadReferenceNumberWarningView

class SadReferenceNumberWarningControllerSpec extends SpecBase {

  "SadReferenceNumberWarning Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, routes.SadReferenceNumberWarningController.onPageLoad(NormalMode).url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[SadReferenceNumberWarningView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(controllers.imports.routes.SadReferenceNumberController.onPageLoad(NormalMode), NormalMode)(
          request,
          messages(application)
        ).toString
      }
    }

    "must return OK and the correct view for a GET in CheckMode" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, routes.SadReferenceNumberWarningController.onPageLoad(CheckMode).url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[SadReferenceNumberWarningView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(controllers.imports.routes.SadReferenceNumberController.onPageLoad(CheckMode), CheckMode)(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, routes.SadReferenceNumberWarningController.onPageLoad(NormalMode).url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to InvoiceTypeController on submit in NormalMode" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(POST, routes.SadReferenceNumberWarningController.onSubmit(NormalMode).url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.imports.routes.ImportDateController.onPageLoad(NormalMode).url
      }
    }

    "must redirect to import CYA on submit in CheckMode" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(POST, routes.SadReferenceNumberWarningController.onSubmit(CheckMode).url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        // TODO replace to RA5.10.1 - Check your answers (import details)
//        redirectLocation(result).value mustEqual controllers.imports.routes.CheckYourPurchaseDetailsController.onPageLoad().url
        redirectLocation(result).value mustEqual controllers.imports.routes.SadReferenceNumberController.onPageLoad(CheckMode).url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(POST, routes.SadReferenceNumberWarningController.onSubmit(NormalMode).url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
