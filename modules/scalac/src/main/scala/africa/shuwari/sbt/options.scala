package africa.shuwari.sbt

import org.typelevel.scalacoptions.ScalacOption
import org.typelevel.scalacoptions.ScalacOptions
import org.typelevel.scalacoptions.ScalaVersion

import africa.shuwari.sbt.ScalaOptionsKeys._

import sbt._
import scala.util.Try

object ScalaCompilerOptions {

  def options = ScalacOptions

  def dottyOnly(v: ScalaVersion) = v.major == 3L

  def V213Only(v: ScalaVersion) =
    v.isBetween(ScalaVersion(2, 13, 8), ScalaVersion(3, 0, 0))

  def dottyOnlyProject(scalaVersion: String, crossScalaVersions: Seq[String]) =
    (CrossVersion.partialVersion(scalaVersion) match {
      case Some((major, _)) => major == 3L
      case _                => false
    }) && (crossScalaVersions.size < 2)

  def dottyExplain = ScalacOption("-explain", dottyOnly)

  def dottyCheckMods = options.privateOption("check-mods", dottyOnly)

  def dottySafeInit = options.privateOption("safe-init", dottyOnly)

  def dottyCheckReentrant =
    options.privateOption("check-reentrant", dottyOnly)

  def dottyRequireTargetName =
    options.privateOption("require-targetName", dottyOnly)

  def dottyMaxInlines = options.advancedOption("max-inlines:64", dottyOnly)

  def dottyExplicitNulls = options.privateOption("explicit-nulls", dottyOnly)

  def developmentBuild = Def.task(
    (options.default ++ options.fatalWarningOptions ++ Set(
      dottyExplain,
      dottyCheckMods,
      dottyCheckReentrant,
      dottySafeInit,
      dottyMaxInlines,
      dottyRequireTargetName
    ))
      - (options.languageImplicitConversions)
  )

  def integrationBuild =
    releaseOptions.map(_ + (options.optimizerWarnings))

  def releaseBuild = Def.task {
    def sources =
      (Compile / Keys.unmanagedSources).value
        .collect { case f if f.name.endsWith(".scala") => f.getAbsolutePath }
        .mkString(":")
    def optimiserPattern = basePackage.value.map(_ + ".**").getOrElse(sources)
    developmentOptions.value ++ (options.optimizerOptions(optimiserPattern))
  }

  def optionsForVersion(
      version: String,
      options: Set[ScalacOption],
      logger: Logger
  ) = {
    val partialVersion = CrossVersion.partialVersion(version)
    val patch = Try(version.split("\\.")(2).toLong)
    val Regex = """^(\d+)\.(\d+)\.(\d+)(?:$|[-+])""".r
    version match {
      case Regex(major, minor, patch) =>
        options.filter(option =>
          option.isSupported(
            ScalaVersion(major.toLong, minor.toLong, patch.toLong)
          )
        )
      case _ =>
        logger.error(
          s"Unable to determine valid Scala version for compiler options generation. Provided Scala version: $version"
        )
        Set.empty[ScalacOption]
    }
  }

  def optionsResolver = Def.task {
    val modeOptions = BuildModePlugin.buildMode.value match {
      case BuildModePlugin.Mode.Development => developmentOptions.value
      case BuildModePlugin.Mode.Integration => integrationOptions.value
      case BuildModePlugin.Mode.Release     => releaseOptions.value
    }
    optionsForVersion(
      Keys.scalaVersion.value,
      modeOptions,
      Keys.streams.value.log
    )
  }
}
