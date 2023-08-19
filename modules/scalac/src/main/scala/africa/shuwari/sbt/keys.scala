package africa.shuwari.sbt

import sbt._

import org.typelevel.scalacoptions.ScalacOption

object ScalaOptionsKeys {

  val basePackages = settingKey[List[String]](
    "Base package name. Used to enable, and limit inlining to the specified pattern."
  )

//  val developmentOptions = taskKey[Set[ScalacOption]](
//    "Defines Scala compiler options used during Development build mode builds."
//  )
//  val integrationOptions = taskKey[Set[ScalacOption]](
//    "Defines Scala compiler options used during Integration build mode builds."
//  )
//  val releaseOptions = taskKey[Set[ScalacOption]](
//    "Defines Scala compiler options used during Release build mode  builds."
//  )

}
