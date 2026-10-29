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

package models.responses

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.Json

class UpdateImportDetailsResponseSpec extends AnyFreeSpec with Matchers {

  "UpdateImportResponse JSON" - {

    "must read the update-importation-details response" in {
      Json.obj("updateSequenceNumber" -> 3).as[UpdateImportResponse] mustEqual UpdateImportResponse(3)
    }

    "must write the updateSequenceNumber" in {
      Json.toJson(UpdateImportResponse(3)) mustEqual Json.obj("updateSequenceNumber" -> 3)
    }

    "must fail to read when updateSequenceNumber is missing" in {
      Json.obj().validate[UpdateImportResponse].isError mustBe true
    }
  }
}
