package koroga.sbt

import sbt._
import sbt.Keys._
import sbt.plugins.JvmPlugin

import scala.language.implicitConversions

object ShuwariCorePlugin extends AutoPlugin {

  override def requires: Plugins = JvmPlugin

  override def trigger: PluginTrigger = allRequirements

  override def buildSettings: Seq[Setting[_]] =
    baseBuildSettings ++ circularReferenceDefaults(
      scmInfo -> None
    )

  override def projectSettings: Seq[Setting[_]] =
    baseProjectSettings

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
    scalaVersion
  ).map(fromRoot(_))

  private def baseProjectSettings =
    (List(
      apiURL,
      developers,
      homepage,
      licenses,
      organization,
      startYear,
      version
    ).map(fromRoot(_)) :+ pomIncludeRepositorySetting) ++ baseBuildSettings

  private def fromRoot[A](key: SettingKey[A]): Setting[A] =
    key := ((LocalRootProject / key)).value

  private type SettingPair[A] = (SettingKey[A], A)

  private def circularReferenceDefaults(defaults: SettingPair[_]*) = {
    def circ[A](pair: SettingPair[A]) =
      pair._1 := Option((LocalRootProject / pair._1).value).getOrElse(pair._2)
    defaults.map(kv => circ(kv))
  }

}
