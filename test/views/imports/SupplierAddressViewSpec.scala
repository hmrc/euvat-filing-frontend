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

package views.imports

import base.SpecBase
import config.FrontendAppConfig
import forms.imports.SupplierAddressFormProvider
import models.NormalMode
import play.api.mvc.Call
import play.api.test.FakeRequest
import views.html.imports.SupplierAddressView

class SupplierAddressViewSpec extends SpecBase {

  private val request = FakeRequest()

  private lazy val application = applicationBuilder().build()

  private lazy val formProvider =
    application.injector.instanceOf[SupplierAddressFormProvider]

  private lazy val config =
    application.injector.instanceOf[FrontendAppConfig]

  private lazy val view =
    application.injector.instanceOf[SupplierAddressView]

  private val backLink =
    Call("GET", "/import/supplier-name")

  "SupplierAddressView" - {

    "must display the correct page content" in {

      val html =
        view(
          formProvider(),
          config.supplierCountries,
          NormalMode,
          backLink
        )(request, messages(application)).toString

      html must include(messages(application)("import.caption"))
      html must include(messages(application)("supplierAddress.heading"))
      html must include(messages(application)("supplierAddress.lead"))
      html must include(messages(application)("supplierAddress.line1.label"))
      html must include(messages(application)("supplierAddress.line2.label"))
      html must include(messages(application)("supplierAddress.line3.label"))
      html must include(messages(application)("supplierAddress.country.label"))
      html must include(messages(application)("site.continue"))
    }

    "must use the correct back link" in {

      val html =
        view(
          formProvider(),
          config.supplierCountries,
          NormalMode,
          backLink
        )(request, messages(application)).toString

      html must include("""href="/import/supplier-name"""")
    }

    "must display supplier countries" in {

      val html =
        view(
          formProvider(),
          config.supplierCountries,
          NormalMode,
          backLink
        )(request, messages(application)).toString

      val (countryCode, countryName) =
        config.supplierCountries.head

      html must include(countryName)
      html must include(s"""value="$countryCode"""")
    }

    "must display the country field as an accessible autocomplete" in {

      val html =
        view(
          formProvider(),
          config.supplierCountries,
          NormalMode,
          backLink
        )(request, messages(application)).toString

      html must include("""data-module="hmrc-accessible-autocomplete"""")
    }

    "must display errors when the form has errors" in {

      val form =
        formProvider().bind(
          Map(
            "addressLine1" -> "",
            "country"      -> ""
          )
        )

      val html =
        view(
          form,
          config.supplierCountries,
          NormalMode,
          backLink
        )(request, messages(application)).toString

      html must include(
        messages(application)("supplierAddress.error.line1.required")
      )

      html must include(
        messages(application)("supplierAddress.error.country.required")
      )
    }
  }
}
