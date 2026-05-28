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

package models

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.Json

class SupplierAddressSpec extends AnyFreeSpec with Matchers {

  "SupplierAddress" - {

    "must serialise a fully-populated value to JSON" in {
      val address = SupplierAddress(
        line1 = "1 High Street",
        line2 = Some("Apartment 3"),
        line3 = Some("London")
      )
      Json.toJson(address) mustEqual Json.obj(
        "line1" -> "1 High Street",
        "line2" -> "Apartment 3",
        "line3" -> "London"
      )
    }

    "must serialise with only the mandatory line1 populated" in {
      val address = SupplierAddress(line1 = "1 High Street", line2 = None, line3 = None)
      Json.toJson(address) mustEqual Json.obj("line1" -> "1 High Street")
    }

    "must deserialise JSON with all three lines" in {
      val json = Json.obj(
        "line1" -> "1 High Street",
        "line2" -> "Apartment 3",
        "line3" -> "London"
      )
      json.as[SupplierAddress] mustEqual SupplierAddress(
        line1 = "1 High Street",
        line2 = Some("Apartment 3"),
        line3 = Some("London")
      )
    }

    "must deserialise JSON with only line1" in {
      val json = Json.obj("line1" -> "1 High Street")
      json.as[SupplierAddress] mustEqual SupplierAddress(
        line1 = "1 High Street",
        line2 = None,
        line3 = None
      )
    }

    "must round-trip through JSON" in {
      val original = SupplierAddress(
        line1 = "1 High Street",
        line2 = Some("Apartment 3"),
        line3 = None
      )
      Json.toJson(original).as[SupplierAddress] mustEqual original
    }
  }
}
