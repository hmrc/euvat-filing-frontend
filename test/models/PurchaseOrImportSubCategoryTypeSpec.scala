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

import base.SpecBase

class PurchaseOrImportSubCategoryTypeSpec extends SpecBase {

  "PurchaseOrImportSubCategoryType" - {
    "slugFor should return configured slug when present" in {
      PurchaseOrImportSubCategoryType.purchaseOrImportSubCategoryUrlSlugFor("fuel", "1.1") mustBe Some("fuel-type")
    }

    "pathFor should return configured slug when present" in {
      PurchaseOrImportSubCategoryType.pathFor("fuel", "1.1") mustBe "fuel-type"
    }

    "pathFor should fall back to parentKey-parentCode when no mapping" in {
      PurchaseOrImportSubCategoryType.pathFor("unknownKey", "2.4") mustBe "unknownKey-2-4"
    }

    "subCategoryPageSlugFor" - {
      "must return the slug for an exact parent code" in {
        PurchaseOrImportSubCategoryType.subCategoryPageSlugFor("foodAndDrink", "7.1") mustBe Some("who-food-drink-for")
      }

      "must walk up a nested sub category code to its parent slug" in {
        PurchaseOrImportSubCategoryType.subCategoryPageSlugFor("fuel", "1.1.2") mustBe Some("fuel-type")
        PurchaseOrImportSubCategoryType.subCategoryPageSlugFor("fuel", "1.2.6") mustBe Some("fuel-type-or-vehicle")
        PurchaseOrImportSubCategoryType.subCategoryPageSlugFor("fuel", "1.10.1") mustBe Some("fuel-type")
      }

      "must use the first child slug for a top-level code" in {
        PurchaseOrImportSubCategoryType.subCategoryPageSlugFor("fuel", "1") mustBe Some("fuel-type")
      }

      "must return None when there is no sub category page for the code" in {
        PurchaseOrImportSubCategoryType.subCategoryPageSlugFor("fuel", "1.5") mustBe None
        PurchaseOrImportSubCategoryType.subCategoryPageSlugFor("unknownKey", "2.4") mustBe None
      }
    }
  }
}
