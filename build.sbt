// shadow sbt-scalajs' crossProject and CrossType from Scala.js 0.6.x
import sbtcrossproject.CrossPlugin.autoImport.{ crossProject, CrossType }

addCommandAlias("ci", ";project root ;versionDump ;+clean ;+test:compile ;+test")

lazy val root = project
  .in(file("."))
  .aggregate(coreJVM, frameworkJVM)
  .configure(WeaverPlugin.profile)
  .settings(WeaverPlugin.doNotPublishArtifact)
  .settings(
    // Try really hard to not execute tasks in parallel
    Global / concurrentRestrictions := Tags.limitAll(1) :: Nil
  )

lazy val core = crossProject(JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("modules/core"))
  .configure(WeaverPlugin.profile)
  .settings(
    libraryDependencies ++= Seq(
      "co.fs2"               %%% "fs2-core"      % "2.1.0",
      "com.eed3si9n.expecty" %%% "expecty"       % "0.13.0",
      "org.scala-js"         %%% "scalajs-stubs" % scalaJSVersion % "provided"
    )
  )

lazy val coreJVM = core.jvm
// lazy val coreJS  = core.js

lazy val framework = crossProject(JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("modules/framework"))
  .dependsOn(core)
  .configure(WeaverPlugin.profile)
  .settings(
    libraryDependencies ++= Seq(
      "org.scala-js"  %%% "scalajs-stubs" % scalaJSVersion % "provided",
      "org.scala-sbt" % "test-interface" % "1.0"
    ),
    scalacOptions in Test ~= (_ filterNot (_ == "-Xfatal-warnings"))
  )

lazy val frameworkJVM = framework.jvm
// lazy val frameworkJS  = framework.js

lazy val scalacheck = crossProject(JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("modules/scalacheck"))
  .dependsOn(core, framework % "test->compile")
  .configure(WeaverPlugin.profile)
  .settings(
    libraryDependencies ++= Seq(
      "org.scalacheck" %%% "scalacheck" % "1.14.2"
    )
  )

lazy val zio = crossProject(JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("modules/zio"))
  .dependsOn(core, framework % "test->compile")
  .configure(WeaverPlugin.profile)
  .settings(
    libraryDependencies ++= Seq(
      "dev.zio" %%% "zio-interop-cats" % "2.0.0.0-RC10"
    )
  )

lazy val zioJVM = zio.jvm
// lazy val scalacheckJS  = scalacheck.js

lazy val versionDump =
  taskKey[Unit]("Dumps the version in a file named version")

versionDump := {
  val file = (baseDirectory in ThisBuild).value / "version"
  IO.write(file, (version in (Compile)).value)
}
