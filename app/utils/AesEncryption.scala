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

import utils.EncryptionKeyStore

import java.nio.charset.StandardCharsets.UTF_8
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.inject.{Inject, Singleton}

@Singleton
class AesEncryption @Inject()(keyStoreService: EncryptionKeyStore) {
  private val Transformation = "AES/CBC/PKCS5Padding"
  private val IvLength = 16
  private val secureRandom = new SecureRandom()

  def encrypt(value: String): String = {
    val iv = new Array[Byte](IvLength)
    secureRandom.nextBytes(iv)

    val cipher = Cipher.getInstance(Transformation)
    cipher.init(Cipher.ENCRYPT_MODE, keyStoreService.secretKey, new IvParameterSpec(iv))
    val encrypted = cipher.doFinal(value.getBytes(UTF_8))

    Base64.getEncoder.encodeToString(iv ++ encrypted)
  }

  def decrypt(value: String): String = {
    val decoded = Base64.getDecoder.decode(value)
    val (iv, encrypted) = decoded.splitAt(IvLength)
    val cipher = Cipher.getInstance(Transformation)
    cipher.init(Cipher.DECRYPT_MODE, keyStoreService.secretKey, new IvParameterSpec(iv))
    new String(cipher.doFinal(encrypted), UTF_8)
  }

  def knownCipherText: String = encrypt("knowntext")

}
