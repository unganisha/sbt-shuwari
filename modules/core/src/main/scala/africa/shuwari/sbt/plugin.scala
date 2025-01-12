package africa.shuwari.sbt

import sbt.Keys.*
import sbt.*
import sbt.plugins.JvmPlugin

import scala.language.implicitConversions

object ShuwariCorePlugin extends AutoPlugin {

  override def requires: Plugins = JvmPlugin

  override def trigger: PluginTrigger = allRequirements

  override def buildSettings: Seq[Setting[?]] = commonSettings.toList ++
    circularReferenceDefaults(
      scmInfo -> None
    )

  override def projectSettings: Seq[Setting[?]] =
    baseProjectSettings.toList

  object autoImport {

    implicit def projectToProjectOps(p: Project): ProjectOps = ProjectOps(p)
    implicit def optsToOptsOps(o: Opts.type): OptsOps = OptsOps(o)
    implicit def wrapLibraryDependency(v: ModuleID): Def.Initialize[ModuleID] = Def.setting(v)

    def shuwariProject: List[Setting[?]] = defaultOrganisationSettings
    def notPublished: List[Setting[?]] = projectNotPublished

    def dependsOn(libraries: Def.Initialize[ModuleID]*): List[Setting[Seq[ModuleID]]] =
      libraries.toList.map(Keys.libraryDependencies += _.value)

  }

  final case class OptsOps(o: Opts.type) {
    object scm {
      def github(owner: String, repository: String): Some[ScmInfo] = {
        def browseUrl = s"https://github.com/$owner/$repository"
        Some(
          ScmInfo(
            url(browseUrl),
            s"scm:git:$browseUrl.git",
            Some(s"scm:git:git@github.com:$owner/$repository.git")
          )
        )
      }

      def azure(owner: String, project: String, repository: String): Some[ScmInfo] = Some(
        ScmInfo(
          url(s"https://dev.azure.com/$owner/$project/_git/$repository"),
          s"scm:git:https://$owner@dev.azure.com/$owner/$project/_git/$repository"
        )
      )
    }

  }

  final case class ProjectOps(p: Project) extends AnyVal {
    def shuwariProject: Project = p.settings(defaultOrganisationSettings)
    def notPublished: Project = p.settings(projectNotPublished)
    def dependsOn(l: Def.Initialize[ModuleID]*): Project = p.settings(autoImport.dependsOn(l*))
  }

  private def projectNotPublished: List[Setting[?]] = List(
    publish / skip := true,
    publish := {},
    publishLocal := {},
    publishArtifact := false
  )

  private def defaultOrganisationSettings: List[Setting[?]] = List(
    organizationName := "Shuwari Africa Ltd.",
    organizationHomepage := Some(url("https://shuwari.africa")),
    developers := List(
      Developer(
        "shuwari-dev",
        "Shuwari Africa Ltd. Developer Team",
        "developers at shuwari dot africa",
        url("https://shuwari.africa")
      )
    ),
    versionScheme := Some("semver-spec")
  )

  private def pomIncludeRepositorySetting = pomIncludeRepository := (_ ⇒ false)

  private def commonKeys: Set[SettingKey[?]] = Set(
    organizationHomepage,
    organizationName,
    versionScheme,
    licenses
  )

  private def projectKeys: Set[SettingKey[?]] = commonKeys ++
    Set(
      apiURL,
      developers,
      homepage,
      licenses,
      organization,
      startYear,
      version
    )

  private def commonSettings = commonKeys.map(fromRoot(_))

  private def baseProjectSettings =
    projectKeys.map(fromRoot(_)) + pomIncludeRepositorySetting

  @inline private[sbt] def fromRoot[A](key: SettingKey[A]): Setting[A] =
    key := (LocalRootProject / key).value

  private type SettingPair[A] = (SettingKey[A], A)

  @inline private[sbt] def circularReferenceDefaults(defaults: SettingPair[?]*) = {
    def circ[A](pair: SettingPair[A]) =
      pair._1 := Option((LocalRootProject / pair._1).value).getOrElse(pair._2)
    defaults.map(kv => circ(kv))
  }

}
