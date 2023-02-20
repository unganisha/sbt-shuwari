package africa.shuwari.sbt

import org.checkerframework.checker.nullness.Opt
import sbt.{ScalaVersion => _, _}

import scala.Ordering.Implicits._
import scala.collection.GenTraversableOnce
import scala.language.implicitConversions

import africa.shuwari.sbt.BuildModePlugin.Mode
import org.typelevel.scalacoptions.ScalacOption
import org.typelevel.scalacoptions.ScalacOptions
import org.typelevel.scalacoptions.ScalaVersion

import africa.shuwari.sbt.ScalaOptionsKeys._

object ScalaOptionsPlugin extends AutoPlugin {

  object autoImport {

    val ScalaCompiler = ScalaOptionsKeys
    val ScalacOption = org.typelevel.scalacoptions.ScalacOption
    type ScalacOption = org.typelevel.scalacoptions.ScalacOption

    implicit def optionsToOptionsOps(opts: Set[ScalacOption]) =
      OptionsOps(opts)
  }

  final case class OptionsOps(opts: Set[ScalacOption]) extends AnyVal {
    def and(option: ScalacOption) = opts + option
    def and(options: GenTraversableOnce[ScalacOption]) = opts ++ options
    def except(option: ScalacOption) = opts.filterNot(_ == option)
    def except(options: GenTraversableOnce[ScalacOption]) =
      opts.diff(options.toSet)
  }

  override def requires: Plugins = BuildModePlugin

  override def trigger: PluginTrigger = allRequirements

  override def projectSettings: Seq[Setting[_]] = List(
    basePackage := None,
    developmentOptions := ScalaCompilerOptions.developmentBuild.value,
    integrationOptions := ScalaCompilerOptions.integrationBuild.value,
    releaseOptions := ScalaCompilerOptions.releaseBuild.value,
    Compile / Keys.scalacOptions := {
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
    Test / Keys.scalacOptions :=
      ScalaCompilerOptions
        .optionsForVersion(
          Keys.version.value,
          developmentOptions.value,
          Keys.streams.value.log
        )
        .toList
        .flatMap(opt => opt.option :: opt.args)
  )

}
