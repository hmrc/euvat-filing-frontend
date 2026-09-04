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
import play.api.libs.json.{JsString, Json}

class ImportTypeSpec extends AnyFreeSpec with Matchers {

  "ImportType" - {

    "must hold the five category keys in order" in {
      ImportType.values.map(_.toString) mustEqual Seq("fuel", "transport", "foodAndDrink", "luxuries", "other")
    }

    "must resolve each value from its key" in {
      ImportType.values.foreach { value =>
        ImportType.fromKey(value.toString) mustBe Some(value)
      }
    }

    "must not resolve an unknown key" in {
      ImportType.fromKey("unknown") mustBe None
    }

    "must map every value to an import URL slug" in {
      ImportType.values.foreach { value =>
        ImportType.urlSlugForImportType.keySet must contain(value)
      }
      ImportType.urlSlugForImportType(ImportType.Fuel) mustEqual "import-fuel-use"
      ImportType.urlSlugForImportType(ImportType.Other) mustEqual "import-type-other"
    }

    "must serialise and deserialise via the enumerable format" in {
      ImportType.values.foreach { value =>
        Json.toJson[ImportType](value) mustEqual JsString(value.toString)
        JsString(value.toString).as[ImportType] mustEqual value
      }
    }

    "must fail to deserialise an invalid value" in {
      JsString("invalid").validate[ImportType].isError mustBe true
    }
  }
}
