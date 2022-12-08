package koroga.sbt

import sbt._
import sbt.Keys._
import sbt.plugins.JvmPlugin

import scala.language.implicitConversions

object ShuwariCorePlugin extends AutoPlugin {

  override def requires: Plugins = JvmPlugin

  override def trigger: PluginTrigger = allRequirements

  override def buildSettings: Seq[Setting[_]] = baseBuildSettings
  override def projectSettings: Seq[Setting[_]] =
    baseProjectSettings ++ Seq(
      // LocalRootProject / crossScalaVersions := List((LocalRootProject / scalaVersion).value),
      LocalRootProject / organizationHomepage := None,
      LocalRootProject / organizationName := "",
      LocalRootProject / scalaVersion := "3.2.1",
      LocalRootProject / scmInfo := None
    )

  object autoImport {

    implicit def projectToProjectOps(p: Project): ProjectOps = ProjectOps(p)

    def shuwariProject: List[Setting[_]] = defaultOrganisationSettings
    def notPublished: List[Setting[_]] = projectNotPublished

  }

  final case class ProjectOps(p: Project) extends AnyVal {
    def shuwariProject: Project = p.settings(defaultOrganisationSettings)
    def notPublished: Project = p.settings(projectNotPublished)
  }

  private def projectNotPublished: List[Setting[_]] = List(
    publish / skip := true,
    publish := {},
    publishLocal := {},
    publishArtifact := false
  )

  private def defaultOrganisationSettings: List[Setting[_]] = List(
    // organization := ((LocalRootProject / organization) or (LocalRootProject / normalizedName)).value,
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

  private def pomIncludeRepositorySetting = pomIncludeRepository := (_ ⇒ false)

  private def baseBuildSettings = List(
    crossScalaVersions,
    organizationHomepage,
    organizationName,
    scalaVersion,
    scmInfo
  ).map(fromRoot(_))

  private def baseProjectSettings =
    (List(
      organizationName,
      organizationHomepage,
      organization,
      apiURL,
      developers,
      homepage,
      licenses,
      startYear,
      version
    ).map(fromRoot(_)) :+ pomIncludeRepositorySetting) ++ baseBuildSettings

  private def fromRoot[A](key: SettingKey[A]): Setting[A] =
    key := ((LocalRootProject / key)).value

  // private def scmInfoSetting =
  //   scmInfo := ((LocalRootProject / scmInfo) ?? None).value

}
