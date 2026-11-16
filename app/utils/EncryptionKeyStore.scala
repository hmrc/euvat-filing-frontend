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
import java.nio.file.{Path, Paths}
import java.security.KeyStore
import javax.crypto.SecretKey
import javax.inject.Inject
import scala.util.Using

class EncryptionKeyStore @Inject() (config: Configuration) {
  private val keyStoreLocation = config.get[String]("encryption.keystore.keyStoreLocation")
  private val keyStoreFile = config.get[String]("encryption.keystore.keyStore")
  private val keyStoreType = config.get[String]("encryption.keystore.type")
  private val keyAlias = config.get[String]("encryption.keystore.alias")
  private val keyEntryPwd = config.get[String]("encryption.keystore.keyEntryPwd").toCharArray
  private val keyStorePwd = config.get[String]("encryption.keystore.keyStorePwd").toCharArray
  private val keyStorePath: Path = Paths.get(keyStoreLocation, keyStoreFile)

  lazy val secretKey: SecretKey = {
    val keyStore = KeyStore.getInstance(keyStoreType)

    Using.resource(new FileInputStream(keyStorePath.toFile)) { inputStream =>
      keyStore.load(inputStream, keyStorePwd)
    }

    keyStore.getKey(keyAlias, keyEntryPwd) match {
      case key: SecretKey => key
      case _              => throw new Exception(s"Secret key '$keyAlias' does not exist")
    }
  }

}
