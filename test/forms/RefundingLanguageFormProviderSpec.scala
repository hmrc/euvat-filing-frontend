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

package forms

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.data.FormError

class RefundingLanguageFormProviderSpec extends AnyWordSpec with Matchers {

  "RefundingLanguageFormProvider" should {
    val form = new RefundingLanguageFormProvider()()

    "require a value" in {
      val result = form.bind(Map("value" -> "")).errors
      result should contain(FormError("value", "refundingLanguage.error.required"))
    }
  }
}
