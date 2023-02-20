package africa.shuwari.sbt

import org.scalajs.linker.interface.ESFeatures
import org.scalajs.linker.interface.ModuleKind
import org.scalajs.linker.interface.ModuleSplitStyle
import org.scalajs.linker.interface.OutputPatterns
import org.scalajs.linker.interface.StandardConfig
import org.scalajs.sbtplugin.ScalaJSPlugin
import org.typelevel.scalacoptions.ScalacOption
import sbt.Keys._
import sbt._

object JSPlugin extends AutoPlugin {

  override def requires: Plugins = ScalaJSPlugin && ScalaOptionsPlugin

  override def trigger: PluginTrigger = allRequirements

  def scalaJsDottyOption =
    ScalacOption("-scalajs", ScalaCompilerOptions.dottyOnly)

  override def projectSettings: Seq[Def.Setting[_]] = {
    import ScalaJSPlugin.autoImport.*
    Seq(
      ScalaOptionsKeys.developmentOptions ~= (_ + scalaJsDottyOption),
      scalaJSLinkerConfig := defaultLinkerConfigOptions.value
    )
  }

  def defaultLinkerConfigOptions = Def.setting {
    def explicitsplit =
      ScalaOptionsKeys.basePackage.value.map(p =>
        ModuleSplitStyle.SmallModulesFor(List(p))
      )
    def defaultSplit = if (
      BuildModePlugin.buildMode.value == BuildModePlugin.Mode.Development
    ) ModuleSplitStyle.FewestModules
    else ModuleSplitStyle.SmallestModules

    StandardConfig()
      .withModuleKind(ModuleKind.ESModule)
      .withESFeatures(ESFeatures.Defaults)
      .withOutputPatterns(OutputPatterns.fromJSFile("%s.mjs"))
      .withModuleSplitStyle(explicitsplit.getOrElse(defaultSplit))
  }
}
