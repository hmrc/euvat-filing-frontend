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

package utils

import play.api.{Configuration, Logging}
import uk.gov.hmrc.crypto.{Crypted, PlainText, SymmetricCryptoFactory}

import javax.inject.{Inject, Singleton}
import scala.util.{Failure, Success, Try}

@Singleton
class BankDetailsEncryption @Inject() (configuration: Configuration) extends Logging {

  private val KnownText = "knowntext"

  private val crypto =
    SymmetricCryptoFactory.aesGcmCryptoFromConfig("bankDetails.encryption", configuration.underlying)

  def encrypt(plainText: String): String =
    crypto.encrypt(PlainText(plainText)).value

  def decrypt(cipherText: String): Option[String] =
    Try(crypto.decrypt(Crypted(cipherText)).value) match {
      case Success(value) => Some(value)
      case Failure(_) =>
        logger.warn("Failed to decrypt bank details value")
        None
    }

  def knownCipherText: String = encrypt(KnownText)
}
