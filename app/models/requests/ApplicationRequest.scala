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

import play.api.libs.json.{Json, OFormat}

import java.time.LocalDateTime

case class ApplicationRequest(
  refundingCountryCode: Option[String] = None,
  periodStartDate: Option[LocalDateTime] = None,
  periodEndDate: Option[LocalDateTime] = None,
  applicantEmailAddress: Option[String] = None,
  applicantTelephoneNumber: Option[String] = None,
  applicationLanguage: Option[String] = None,
  businessActivityCode1: Option[String] = None,
  businessActivityCode2: Option[String] = None,
  businessActivityCode3: Option[String] = None,
  representativeId: Option[String] = None,
  representativeCountryCode: Option[String] = None,
  representativeEmailAddress: Option[String] = None,
  representativeIdType: Option[String] = None,
  representativeTelephoneNumber: Option[String] = None,
  bankAccountOwnerName: Option[String] = None,
  bankAccountOwnerType: Option[String] = None,
  iBanCode: Option[String] = None,
  bicCode: Option[String] = None,
  bankAccountCurrencyCode: Option[String] = None
)

object ApplicationRequest {
  implicit val format: OFormat[ApplicationRequest] = Json.format[ApplicationRequest]
}
