package africa.shuwari.sbt

import de.heikoseeberger.sbtheader.CommentBlockCreator
import de.heikoseeberger.sbtheader.CommentCreator
import de.heikoseeberger.sbtheader.CommentStyle
import de.heikoseeberger.sbtheader.HeaderPlugin
import de.heikoseeberger.sbtheader.HeaderPlugin.autoImport.HeaderLicense
import de.heikoseeberger.sbtheader.HeaderPlugin.autoImport.*
import de.heikoseeberger.sbtheader.HeaderPlugin.autoImport.headerLicense
import sbt.Keys.*
import sbt.*
import sbt.plugins.JvmPlugin

import scala.language.implicitConversions

object ShuwariHeaderPlugin extends AutoPlugin {

  override def requires: Plugins = JvmPlugin && HeaderPlugin

  override def trigger: PluginTrigger = allRequirements

  override def projectSettings: Seq[Setting[?]] = Seq(
    headerMappings := {
      val scalaCommentStyleTypes =
        List(HeaderFileType.java, HeaderFileType.scala)
          .map(_ -> scalaBlockCommentStyle)
      headerMappings.value ++ scalaCommentStyleTypes
    },
    headerEmptyLine := false,
    headerLicense := defaultLicensing.value
  )

  object autoImport {
    implicit def toProjectOps(p: Project): ProjectOpts = ProjectOpts(p)

    def internalSoftware = internalSoftwareLicensesSetting
    def apacheLicensed = apacheLicensedLicensesSetting
  }

  final case class DefaultBlockCommentCreator(
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
        longestLineSize + linePrefix.size + lineSuffix.size + indentSize + 1

      def padding(count: Int) = " " * count

      val indent = padding(indentSize)

      def processLines(line: String) =
        (line, linePrefix, lineSuffix) match {
          case (str, pre, post) =>
            def first = s"$indent$pre $str"
            s"$first${padding(maxLength - (first.size + post.size))} $post"
        }

      def firstLine =
        s"$commentPrefix${boundaryCharacter * (preLengthModifier + maxLength - commentPrefix.size)}"

      def lastLine =
        s"${if (postIndent) indent else ""}${commentSuffix
            .map(str => (boundaryCharacter * (postLengthModifier + maxLength - (str.size + indent.size))) + str)
            .getOrElse(firstLine)}"

      (firstLine +: text.linesIterator.toList.map(processLines) :+ lastLine)
        .mkString(System.lineSeparator)

    }
  }

  val scalaBlockCommentStyle: CommentStyle = CommentStyle(
    DefaultBlockCommentCreator(
      raw"/*",
      Some(raw"*/"),
      "*",
      "*",
      "*",
      true,
      1,
      1,
      2
    ),
    HeaderPattern.commentBetween("""/\*+""", "*", """\*/""")
  )

  def defaultLicensing: Def.Initialize[Option[HeaderLicense.Custom]] =
    Def.setting(
      licenses.value
        .collectFirst {
          case (name, _) if name.matches("(?i)apache.+2[.]0$") =>
            Headers.apacheLicenseHeader.value
        }
        .orElse(Some(Headers.internalSoftwareHeader.value))
    )

  object Headers {
    private def end(str: String): String =
      if (str.endsWith(".")) str else str + "."

    def apacheLicenseHeader: Def.Initialize[HeaderLicense.Custom] =
      Def.setting {
        def organizationName = Keys.organizationName.value
        HeaderLicense.Custom(
          s"""|Copyright © ${end(organizationName)} All rights reserved.
              |
              |${organizationName} licenses this file to you under the terms
              |of the Apache License Version 2.0 (the "License"); you may 
              |not use this file except in compliance with the License. You
              |may obtain a copy of the License at:
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

    def internalSoftwareHeader: Def.Initialize[HeaderLicense.Custom] =
      Def.setting {
        def organizationName = Keys.organizationName.value
        HeaderLicense.Custom(
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

  def internalSoftwareLicensesSetting = licenses := Seq.empty
  def apacheLicensedLicensesSetting = licenses := List(
    "Apache License 2.0" -> url(
      "https://www.apache.org/licenses/LICENSE-2.0.txt"
    )
  )

  final case class ProjectOpts(p: Project) extends AnyVal {
    def internalSoftware = p.settings(internalSoftwareLicensesSetting)
    def apacheLicensed = p.settings(apacheLicensedLicensesSetting)
  }
}
