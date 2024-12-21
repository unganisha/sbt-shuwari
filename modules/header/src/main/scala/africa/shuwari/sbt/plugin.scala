package africa.shuwari.sbt

import de.heikoseeberger.sbtheader.CommentCreator
import de.heikoseeberger.sbtheader.CommentStyle
import de.heikoseeberger.sbtheader.FileType
import de.heikoseeberger.sbtheader.HeaderPlugin
import de.heikoseeberger.sbtheader.HeaderPlugin.autoImport.HeaderPattern
import de.heikoseeberger.sbtheader.HeaderPlugin.autoImport.*
import de.heikoseeberger.sbtheader.License
import de.heikoseeberger.sbtheader.LicenseDetection

import sbt.Def
import sbt.Keys.*
import sbt.URL
import sbt.*
import sbt.plugins.JvmPlugin

import scala.language.implicitConversions

object ShuwariHeaderPlugin extends AutoPlugin {

  val headerCopyrightHolder = settingKey[Option[String]](
    "The name of the organization or individual that holds the copyright."
  )

  override def requires: Plugins = JvmPlugin && HeaderPlugin

  override def trigger: PluginTrigger = allRequirements

  override def buildSettings: Seq[Def.Setting[?]] = List(
    headerCopyrightHolder := None
  )

  override def projectSettings: Seq[Setting[?]] = Seq(
    headerMappings := {
      val scalaCommentStyleTypes =
        List(FileType.java, FileType.scala)
          .map(_ -> scalaBlockCommentStyle)
      headerMappings.value ++ scalaCommentStyleTypes
    },
    headerEmptyLine := false,
    headerLicense := defaultLicensing.value
  )

  object autoImport {
    implicit def toProjectOps(p: Project): ProjectOpts = ProjectOpts(p)

    def headerCopyrightHolder: SettingKey[Option[String]] = ShuwariHeaderPlugin.headerCopyrightHolder
    def internalSoftware: Setting[Seq[(String, URL)]] = internalLicenseSetting
    def apacheLicensed: Setting[Seq[(String, URL)]] = apacheLicenseSetting
    def gplv3Licensed: Setting[Seq[(String, URL)]] = gplv3LicenseSetting
  }

