package africa.shuwari.sbt

import io.github.davidgregory084.ScalacOption
import org.scalajs.linker.interface.ESFeatures
import org.scalajs.linker.interface.ModuleKind
import org.scalajs.linker.interface.ModuleSplitStyle
import org.scalajs.linker.interface.OutputPatterns
import org.scalajs.linker.interface.StandardConfig
import org.scalajs.sbtplugin.ScalaJSPlugin
import sbt.AutoPlugin
import sbt.Keys.*
import sbt.Plugins
import sbt.*

object ShuwariJsPlugin extends AutoPlugin {

  override def requires: Plugins = ScalaJSPlugin && ScalaCompileOptionsPlugin

  def scalaJsDottyOption =
    ScalacOption("-scalajs", ScalaCompileOptionsPlugin.Options.dottyOnly)

  override def projectSettings: Seq[Def.Setting[_]] = {
    import ScalaJSPlugin.autoImport.*
    Seq(
      ScalaCompileOptionsPlugin.developmentBuildOptions ~= (_ + scalaJsDottyOption),
      scalaJSLinkerConfig := defaultLinkerConfigOptions.value
    )
  }

  def defaultLinkerConfigOptions = Def.setting {
    def explicitsplit =
      ScalaCompileOptionsPlugin.basePackage.value.map(p =>
        ModuleSplitStyle.SmallModulesFor(List(p))
      )
    def defaultSplit = if (
      BuildModePlugin.buildMode.value == BuildModePlugin.DevelopmentBuild
    ) ModuleSplitStyle.FewestModules
    else ModuleSplitStyle.SmallestModules

    StandardConfig()
      .withModuleKind(ModuleKind.ESModule)
      .withESFeatures(ESFeatures.Defaults)
      .withOutputPatterns(OutputPatterns.fromJSFile("%s.mjs"))
      .withModuleSplitStyle(explicitsplit.getOrElse(defaultSplit))
  }
}
