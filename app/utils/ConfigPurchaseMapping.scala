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

import javax.inject.Inject
import play.api.Configuration
import uk.gov.hmrc.govukfrontend.views.viewmodels.radios.RadioItem

case class PurchaseNode(parent: String, code: String, label: String, children: Seq[PurchaseNode] = Seq.empty)

class ConfigPurchaseMapping @Inject() (config: Configuration = Configuration.empty) {

  private def normalizeLabel(label: String, code: String): String = {
    if (label == null) return ""
    val prefix = "purchase.sub."
    if (!label.startsWith(prefix)) return label
    if (code == null || code.isEmpty) return label
    val rest = label.substring(prefix.length)
    val parts = rest.split("\\.")
    val codeFirst = code.split("\\.").headOption.getOrElse("")
    // Only drop the numeric segment after the type if it matches the first numeric segment of the entry code
    if (parts.length >= 2 && parts(1) == codeFirst) {
      val newRest = parts.head +: parts.drop(2)
      prefix + newRest.mkString(".")
    } else label
  }


  private val mapping: Map[String, Seq[PurchaseNode]] = try {
    val rootConfig = config.underlying.getConfig("purchase.mapping")

    def parseEntry(entry: Any): PurchaseNode = entry match {
      case s: String =>
        val parts = s.split("\\|", 3)
        PurchaseNode(parts(0), parts(1), parts(2), Seq.empty)
      case c: Configuration =>
        val parent = c.getOptional[String]("parent").getOrElse("")
        val code = c.getOptional[String]("code").getOrElse("")
        val label = c.getOptional[String]("label").getOrElse("")
        // handle mixed arrays in subcodes (strings and objects)
        val children: Seq[PurchaseNode] = try {
          val underlying = c.underlying
          import scala.jdk.CollectionConverters.*
          val list = underlying.getList("subcodes")
          list.asScala.toSeq.map { v =>
            import com.typesafe.config.ConfigValueType
            v.valueType() match {
              case ConfigValueType.STRING => parseEntry(v.unwrapped().asInstanceOf[String])
              case ConfigValueType.OBJECT =>
                val obj = v.asInstanceOf[com.typesafe.config.ConfigObject].toConfig
                parseEntry(Configuration(obj))
              case _ => PurchaseNode("", "", "", Seq.empty)
            }
          }
        } catch {
          case _: Throwable => Seq.empty
        }
        PurchaseNode(parent, code, label, children)
      case _ => PurchaseNode("", "", "", Seq.empty)
    }

    import scala.jdk.CollectionConverters.*

    // iterate the top-level keys under purchase.mapping (country codes)
    val countries: Seq[String] = rootConfig.root().keySet().asScala.toSeq
    val playRoot = Configuration(rootConfig)

    countries.map { key =>
      // each key maps to either a Seq[String] or Seq[ConfigObject]
      val seqAny: Seq[Any] = try {
        // Prefer using the underlying Config list which handles mixed types
        val list = rootConfig.getList(key)
        list.asScala.toSeq.map { v =>
          import com.typesafe.config.ConfigValueType
          v.valueType() match {
            case ConfigValueType.STRING => v.unwrapped().asInstanceOf[String]
            case ConfigValueType.OBJECT =>
              // convert to a play Configuration for reuse
              val obj = v.asInstanceOf[com.typesafe.config.ConfigObject].toConfig
              Configuration(obj)
            case _ => v.unwrapped()
          }
        }
      } catch {
        case _: Throwable =>
          try {
            playRoot.get[Seq[String]](key).map(_.asInstanceOf[Any])
          } catch {
            case _: Throwable =>
              try {
                playRoot.get[Seq[Configuration]](key).map(_.asInstanceOf[Seq[Any]])
              } catch {
                case _: Throwable => Seq.empty
              }
          }
      }

      // parse into flat nodes first
      val flatNodes = seqAny.map(parseEntry)

      // helper to normalize label keys by stripping a redundant numeric segment
      // Example: purchase.sub.fuel.1.10.5 -> purchase.sub.fuel.10.5
      def normalizeLabelKey(label: String): String = {
        if (label == null) return ""
        val prefix = "purchase.sub."
        if (!label.startsWith(prefix)) return label
        val rest = label.substring(prefix.length)
        val parts = rest.split("\\.")
        if (parts.length >= 2 && parts(1).matches("\\d+")) {
          val newRest = parts.head +: parts.drop(2)
          prefix + newRest.mkString(".")
        } else {
          label
        }
      }

      // build a two-level tree: base subcodes (first two segments) and their children
      val groupedByParent: Map[String, Seq[PurchaseNode]] = flatNodes.groupBy(_.parent)

      val nodes = groupedByParent.toSeq.flatMap { case (parentKey, nodesForParent) =>
        // compute base subcode keys (take first two dot segments)
        val baseKeys: Seq[String] = nodesForParent.map { n =>
          val parts = n.code.split("\\.")
          parts.take(2).mkString(".")
        }.distinct

        baseKeys.map { base =>
          // label for the base: prefer an exact match
          val explicitLabelOpt = nodesForParent.find(_.code == base).map(_.label)
          val derivedLabel = explicitLabelOpt.orElse {
            // derive from the first child's label by dropping the last segment
            nodesForParent.find(n => n.code.startsWith(base + ".")).flatMap { child =>
              val l = child.label
              if (l != null && l.startsWith("purchase.sub.")) {
                val parts = l.split("\\.")
                if (parts.length > 3) Some(parts.dropRight(1).mkString(".")) else None
              } else None
            }
          }
          val label = derivedLabel.getOrElse(base)
          // children are those nodes with codes that start with base + '.' and are not the base itself
          // collect children from both flat entries and any nested children on explicit nodes
          val explicitNodeOpt = nodesForParent.find(_.code == base)
          val explicitChildren: Seq[PurchaseNode] = explicitNodeOpt.toSeq.flatMap(_.children)

          val siblingChildren: Seq[PurchaseNode] = nodesForParent
            .filter(n => n.code != base && n.code.startsWith(base + "."))
            .map(n => PurchaseNode(parentKey, n.code, n.label, Seq.empty))

          // merge and deduplicate children by code
          val children = (explicitChildren ++ siblingChildren).groupBy(_.code).map(_._2.head).toSeq.sortBy(_.code)

          PurchaseNode(parentKey, base, label, children)
        }
      }

      key -> nodes
    }.toMap
  } catch {
    case _: Throwable => Map.empty
  }

