package africa.shuwari.sbt

import _root_.io.github.davidgregory084.TpolecatPlugin.{autoImport => tp}
import _root_.io.github.davidgregory084.*
import org.checkerframework.checker.nullness.Opt
import sbt.{ScalaVersion => _, _}

import scala.Ordering.Implicits._
import scala.collection.GenTraversableOnce
import scala.language.implicitConversions

import BuildModePlugin.Mode

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

    def dottyOnly(v: ScalaVersion) = v.major == 3L

    def V213Only(v: ScalaVersion) =
      v.isBetween(ScalaVersion(2, 13, 8), ScalaVersion(3, 0, 0))

    def dottyExplain = ScalacOption("-explain", dottyOnly)

    def dottyCheckMods = options.privateOption("check-mods", dottyOnly)

    def dottyCheckInit = options.privateOption("check-init", dottyOnly)

    def dottyCheckReentrant =
      options.privateOption("check-reentrant", dottyOnly)

    def dottyRequireTargetName = options.privateOption("require-targetName")

    def dottyMaxInlines = options.advancedOption("max-inlines:64", dottyOnly)

    def developmentBuild = Def.task(
      options.default
        .and(options.fatalWarningOptions)
        .and(dottyExplain)
        .and(dottyCheckMods)
        .and(dottyCheckReentrant)
        .and(dottyCheckInit)
        .and(dottyMaxInlines)
        .and(dottyRequireTargetName)
        .except(options.languageImplicitConversions)
    )

    def integrationBuild =
      releaseBuildOptions.map(_.and(options.optimizerWarnings))

    def releaseBuild = Def.task {
      def sources =
        (Compile / Keys.unmanagedSources).value
          .collect { case f if f.name.endsWith(".scala") => f.getAbsolutePath }
          .mkString(":")
      def optimiserPattern = basePackage.value.map(_ + ".**").getOrElse(sources)
      developmentBuildOptions.value
        .and(options.optimizerOptions(optimiserPattern))
    }

  }

  def optionsResolver = Def.task {
    BuildModePlugin.buildMode.value match {
      case Mode.Development => developmentBuildOptions.value
      case Mode.Integration => integrationBuildOptions.value
      case Mode.Release     => releaseBuildOptions.value
    }
  }

  def optionsModeResolver: Def.Initialize[OptionsMode] = Def.setting {
    BuildModePlugin.buildMode.value match {
      case Mode.Development => DevMode
      case Mode.Integration => CiMode
      case Mode.Release     => ReleaseMode
    }
  }

  override def buildSettings: Seq[Setting[_]] = Seq(
    tp.tpolecatOptionsMode := optionsModeResolver.value
  )

  override def projectSettings: Seq[Setting[_]] = List(
    basePackage := None,
    tp.tpolecatDevModeOptions := Set(),
    tp.tpolecatCiModeOptions := Set(),
    tp.tpolecatReleaseModeOptions := Set(),
    developmentBuildOptions := Options.developmentBuild.value,
    integrationBuildOptions := Options.integrationBuild.value,
    releaseBuildOptions := Options.releaseBuild.value,
    Compile / Keys.scalacOptions := {
      val options = BuildModePlugin.buildMode.value match {
        case Mode.Development =>
          developmentBuildOptions.value
        case Mode.Integration =>
          integrationBuildOptions.value
        case Mode.Release => releaseBuildOptions.value
      }
      tp.scalacOptionsFor(Keys.scalaVersion.value, options)
    },
    Test / Keys.scalacOptions := tp.scalacOptionsFor(
      Keys.version.value,
      developmentBuildOptions.value
    )
  )

  override def requires: Plugins = BuildModePlugin && TpolecatPlugin
  override def trigger: PluginTrigger = allRequirements

}
