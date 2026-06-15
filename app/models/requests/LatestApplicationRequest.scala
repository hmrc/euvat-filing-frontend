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

package models.requests

import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json.{Format, __}

import java.time.LocalDateTime

case class LatestApplicationRequest(
                                     applicantVatRegNumber: String,
                                     refundingCountry: String,
                                     startDate: LocalDateTime,
                                     endDate: LocalDateTime,
                                     representativeId: String,
                                     maxNumber: Int,
                                     orderBy: Option[Int],
                                     sortOrder: Option[String],
                                     startAt: Option[Int]
                                   )

object LatestApplicationRequest:
  implicit val format: Format[LatestApplicationRequest] =
    (
      (__ \ "applicantVatRegNumber").format[String] and
        (__ \ "refundingCountry").format[String] and
        (__ \ "startDate").format[LocalDateTime] and
        (__ \ "endDate").format[LocalDateTime] and
        (__ \ "representativeId").format[String] and
        (__ \ "maxNumber").format[Int] and
        (__ \ "orderBy").formatNullable[Int] and
        (__ \ "sortOrder").formatNullable[String] and
        (__ \ "startAt").formatNullable[Int]
      )(LatestApplicationRequest.apply, o => Tuple.fromProductTyped(o))
