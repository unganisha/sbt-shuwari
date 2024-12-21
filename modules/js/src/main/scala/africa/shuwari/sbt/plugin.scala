package africa.shuwari.sbt

import org.scalajs.linker.interface.ESFeatures
import org.scalajs.linker.interface.ModuleKind
import org.scalajs.linker.interface.ModuleSplitStyle
import org.scalajs.linker.interface.OutputPatterns
import org.scalajs.linker.interface.StandardConfig
import org.scalajs.sbtplugin.ScalaJSPlugin
import org.typelevel.scalacoptions.ScalacOption

import sbt.Keys.*
import sbt.*

object JSPlugin extends AutoPlugin {

  override def requires: Plugins = ScalaJSPlugin && ScalaOptionsPlugin

  override def trigger: PluginTrigger = allRequirements

  override def projectSettings: Seq[Def.Setting[?]] = {
    import ScalaJSPlugin.autoImport.*
    Seq(
      scalaJSLinkerConfig := defaultLinkerConfigOptions.value
    )
  }

  object autoImport {
    val shuwarijs = JSKeys
  }

  def defaultLinkerConfigOptions = Def.setting {
    val basePackages = JSKeys.basePackages.value
    def splitStyle = if (basePackages.nonEmpty)
      ModuleSplitStyle.SmallModulesFor(basePackages)
    else if (BuildModePlugin.buildMode.value === BuildModePlugin.Mode.Development) ModuleSplitStyle.FewestModules
    else ModuleSplitStyle.SmallestModules

    StandardConfig()
      .withModuleKind(ModuleKind.ESModule)
      .withESFeatures(ESFeatures.Defaults)
      .withOutputPatterns(OutputPatterns.fromJSFile("%s.mjs"))
      .withModuleSplitStyle(splitStyle)
  }
}
