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

package forms.imports

import base.SpecBase
import config.FrontendAppConfig
import models.ImportSupplierAddress
import org.scalatestplus.mockito.MockitoSugar
import play.api.i18n.MessagesApi

class ImportSupplierAddressFormProviderSpec extends SpecBase with MockitoSugar {

  private val messagesApi =
    applicationBuilder().build().injector.instanceOf[MessagesApi]

  private val config =
    applicationBuilder().build().injector.instanceOf[FrontendAppConfig]

  private val form =
    new ImportSupplierAddressFormProvider(messagesApi, config)()

  private val validData = Map(
    "addressLine1" -> "1 High Street",
    "addressLine2" -> "Apartment 3",
    "addressLine3" -> "London",
    "country"      -> "AF"
  )

  "SupplierAddressFormProvider" - {

    "must bind valid data" in {

      val result = form.bind(validData)

      result.value mustEqual Some(
        ImportSupplierAddress(
          addressLine1 = "1 High Street",
          addressLine2 = Some("Apartment 3"),
          addressLine3 = Some("London"),
          country      = "AF"
        )
      )
    }

    "must bind when optional address lines are missing" in {

      val data = Map(
        "addressLine1" -> "1 High Street",
        "country"      -> "AF"
      )

      val result = form.bind(data)

      result.value mustEqual Some(
        ImportSupplierAddress(
          addressLine1 = "1 High Street",
          addressLine2 = None,
          addressLine3 = None,
          country      = "AF"
        )
      )
    }

    "must fail when address line 1 is empty" in {

      val data =
        validData.updated("addressLine1", "")

      val result =
        form.bind(data)

      result.errors("addressLine1").head.message mustEqual
        "supplierAddress.error.line1.required"
    }

    "must accept address line 1 with exactly 35 characters" in {

      val data =
        validData.updated("addressLine1", "a" * 35)

      form.bind(data).hasErrors mustEqual false
    }

    "must fail when address line 1 is longer than 35 characters" in {

      val data =
        validData.updated("addressLine1", "a" * 36)

      val result =
        form.bind(data)

      result.errors("addressLine1").head.message mustEqual
        "supplierAddress.error.line1.maxLength"
    }

    "must fail when address line 2 is longer than 35 characters" in {

      val data =
        validData.updated("addressLine2", "a" * 36)

      val result =
        form.bind(data)

      result.errors("addressLine2").head.message mustEqual
        "supplierAddress.error.line2.maxLength"
    }

    "must fail when address line 3 is longer than 35 characters" in {

      val data =
        validData.updated("addressLine3", "a" * 36)

      val result =
        form.bind(data)

      result.errors("addressLine3").head.message mustEqual
        "supplierAddress.error.line3.maxLength"
    }

    "must fail when country is empty" in {

      val data =
        validData.updated("country", "")

      val result =
        form.bind(data)

      result.errors("country").head.message mustEqual
        "supplierAddress.error.country.required"
    }

    "must fail when country is not in supplier-countryCode" in {

      val data =
        validData.updated("country", "ZZ")

      val result =
        form.bind(data)

      result.errors("country").head.message mustEqual
        "supplierAddress.error.country.invalid"
    }

    "must store the country code rather than the country name" in {

      val result =
        form.bind(validData)

      result.value.value.country mustEqual "AF"
    }
  }
}
