inThisBuild(
  List(
    organization := "africa.shuwari.sbt",
    organizationName := "Shuwari Africa Ltd.",
    licenses := List(
      "Apache-2.0" -> url("https://www.apache.org/licenses/LICENSE-2.0")
    ),
    description := "Collection of sbt plugins for easy initialisation of uniform organisation wide default project settings.",
    version := defaultVersionSetting.value,
    dynver := defaultVersionSetting.toTaskable.toTask.value,
    scmInfo := Some(
      ScmInfo(
        url("https://github.com/unganisha/sbt-shuwari"),
        "scm:git@github.com:unganisha/sbt-shuwari.git"
      )
    ),
    scalacOptions ++= List("-feature", "-deprecation"),
    startYear := Some(2022)
  )
)

lazy val `sbt-shuwari-mode` =
  project
    .in(modules("mode"))
    .enablePlugins(SbtPlugin, SignedAetherPlugin)
    .settings(publishSettings)

lazy val `sbt-shuwari-header` =
  project
    .in(modules("header"))
    .enablePlugins(SbtPlugin, SignedAetherPlugin)
    .dependsOn(`sbt-shuwari-mode`)
    .settings(addSbtPlugin("de.heikoseeberger" % "sbt-header" % "5.6.5"))
    .settings(publishSettings)

lazy val `sbt-shuwari-scalac` =
  project
    .in(modules("scalac"))
    .enablePlugins(SbtPlugin, SignedAetherPlugin)
    .dependsOn(`sbt-shuwari-mode`)
    .settings(
      addSbtPlugin("io.github.davidgregory084" % "sbt-tpolecat" % "0.4.1")
    )
    .settings(publishSettings)

lazy val `sbt-shuwari-core` =
  project
    .in(modules("core"))
    .enablePlugins(SbtPlugin, SignedAetherPlugin)
    .settings(publishSettings)

lazy val `sbt-shuwari` =
  project
    .in(file(".sbt-shuwari"))
    .dependsOn(`sbt-shuwari-core`, `sbt-shuwari-scalac`, `sbt-shuwari-header`)
    .enablePlugins(SbtPlugin, SignedAetherPlugin)
    .settings(publishSettings)

lazy val `sbt-shuwari-build-root` =
  project
    .in(file("."))
    .aggregate(
      `sbt-shuwari-mode`,
      `sbt-shuwari-header`,
      `sbt-shuwari-scalac`,
      `sbt-shuwari-core`,
      `sbt-shuwari`
    )
    .enablePlugins(SbtPlugin)
    .settings(aetherSettings)
    .settings(
      publish := {},
      publish / skip := true,
      aether.AetherKeys.aetherDeploy := {}
    )

def modules(name: String) = file(s"./modules/$name")

def publishSettings = List(
  packageOptions += Package.ManifestAttributes(
    "Created-By" -> "Simple Build Tool",
    "Built-By" -> System.getProperty("user.name"),
    "Build-Jdk" -> System.getProperty("java.version"),
    "Specification-Title" -> name.value,
    "Specification-Version" -> version.value,
    "Specification-Vendor" -> organizationName.value,
    "Implementation-Title" -> name.value,
    "Implementation-Version" -> version.value,
    "Implementation-Vendor-Id" -> organization.value,
    "Implementation-Vendor" -> organizationName.value
  ),
  credentials := List(
    Credentials(
      "",
      "s01.oss.sonatype.org",
      System.getenv("PUBLISH_USER"),
      System.getenv("PUBLISH_USER_PASSPHRASE")
    )
  ),
  publishTo := Some("OSSRH" at {
    if (isSnapshot.value)
      "https://s01.oss.sonatype.org/content/repositories/snapshots"
    else "https://s01.oss.sonatype.org/service/local/staging/deploy/maven2"
  }),
  developers := List(
    Developer(
      id = "shuwaridev",
      name = "Shuwari Developer Team",
      email = "dev at shuwari com",
      url = url("https://shuwari.com/dev")
    )
  ),
  pomIncludeRepository := (_ => false)
) ++ pgpSettings ++ aetherSettings

def pgpSettings = List(
  PgpKeys.pgpSelectPassphrase :=
    sys.props
      .get("SIGNING_KEY_PASSPHRASE")
      .map(_.toCharArray),
  usePgpKeyHex(System.getenv("SIGNING_KEY_ID"))
)

def aetherSettings = List(
  aether.AetherKeys.aetherDeploy / version :=
    version.value
      .split("\\+")
      .head
)

def defaultVersionSetting = {
  def versionFormatter(in: sbtdynver.GitDescribeOutput): String =
    if (!in.isSnapshot()) in.ref.dropPrefix
    else {
      val parts = {
        def current = in.ref.dropPrefix.split("\\.").map(_.toInt)
        current.updated(current.length - 1, current.last + 1)
      }
      s"${parts.mkString(".")}-SNAPSHOT+${in.commitSuffix.distance}.${in.commitSuffix.sha}"
    }

  Def.setting(
    dynverGitDescribeOutput.value.mkVersion(
      versionFormatter,
      "SNAPHOT"
    )
  )
}
