package africa.shuwari.sbt

import _root_.io.github.davidgregory084.CiMode
import _root_.io.github.davidgregory084.DevMode
import _root_.io.github.davidgregory084.OptionsMode
import _root_.io.github.davidgregory084.ScalaVersion
import _root_.io.github.davidgregory084.ScalacOption
import _root_.io.github.davidgregory084.ScalacOptions
import _root_.io.github.davidgregory084.TpolecatPlugin
import _root_.io.github.davidgregory084.TpolecatPlugin.{autoImport => tp}
import africa.shuwari.sbt.BuildModePlugin.DevelopmentBuild
import africa.shuwari.sbt.BuildModePlugin.IntegrationBuild
import africa.shuwari.sbt.BuildModePlugin.ReleaseBuild
import sbt.{ScalaVersion => _, _}

import scala.Ordering.Implicits._
import scala.collection.GenTraversableOnce
import org.checkerframework.checker.nullness.Opt

import scala.language.implicitConversions

object ScalaCompileOptionsPlugin extends AutoPlugin {

  val basePackage = settingKey[Option[String]](
    "Base package name. Used to enable, and limit inlining to the specified specified pattern."
  )

  val developmentBuildOptions = taskKey[Set[ScalacOption]](
    "Defines compiler options used during DevelopmentBuild build."
  )
  val integrationBuildOptions = taskKey[Set[ScalacOption]](
    "Defines compiler options used during IntegrationBuild build."
  )
  val releaseBuildOptions = taskKey[Set[ScalacOption]](
    "Defines compiler options used during ReleaseBuild builds.."
  )

  final case class OptionsOps(opts: Set[ScalacOption]) extends AnyVal {
    def and(option: ScalacOption) = opts + option
    def and(options: GenTraversableOnce[ScalacOption]) = opts ++ options
    def except(option: ScalacOption) = opts.filterNot(_ == option)
    def except(options: GenTraversableOnce[ScalacOption]) =
      opts.diff(options.toSet)
  }

  implicit def optionsToOptionsOps(opts: Set[ScalacOption]) =
    OptionsOps(opts)

  object Options {

    def options = tp.ScalacOptions

    def dottyOnly(v: ScalaVersion) =
      v.isBetween(ScalaVersion(3, 0, 0), ScalaVersion(4, 0, 0))

    def V213Only(v: ScalaVersion) =
      v.isBetween(ScalaVersion(2, 13, 8), ScalaVersion(3, 0, 0))

    def dottyExplain = ScalacOption("-explain", dottyOnly)

    def dottyCheckMods = options.privateOption("check-mods", dottyOnly)

    def dottyCheckInit = options.privateOption("check-init", dottyOnly)

    def dottyCheckReentrant =
      options.privateOption("check-reentrant", dottyOnly)

    def dottyRequireTargetName = options.privateOption("require-targetName")

    def dottyMaxInlines = options.advancedOption("max-inlines:64", dottyOnly)

    def developmentOptions =
      options.default
        .and(options.fatalWarningOptions)
        .and(dottyExplain)
        .and(dottyCheckMods)
        .and(dottyCheckReentrant)
        .and(dottyCheckInit)
        .and(dottyMaxInlines)
        .and(dottyRequireTargetName)
        .except(options.languageImplicitConversions)

    def integrationBuildOptions =
      releaseBuildOptions.map(_.and(options.optimizerWarnings))

    def releaseBuildOptions = Def.task {

      def sources =
        (Compile / Keys.sources).value
          .collect { case f if f.name.endsWith(".scala") => f.getAbsolutePath }
          .mkString(":")

      def optimiserPattern = basePackage.value.map(_ + ".**").getOrElse(sources)

      developmentOptions
        .and(options.optimizerOptions(optimiserPattern))
    }

  }

  def optionsResolver = Def.task {
    BuildModePlugin.buildMode.value match {
      case DevelopmentBuild => developmentBuildOptions.value
      case IntegrationBuild => integrationBuildOptions.value
      case ReleaseBuild     => releaseBuildOptions.value
    }
  }

  def optionsModeResolver = Def.setting {
    BuildModePlugin.buildMode.value match {
      case DevelopmentBuild => DevMode.asInstanceOf[OptionsMode]
      case IntegrationBuild => CiMode.asInstanceOf[OptionsMode]
      case ReleaseBuild     => ReleaseBuild.asInstanceOf[OptionsMode]
    }
  }

  override def buildSettings: Seq[Setting[_]] = Seq(
    tp.tpolecatOptionsMode := optionsModeResolver.value
  )

  override def projectSettings: Seq[Setting[_]] = List(
    tp.tpolecatDevModeOptions := Set(),
    tp.tpolecatCiModeOptions := Set(),
    tp.tpolecatReleaseModeOptions := Set(),
    developmentBuildOptions := Options.developmentOptions,
    integrationBuildOptions := Options.releaseBuildOptions.value,
    releaseBuildOptions := Options.releaseBuildOptions.value,
    Compile / Keys.scalacOptions := {
      BuildModePlugin.buildMode.value match {
        case DevelopmentBuild =>
          developmentBuildOptions.value.map(_.toString).toList
        case IntegrationBuild =>
          integrationBuildOptions.value.map(_.toString).toList
        case ReleaseBuild => releaseBuildOptions.value.map(_.toString).toList
      }
    },
    Test / Keys.scalacOptions := {
      BuildModePlugin.buildMode.value match {
        case _ => developmentBuildOptions.value.map(_.toString).toList
      }
    }
  )

  override def requires: Plugins = BuildModePlugin && TpolecatPlugin

}
