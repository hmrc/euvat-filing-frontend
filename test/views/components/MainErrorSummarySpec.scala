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

package views.components

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

import base.SpecBase
import org.jsoup.Jsoup
import org.scalatest.matchers.must.Matchers
import play.api.data.Forms.*
import play.api.data.{Form, Forms}
import play.api.i18n.Messages
import play.api.test.FakeRequest
import uk.gov.hmrc.govukfrontend.views.html.components.GovukErrorSummary
import views.html.components.MainErrorSummary

class MainErrorSummarySpec extends SpecBase with Matchers {

  "MainErrorSummary" - {
    "must render the error summary when the form has errors" in new Setup {
      val formWithError = form.bind(Map("value" -> ""))
      val html = mainErrorSummary(formWithError)
      val doc = Jsoup.parse(html.body)
      doc.select(".govuk-error-summary").size mustBe 1
      doc.select(".govuk-error-summary__title").text mustBe messages("error.summary.title")
    }

    "must not render the error summary when the form has no errors" in new Setup {
      val formNoError = form.bind(Map("value" -> "something"))
      val html = mainErrorSummary(formNoError)
      val doc = Jsoup.parse(html.body)
      doc.select(".govuk-error-summary").size mustBe 0
    }

    "must render the error summary with error message arguments" in new Setup {
      val formWithError = form.withError(
        key     = "fieldName",
        message = "error.with.args",
        args    = Seq("arg1", "arg2")
      )
      val html = mainErrorSummary(formWithError)
      val doc = Jsoup.parse(html.body)
      doc.select(".govuk-error-summary").size mustBe 1
      doc.select(".govuk-error-summary__title").text mustBe messages("error.summary.title")
      doc.select(".govuk-error-summary__list a").text mustBe messages("error.with.args", "arg1", "arg2")
    }

    "must render the error summary with no href when error key is empty" in new Setup {
      val formWithError = form.withError(
        key     = "",
        message = "error.without.key"
      )
      val html = mainErrorSummary(formWithError)
      val doc = Jsoup.parse(html.body)
      doc.select(".govuk-error-summary").size mustBe 1
      doc.select(".govuk-error-summary__title").text mustBe messages("error.summary.title")
      doc.select(".govuk-error-summary__list a").isEmpty mustBe true
      doc.select(".govuk-error-summary__list li").text mustBe messages("error.without.key")
    }

    "must render every error when the form has multiple errors, preserving order" in new Setup {
      val formWithErrors = form
        .withError(key = "fieldA", message = "error.a")
        .withError(key = "fieldB", message = "error.b")
        .withError(key = "fieldC", message = "error.c")

      val html = mainErrorSummary(formWithErrors)
      val doc  = Jsoup.parse(html.body)

      val items = doc.select(".govuk-error-summary__list li")
      items.size mustBe 3
      items.get(0).select("a").attr("href") mustBe "#fieldA"
      items.get(0).select("a").text mustBe messages("error.a")
      items.get(1).select("a").attr("href") mustBe "#fieldB"
      items.get(1).select("a").text mustBe messages("error.b")
      items.get(2).select("a").attr("href") mustBe "#fieldC"
      items.get(2).select("a").text mustBe messages("error.c")
    }

    "must apply errorLinkOverrides to anchor hrefs across all errors" in new Setup {
      val formWithErrors = form
        .withError(key = "fieldA", message = "error.a")
        .withError(key = "fieldB", message = "error.b")

      val html = mainErrorSummary(formWithErrors, Map("fieldA" -> "fieldA-input", "fieldB" -> "fieldB-input"))
      val doc  = Jsoup.parse(html.body)

      val items = doc.select(".govuk-error-summary__list li a")
      items.size mustBe 2
      items.get(0).attr("href") mustBe "#fieldA-input"
      items.get(1).attr("href") mustBe "#fieldB-input"
    }

    "must mix anchor and plain text entries when some errors have an empty key" in new Setup {
      val formWithErrors = form
        .withError(key = "fieldA", message = "error.a")
        .withError(key = "",       message = "error.cross.field")
        .withError(key = "fieldB", message = "error.b")

      val html = mainErrorSummary(formWithErrors)
      val doc  = Jsoup.parse(html.body)

      val items = doc.select(".govuk-error-summary__list li")
      items.size mustBe 3
      items.get(0).select("a").attr("href") mustBe "#fieldA"
      items.get(1).select("a").isEmpty mustBe true
      items.get(1).text mustBe messages("error.cross.field")
      items.get(2).select("a").attr("href") mustBe "#fieldB"
    }
  }

  trait Setup {
    val app = applicationBuilder().build()
    val govukErrorSummary = app.injector.instanceOf[GovukErrorSummary]
    val mainErrorSummary = new MainErrorSummary(govukErrorSummary)
    val form: Form[String] = Form(
      "value" -> nonEmptyText
    )
    implicit val request: play.api.mvc.Request[?] = FakeRequest()
    implicit val messages: Messages = play.api.i18n.MessagesImpl(
      play.api.i18n.Lang.defaultLang,
      app.injector.instanceOf[play.api.i18n.MessagesApi]
    )
  }
}
