package africa.shuwari.sbt

import sbt.*
import sbt.plugins.JvmPlugin

object BuildModePlugin extends AutoPlugin {

  sealed trait Mode extends Product with Serializable
  object Mode {
    case object Development extends Mode
    case object Integration extends Mode
    case object Release extends Mode
  }

  object autoImport {
    def Mode = BuildModePlugin.Mode
    def buildMode = BuildModePlugin.buildMode
  }

  override def trigger: PluginTrigger = allRequirements
  override def requires: Plugins = JvmPlugin

  override def buildSettings: Seq[Setting[_]] = Seq(
    buildMode := buildModeResolver.value
  )

  val buildMode = settingKey[Mode](
    "Defines the current BuildMode. Defaults to DevelopmentBuild unless 'profile' property is detected and set to 'deployment'."
  )

  private def buildModeResolver = Def.setting {

    def environmentSetting = "BUILD_MODE"

    def modeIdentifier(mode: Mode): String = {
      mode.getClass.getSimpleName
        .dropWhile(_ == '$')
        .toLowerCase
    }

    val modes: Map[String, Mode] =
      Set(Mode.Development, Mode.Integration, Mode.Release)
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
      case _        => Mode.Development
    }
  }

}
