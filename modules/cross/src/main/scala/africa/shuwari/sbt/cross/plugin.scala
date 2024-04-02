package africa.shuwari.sbt.cross

import sbt.AutoPlugin
import sbt.Def
import sbt.librarymanagement.ModuleID
import sbtcrossproject.CrossProject

import africa.shuwari.sbt.ShuwariCorePlugin

import scala.language.implicitConversions

object ShuwariSbtCrossPlugin extends AutoPlugin {
  final case class CrossProjectOps(p: CrossProject) {
    def dependsOn(libraries: Def.Initialize[ModuleID]*): CrossProject =
      p.settings(ShuwariCorePlugin.autoImport.dependsOn(libraries*))
  }

  object autoImport {
    implicit def crossProjectOps(p: CrossProject): CrossProjectOps = CrossProjectOps(p)
  }
}
