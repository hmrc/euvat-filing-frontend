package controllers

import base.SpecBase
import play.api.test.FakeRequest
import play.api.test.Helpers._
import views.html.AboutThePurchaseView

class AboutThePurchaseControllerSpec extends SpecBase {

  "AboutThePurchase Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, routes.AboutThePurchaseController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[AboutThePurchaseView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view()(request, messages(application)).toString
      }
    }
  }
}
