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

import play.api.Logging

import java.nio.charset.StandardCharsets.UTF_8
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.inject.{Inject, Singleton}

@Singleton
class AesEncryption @Inject() (keyStoreService: EncryptionKeyStore) extends Logging {
  private val transformation = "AES/CBC/PKCS5Padding"

  def encrypt(value: String): String = {
    val cipher = Cipher.getInstance(transformation)
    val iv = new Array[Byte](cipher.getBlockSize)

    logger.info(s"******* keyStoreService.secretKey: ${keyStoreService.secretKey}")
    cipher.init(Cipher.ENCRYPT_MODE, keyStoreService.secretKey, new IvParameterSpec(iv))
    val encrypted = cipher.doFinal(value.getBytes(UTF_8))

    Base64.getEncoder.encodeToString(iv ++ encrypted)
  }

  def decrypt(encryptedValue: String): String = {

    val cipher = Cipher.getInstance(transformation)
    val decoded = Base64.getDecoder.decode(encryptedValue)
    var (iv, encrypted) = decoded.splitAt(cipher.getBlockSize) // works for to-be encrypted data
    if (decoded.length == cipher.getBlockSize) {
      iv = new Array[Byte](cipher.getBlockSize) // works for as-is encrypted data
    }

    logger.info(s"******* keyStoreService.secretKey: ${keyStoreService.secretKey}")

    cipher.init(Cipher.DECRYPT_MODE, keyStoreService.secretKey, new IvParameterSpec(iv))
    if (decoded.length == cipher.getBlockSize) {
      new String(cipher.doFinal(decoded), UTF_8)
    } else {
      new String(cipher.doFinal(encrypted), UTF_8)
    }
  }

  def knownCipherText: String = encrypt(keyStoreService.knownCipherText)

}
