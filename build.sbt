ThisBuild / organization := "africa.shuwari.sbt"
ThisBuild / organizationName := "Shuwari Africa Ltd."
ThisBuild / licenses := List(
  "Apache-2.0" -> url("https://www.apache.org/licenses/LICENSE-2.0")
)

ThisBuild / scalacOptions += "-feature"
ThisBuild / scalacOptions += "-deprecation"

lazy val `sbt-shuwari-mode` =
  project
    .in(modules("mode"))
    .enablePlugins(SbtPlugin)

lazy val `sbt-shuwari-header` =
  project
    .in(modules("header"))
    .enablePlugins(SbtPlugin)
    .dependsOn(`sbt-shuwari-mode`)
    .settings(addSbtPlugin("de.heikoseeberger" % "sbt-header" % "5.6.0"))

lazy val `sbt-shuwari-scalac` =
  project
    .in(modules("scalac"))
    .enablePlugins(SbtPlugin)
    .dependsOn(`sbt-shuwari-mode`)
    .settings(
      addSbtPlugin("io.github.davidgregory084" % "sbt-tpolecat" % "0.4.1")
    )

lazy val `sbt-shuwari-core` =
  project
    .in(modules("core"))
    .enablePlugins(SbtPlugin)

lazy val `sbt-shuwari` =
  project
    .in(modules(".core"))
    .dependsOn(`sbt-shuwari-core`, `sbt-shuwari-scalac`, `sbt-shuwari-header`)

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
    .settings(
      publish := {}
    )

def modules(name: String) = file(s"./modules/$name")
