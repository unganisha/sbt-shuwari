package africa.shuwari.sbt

import sbt.*
import sbt.plugins.JvmPlugin

object BuildModePlugin extends AutoPlugin {

  sealed trait BuildMode extends Product with Serializable
  case object DevelopmentBuild extends BuildMode
  case object IntegrationBuild extends BuildMode
  case object ReleaseBuild extends BuildMode

  object autoImport {
    def DevelopmentBuild = BuildModePlugin.DevelopmentBuild
    def IntegrationBuild = BuildModePlugin.IntegrationBuild
    def ReleaseBuild = BuildModePlugin.ReleaseBuild

    def buildMode = BuildModePlugin.buildMode
  }

  override def trigger: PluginTrigger = allRequirements
  override def requires: Plugins = JvmPlugin

  override def buildSettings: Seq[Setting[_]] = Seq(
    buildMode := buildModeResolver.value
  )

  val buildMode = settingKey[BuildMode](
    "Defines the current BuildMode. Defaults to DevelopmentBuild unless 'profile' property is detected and set to 'deployment'."
  )

  private def buildModeResolver = Def.setting {

    def environmentSetting = "BUILD_MODE"

    def modeIdentifier(mode: BuildMode): String = {
      mode.getClass.getSimpleName
        .dropWhile(_ == '$')
        .split("""(?<!^)(?=[A-Z])""")
        .head
        .toLowerCase
    }

    val modes: Map[String, BuildMode] =
      Set(DevelopmentBuild, IntegrationBuild, ReleaseBuild)
        .map(mode => modeIdentifier(mode) -> mode)
        .toMap

    def validIdentifier(identifier: String): Boolean = {
      val matched =
        identifier.matches(modes.keySet.mkString("(?i)^", "(?:mode)?|", "$"))
      if (!matched) {
        def validIdentifiers = modes.keySet.map(s => "s").mkString(", ")
        sys.error(
          s"""Unknown "$environmentSetting" environment variable identifier specified. Please specify one of""" +
            s"""${validIdentifiers} is defined, as the $environmentSetting environment variable, or set "buildMode" explicitly."""
        )
      } else matched
    }

    sys.env
      .get(environmentSetting)
      .filter(validIdentifier) match {
      case Some(id) => modes.get(id).get
      case _        => DevelopmentBuild
    }
  }

}
