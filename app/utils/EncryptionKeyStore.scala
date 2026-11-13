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

import play.api.Configuration

import java.io.FileInputStream
import java.security.KeyStore
import javax.crypto.SecretKey
import javax.inject.{Inject, Singleton}

@Singleton
class EncryptionKeyStore @Inject()(config: Configuration) {
  private val keyStoreLocation = config.get[String]("encryption.keystore.keyStoreLocation")
  private val keyStoreType = config.get[String]("encryption.keystore.type")
  private val alias = config.get[String]("encryption.keystore.alias")
  private val pwdKeyEntry = config.get[String]("encryption.keystore.pwdkeyEntry")
  private val pwdKeyStore = config.get[String]("encryption.keystore.pwdkeyStore")

  lazy val secretKey: SecretKey = {
    val keyStore = KeyStore.getInstance(keyStoreType)
    val inputStream = new FileInputStream(keyStoreLocation)

    try {
      keyStore.load(inputStream, pwdKeyEntry.toCharArray)
    } finally {
      inputStream.close()
    }

    keyStore.getKey(alias, pwdKeyStore.toCharArray).asInstanceOf[SecretKey]
  }
}