  def subcodesFor(country: String, parentKey: String): Seq[(String, String)] =
    nodesForCountry(country).toSeq.flatMap(_.filter(_.parent == parentKey).map(n => (n.code, n.label)))

  def subcodesFor(parentKey: String): Seq[(String, String)] =
    mapping.values.toSeq.flatten.filter(_.parent == parentKey).map(n => (n.code, n.label))

  def subcategoriesFor(country: String, parentKey: String, subcode: String): Seq[(String, String)] =
    nodesForCountry(country).toSeq.flatMap(_.filter(n => n.parent == parentKey && n.code == subcode).flatMap(_.children).map(c => (c.code, c.label)))

  private def nodesForCountry(country: String): Option[Seq[PurchaseNode]] = {
    if (country == null) return None
    val key = country.trim

    // Build a list of candidate keys to try. Handle common formats such as
    // "Name, CODE" or "CODE, Name" and cases where the stored value is a
    // full country name. Also try the final token after whitespace and a
    // two-letter alpha-2 fallback.
    val commaParts = key.split(",").map(_.trim).filter(_.nonEmpty)
    val spaceParts = key.split(" ").map(_.trim).filter(_.nonEmpty)

    val candidates = Seq(
      key,
      key.toUpperCase,
      key.trim,
      key.trim.toUpperCase
    ) ++
      commaParts.reverse ++ // prefer part after comma (often the code)
      commaParts.reverse.map(_.toUpperCase) ++
      spaceParts.reverse ++ // prefer last space-delimited token
      spaceParts.reverse.map(_.toUpperCase) ++
      (if (key.length >= 2) Seq(key.takeRight(2).toUpperCase) else Seq.empty)

    candidates.iterator.flatMap(c => mapping.get(c)).toSeq.headOption
  }

  def buildRadioItems(options: Seq[(String, String)], msgs: play.api.i18n.Messages): Seq[RadioItem] =
    options.zipWithIndex.map { case ((code, labelKey), idx) =>
      def stripLeadingNumeric(key: String): String = {
        val parts = key.split("\\.")
        if (parts.length >= 4 && parts(0) == "purchase" && parts(1) == "sub") {
          (parts.take(3) ++ parts.drop(4)).mkString(".")
        } else key
      }

      def normalizeLabelKey(label: String): String = {
        if (label == null) return ""
        val prefix = "purchase.sub."
        if (!label.startsWith(prefix)) return label
        val rest = label.substring(prefix.length)
        val parts = rest.split("\\.")
        if (parts.length >= 3 && parts(1).matches("\\d+")) {
          val newRest = parts.head +: parts.drop(2)
          prefix + newRest.mkString(".")
        } else label
      }

      val candidates = Seq(
        labelKey,
        normalizeLabel(labelKey, code),
        normalizeLabelKey(labelKey),
        stripLeadingNumeric(labelKey)
      ).distinct

      val label = candidates.collectFirst({ case k if msgs.isDefinedAt(k) => msgs(k) }).getOrElse(labelKey)
      RadioItem(
        content = uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text(label),
        value = Some(code),
        id = Some(s"value_$idx")
      )
    }
}
