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

package controllers.imports

import play.api.i18n.I18nSupport
import play.api.mvc.*
import views.html.imports.TotalAmountWithoutVatView

import javax.inject.Inject

// TODO: Over ride code when actual development is done in DTR-8186.

class TotalAmountWithoutVatController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  view: TotalAmountWithoutVatView
) extends BaseController
    with I18nSupport {

  def onPageLoad(): Action[AnyContent] =
    Action { implicit request =>
      Ok(view())
    }
}
