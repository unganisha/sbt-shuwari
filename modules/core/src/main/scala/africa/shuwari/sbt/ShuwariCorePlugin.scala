package koroga.sbt

import sbt._
import sbt.Keys._
import sbt.plugins.JvmPlugin

import sbt.PluginTrigger
object ShuwariCommonPlugin extends AutoPlugin {

  override def requires: Plugins = JvmPlugin

  override def trigger: PluginTrigger = allRequirements

  override def buildSettings: Seq[Setting[_]] = List(
    organizationName := "Shuwari Africa Ltd.",
    organizationHomepage := Some(url("https://shuwari.africa")),
    developers := List(
      Developer(
        "shuwari-dev",
        "Shuwari Africa Ltd. Developer Team",
        "developers at shuwari dot africa",
        url("https://shuwari.africa")
      )
    )
  )

}
