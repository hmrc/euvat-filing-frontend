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
import controllers.imports.routes as importRoutes
import controllers.warning.routes
import models.{CheckMode, NormalMode}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import views.html.warning.ImportDetailsWarningView

class ImportDetailsWarningControllerSpec extends SpecBase {

  "ImportDetailsWarningController Controller" - {

    "must return OK and the correct view for a GET in NormalMode" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
      running(application) {
        val request = FakeRequest(GET, routes.ImportDetailsWarningController.onPageLoad(NormalMode).url)
        val result = route(application, request).value
        val view = application.injector.instanceOf[ImportDetailsWarningView]
        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          importRoutes.SadReferenceNumberController.onPageLoad(NormalMode),
          importRoutes.ImportDetailsInfoController.onPageLoad(NormalMode), NormalMode)
          (request,messages(application)).toString
      }
    }

    "must return OK and the correct view for a GET in CheckMode" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
      running(application) {
        val request = FakeRequest(GET, routes.ImportDetailsWarningController.onPageLoad(CheckMode).url)
        val result = route(application, request).value
        val view = application.injector.instanceOf[ImportDetailsWarningView]
        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          importRoutes.SadReferenceNumberController.onPageLoad(CheckMode),
          importRoutes.ImportDetailsInfoController.onPageLoad(CheckMode), CheckMode)
          (request,messages(application)).toString
      }
    }

    "must redirect to the next page on submit in NormalMode" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
      running(application) {
        val request = FakeRequest(POST, routes.ImportDetailsWarningController.onSubmit(NormalMode).url)
        val result = route(application, request).value
        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual importRoutes.ImportDateController.onPageLoad(NormalMode).url
      }
    }

    "must redirect to next page on submit in CheckMode" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
      running(application) {
        val request = FakeRequest(POST, routes.ImportDetailsWarningController.onSubmit(CheckMode).url)
        val result = route(application, request).value
        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual importRoutes.ImportDateController.onPageLoad(CheckMode).url
      }
    }
  }
}