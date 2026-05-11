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

package services

import javax.inject.Inject
import play.api.Configuration

class ConfigLanguageMapping @Inject() (config: Configuration) {

  private val mapping: Map[String, Seq[String]] = {
    val cfg = config.get[Configuration]("language.mapping")
    cfg.entrySet.map { case (key, sub) =>
      // play Configuration represents lists as ConfigValue; read as Seq[String]
      val seq = cfg.get[Seq[String]](key)
      key -> seq
    }.toMap
  }

  def languagesFor(code: String): Seq[String] = mapping.getOrElse(code, Seq("english")).map(_.capitalize)
}
