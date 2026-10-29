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

import config.FrontendAppConfig

import java.io.FileInputStream
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.{Cipher, SecretKey}
import javax.inject.{Inject, Singleton}
import scala.util.Using

@Singleton
class BankDetailsEncryption @Inject() (appConfig: FrontendAppConfig) {

  private lazy val secretKey: SecretKey = {
    val keyStore = KeyStore.getInstance(appConfig.keyStoreType)
    val path = s"${appConfig.keyStoreLocation}/${appConfig.keyStoreFile}"

    Using.resource(new FileInputStream(path)) { stream =>
      keyStore.load(stream, appConfig.keyStorePassword.toCharArray)
    }

    keyStore.getKey(appConfig.keyAlias, appConfig.keyEntryPassword.toCharArray).asInstanceOf[SecretKey]
  }

  private val Transformation = "AES/ECB/PKCS5Padding"

  private def toHex(bytes: Array[Byte]): String =
    bytes.map(b => f"$b%02x").mkString

  private def fromHex(hex: String): Array[Byte] =
    hex.grouped(2).map(Integer.parseInt(_, 16).toByte).toArray

  def encrypt(plainText: String): String = {
    val cipher = Cipher.getInstance(Transformation)
    cipher.init(Cipher.ENCRYPT_MODE, secretKey)
    toHex(cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8)))
  }

  def decrypt(cipherText: String): String = {
    val cipher = Cipher.getInstance(Transformation)
    cipher.init(Cipher.DECRYPT_MODE, secretKey)
    new String(cipher.doFinal(fromHex(cipherText)), StandardCharsets.UTF_8)
  }

  def knownCipherText: String = encrypt(appConfig.knownText)
}
