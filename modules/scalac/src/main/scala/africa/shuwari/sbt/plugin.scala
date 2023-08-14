package africa.shuwari.sbt

import africa.shuwari.sbt.BuildModePlugin.Mode
import africa.shuwari.sbt.ScalaOptionsKeys.*
import org.typelevel.scalacoptions.ScalacOption
import sbt.{ScalaVersion as _, *}

import scala.collection.GenTraversableOnce
import scala.language.implicitConversions

object ScalaOptionsPlugin extends AutoPlugin {

  object autoImport {

    final val ScalaCompiler = ScalaOptionsKeys
    final val ScalacOption = org.typelevel.scalacoptions.ScalacOption
    type ScalacOption = org.typelevel.scalacoptions.ScalacOption

    implicit def optionsToOptionsOps(opts: Set[ScalacOption]): OptionsOps =
      OptionsOps(opts)
  }

  final case class OptionsOps(opts: Set[ScalacOption]) extends AnyVal {
    def and(option: ScalacOption): Set[ScalacOption] = opts + option
    def and(options: GenTraversableOnce[ScalacOption]): Set[ScalacOption] = opts ++ options
    def except(option: ScalacOption): Set[ScalacOption] = opts.filterNot(_ == option)
    def except(options: GenTraversableOnce[ScalacOption]): Set[ScalacOption] =
      opts.diff(options.toSet)
  }

  override def requires: Plugins = BuildModePlugin

  override def trigger: PluginTrigger = allRequirements

  override def projectSettings: Seq[Setting[?]] = List(
    basePackage := None,
    developmentOptions := ScalaCompilerOptions.developmentBuild.value,
    integrationOptions := ScalaCompilerOptions.integrationBuild.value,
    releaseOptions := ScalaCompilerOptions.releaseBuild.value,
    Compile / Keys.compile / Keys.scalacOptions := {
      val options = BuildModePlugin.buildMode.value match {
        case Mode.Development =>
          developmentOptions.value
        case Mode.Integration =>
          integrationOptions.value
        case Mode.Release => releaseOptions.value
      }
      ScalaCompilerOptions.optionsResolver.value.toList.flatMap(opt =>
        opt.option :: opt.args
      )
    },
    Test / Keys.compile / Keys.scalacOptions :=
      ScalaCompilerOptions
        .optionsForVersion(
          Keys.scalaVersion.value,
          developmentOptions.value,
          Keys.streams.value.log
        )
        .toList
        .flatMap(opt => opt.option :: opt.args)
  )

}
