package africa.shuwari.sbt

import org.typelevel.scalacoptions.ScalaVersion
import org.typelevel.scalacoptions.ScalacOption
import org.typelevel.scalacoptions.ScalacOptions
import sbt.*

import scala.util.Try

import africa.shuwari.sbt.JSKeys.*

object ScalaCompilerOptions {

  final val options = ScalacOptions

//  ScalacOptions.explain

  def dottyOnly(v: ScalaVersion): Boolean = v.major == 3L // scalafix:ok

  def checkMods: ScalacOption = options.privateOption("check-mods", dottyOnly)

  def safeInit: ScalacOption = options.privateOption("safe-init", dottyOnly)

  def checkReentrant: ScalacOption =
    options.privateOption("check-reentrant", dottyOnly)

  def requireTargetName: ScalacOption =
    options.privateOption("require-targetName", dottyOnly)

  def maxInlines: ScalacOption =
    options.advancedOption("max-inlines:64", dottyOnly)

  def explicitNulls: ScalacOption =
    options.privateOption("explicit-nulls", dottyOnly)

  def defaultOptions: Set[ScalacOption] = {
    def default = options.default
    def optionsAdditional = options.fatalWarningOptions ++ Set(
      options.explain,
      options.explainTypes,
      checkMods,
      checkReentrant,
      explicitNulls,
      maxInlines,
      requireTargetName,
      safeInit
    )
    def optionsExcluded = Set(
      options.languageImplicitConversions
    )
    (default ++ optionsAdditional) -- optionsExcluded
  }

  def developmentOptions: Def.Initialize[Set[ScalacOption]] =
    Def.setting(defaultOptions)

  def ciOptions(
    releaseOptionsKey: SettingKey[Set[ScalacOption]]
  ): Def.Initialize[Set[ScalacOption]] =
    Def.setting(releaseOptionsKey.value + options.optimizerWarnings)

  def tpolecatReleaseOptionsSetting(
    developmentOptions: SettingKey[Set[ScalacOption]]
  ): Def.Initialize[Set[ScalacOption]] = Def.setting {
    val base = basePackages.value
    def opts = developmentOptions.value
    if (base.nonEmpty)
      opts ++ options.optimizerOptions(base*)
    else opts
  }

}
