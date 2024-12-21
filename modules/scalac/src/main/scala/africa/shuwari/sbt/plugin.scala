package africa.shuwari.sbt

import org.typelevel.sbt.tpolecat.CiMode
import org.typelevel.sbt.tpolecat.ReleaseMode
import org.typelevel.sbt.tpolecat.TpolecatPlugin
import org.typelevel.sbt.tpolecat.VerboseMode
import org.typelevel.scalacoptions.ScalacOption

import scala.collection.GenTraversableOnce

import sbt.{ScalaVersion as _, *}

import africa.shuwari.sbt.BuildModePlugin.Mode
import africa.shuwari.sbt.JSKeys.*

import scala.language.implicitConversions

object ScalaOptionsPlugin extends AutoPlugin {

  object autoImport {
    final val ScalaCompiler = JSKeys
    final val ScalacOption = org.typelevel.scalacoptions.ScalacOption
    type ScalacOption = org.typelevel.scalacoptions.ScalacOption

  }

  override def requires: Plugins = BuildModePlugin && TpolecatPlugin

  override def trigger: PluginTrigger = allRequirements

  def tpolecatPluginModeSetting = Def.setting {
    BuildModePlugin.buildMode.value match {
      case Mode.Development => VerboseMode
      case Mode.Integration => CiMode
      case Mode.Release     => ReleaseMode
    }
  }

  override def projectSettings: Seq[Setting[?]] = List(
    basePackages := List.empty,
    TpolecatPlugin.autoImport.tpolecatVerboseModeOptions := TpolecatPlugin.autoImport.tpolecatDevModeOptions.value,
    TpolecatPlugin.autoImport.tpolecatDevModeOptions := ScalaCompilerOptions.developmentOptions.value,
    TpolecatPlugin.autoImport.tpolecatCiModeOptions := ScalaCompilerOptions
      .ciOptions(TpolecatPlugin.autoImport.tpolecatReleaseModeOptions)
      .value,
    TpolecatPlugin.autoImport.tpolecatReleaseModeOptions := ScalaCompilerOptions
      .tpolecatReleaseOptionsSetting(TpolecatPlugin.autoImport.tpolecatDevModeOptions)
      .value,
    TpolecatPlugin.autoImport.tpolecatOptionsMode := tpolecatPluginModeSetting.value
  )
}