  final private case class DefaultBlockCommentCreator(
    commentPrefix: String,
    commentSuffix: Option[String],
    linePrefix: String,
    lineSuffix: String,
    boundaryCharacter: String,
    postIndent: Boolean,
    indentSize: Int,
    preLengthModifier: Int,
    postLengthModifier: Int
  ) extends CommentCreator {

    override def apply(text: String, existingText: Option[String]): String = {
      val longestLineSize = text.linesIterator.map(_.length).max

      val maxLength =
        longestLineSize + linePrefix.length + lineSuffix.length + indentSize + 1

      def padding(count: Int) = " " * count

      val indent = padding(indentSize)

      def processLines(line: String) =
        (line, linePrefix, lineSuffix) match {
          case (str, pre, post) =>
            def first = s"$indent$pre $str"
            s"$first${padding(maxLength - (first.length + post.length))} $post"
        }

      def firstLine =
        s"$commentPrefix${boundaryCharacter * (preLengthModifier + maxLength - commentPrefix.length)}"

      def lastLine =
        s"${if (postIndent) indent else ""}${commentSuffix
            .map(str => (boundaryCharacter * (postLengthModifier + maxLength - (str.length + indent.length))) + str)
            .getOrElse(firstLine)}"

      (firstLine +: text.linesIterator.toList.map(processLines) :+ lastLine)
        .mkString(System.lineSeparator)

    }
  }

  private val scalaBlockCommentStyle: CommentStyle = CommentStyle(
    DefaultBlockCommentCreator(
      raw"/*",
      Some(raw"*/"),
      "*",
      "*",
      "*",
      postIndent = true,
      1,
      1,
      2
    ),
    HeaderPattern.commentBetween("""/\*+""", "*", """\*/""")
  )

  private def defaultLicensing: Def.Initialize[Option[License]] = {
    val apacheLicensePattern =
      """(?i)\bapache[- ]license[- ]2\.0|apache[- ]2\.0|apache-2\.0|apache\s*2\.0|apache-2\.0\b""".r
    val gplv3Pattern =
      """(?i)\bgpl[- ]v3|gnu[- ]gpl[- ]v3|gnu[- ]general[- ]public[- ]license[- ]v3|gpl[- ]3\.0|gpl[- ]3|gnu[- ]gpl[- ]3\.0|gnu[- ]gpl[- ]3|gpl-3\.0\b""".r

    Def.setting {
      val licensesList = licenses.value.toList

      val matchedLicense = licensesList.collectFirst {
        case (name, _) if apacheLicensePattern.findFirstIn(name).isDefined => Headers.apacheLicenseHeader.value
        case (name, _) if gplv3Pattern.findFirstIn(name).isDefined         => Headers.gplv3LicenseHeader.value
      }

      matchedLicense.orElse(
        if (licensesList.isEmpty)
          Some(Headers.internalSoftwareHeader.value)
        else
          LicenseDetection(
            licensesList,
            organizationName.value,
            startYear.value,
            headerEndYear.value,
            headerLicenseStyle.value
          )
      )
    }
  }

  object Headers {
    private def end(str: String): String =
      if (str.endsWith(".")) str else str + "."

    def apacheLicenseHeader: Def.Initialize[License.Custom] =
      Def.setting {
        def copyRightHolder = headerCopyrightHolder.value.getOrElse(Keys.organizationName.value)
        License.Custom(
          s"""|Copyright © ${end(copyRightHolder)}
              |
              |This file is licensed to you under the terms of the Apache
              |License Version 2.0 (the "License"); you may not use this
              |file except in compliance with the License. You may obtain
              |a copy of the License at:
              |
              |    https://www.apache.org/licenses/LICENSE-2.0
              |
              |Unless required by applicable law or agreed to in writing,
              |software distributed under the License is distributed on an
              |"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
              |either express or implied. See the License for the specific
              |language governing permissions and limitations under the
              |License.
              |""".stripMargin
        )
      }

    def gplv3LicenseHeader: Def.Initialize[License.Custom] =
      Def.setting {
        def copyRightHolder = headerCopyrightHolder.value.getOrElse(Keys.organizationName.value)
        License.Custom(
          s"""|Copyright © ${end(copyRightHolder)}
              |
              |This software is licensed to you under the terms of the GNU
              |General Public License, as published by the Free Software
              |Foundation; you may not use this file except in compliance
              |with either version 3 of the License, or (at your option) any
              |later version. You should have received a copy of the GNU
              |General Public License along with this software. You may
              |obtain a copy of the License at:
              |
              |    https://www.gnu.org/licenses/gpl-3.0.en.html
              |
              |Unless required by applicable law or agreed to in writing,
              |software distributed under the License is distributed on an
              |"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
              |either express or implied. See the License for the specific
              |language governing permissions and limitations under the
              |License.
              |""".stripMargin
        )
      }

    def internalSoftwareHeader: Def.Initialize[License.Custom] =
      Def.setting {
        def organizationName = Keys.organizationName.value
        License.Custom(
          s"""|Copyright © ${end(organizationName)} All rights reserved.
              |
              |This work is the sole property of $organizationName,
              |for internal use by ${organizationName}; and may not be
              |copied, used, and/or distributed without the express
              |permission of ${end(organizationName)}
              |""".stripMargin
        )
      }
  }

  private def internalLicenseSetting: Def.Setting[Seq[(String, URL)]] = licenses := Seq.empty

  private def apacheLicenseSetting: Def.Setting[Seq[(String, URL)]] = licenses := List(
    "Apache-2.0" -> url("https://www.apache.org/licenses/LICENSE-2.0.txt")
  )

  private def gplv3LicenseSetting: Def.Setting[Seq[(String, URL)]] = licenses := List(
    "GPL-3.0" -> url("https://www.gnu.org/licenses/gpl-3.0.en.html#license-text")
  )

  final case class ProjectOpts(p: Project) extends AnyVal {
    def internalSoftware: Project = p.settings(internalLicenseSetting)
    def apacheLicensed: Project = p.settings(apacheLicenseSetting)
    def gplv3Licensed: Project = p.settings(gplv3LicenseSetting)
  }
}
