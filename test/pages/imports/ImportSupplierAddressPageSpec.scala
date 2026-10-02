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

package pages.imports

import base.SpecBase
import models.ImportSupplierAddress

class ImportSupplierAddressPageSpec extends SpecBase {

  "SupplierAddressPage" - {

    "must write and read ImportSupplierAddress from UserAnswers" in {

      val address = ImportSupplierAddress(
        addressLine1 = "1 High Street",
        addressLine2 = Some("Apartment 3"),
        addressLine3 = Some("London"),
        country      = "AF"
      )

      val userAnswers =
        emptyUserAnswers
          .set(ImportSupplierAddressPage, address)
          .success
          .value

      userAnswers.get(ImportSupplierAddressPage).value mustEqual address
    }

    "must remove ImportSupplierAddress from UserAnswers" in {

      val address = ImportSupplierAddress(
        addressLine1 = "1 High Street",
        addressLine2 = Some("Apartment 3"),
        addressLine3 = Some("London"),
        country      = "AF"
      )

      val userAnswers =
        emptyUserAnswers
          .set(ImportSupplierAddressPage, address)
          .success
          .value
          .remove(ImportSupplierAddressPage)
          .success
          .value

      userAnswers.get(ImportSupplierAddressPage) mustBe None
    }
  }
}
