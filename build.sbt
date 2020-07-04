// shadow sbt-scalajs' crossProject and CrossType from Scala.js 0.6.x
import sbtcrossproject.CrossPlugin.autoImport.{ crossProject, CrossType }

addCommandAlias(
  "ci",
  ";project root ;versionDump; scalafmtCheckAll; scalafix --check; test:scalafix --check ;+clean ;+test:compile ;+test; docs/docusaurusCreateSite")

addCommandAlias("release",
                ";project root ; +publishSigned; sonatypeBundleRelease")

ThisBuild / scalaVersion := WeaverPlugin.scala213

ThisBuild / scalafixDependencies += "com.github.liancheng" %% "organize-imports" % "0.3.1-RC3"

lazy val root = project
  .in(file("."))
  .enablePlugins(ScalafixPlugin)
  .aggregate(coreJVM,
             frameworkJVM,
             scalacheckJVM,
             zioJVM,
             coreJS,
             frameworkJS,
             scalacheckJS,
             zioJS)
  .configure(WeaverPlugin.profile)
  .settings(WeaverPlugin.doNotPublishArtifact)
  .settings(
    // Try really hard to not execute tasks in parallel
    Global / concurrentRestrictions := Tags.limitAll(1) :: Nil
  )

lazy val core = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("modules/core"))
  .configure(WeaverPlugin.profile)
  .settings(WeaverPlugin.simpleLayout)
  .settings(
    libraryDependencies ++= Seq(
      "co.fs2"               %%% "fs2-core"               % "2.4.2",
      "org.typelevel"        %%% "cats-effect"            % "2.1.3",
      "com.eed3si9n.expecty" %%% "expecty"                % "0.13.0",
      "org.portable-scala"   %%% "portable-scala-reflect" % "1.0.0"
    )
  )
  .jvmSettings(
    libraryDependencies ++= Seq(
      "org.scala-js" %%% "scalajs-stubs" % "1.0.0" % "provided"
    )
  )

lazy val coreJVM = core.jvm
lazy val coreJS  = core.js

lazy val docs = project
  .in(file("modules/docs"))
  .enablePlugins(DocusaurusPlugin, MdocPlugin)
  .dependsOn(coreJVM, frameworkJVM, scalacheckJVM, zioJVM)
  .settings(
    moduleName := "docs",
    watchSources += (ThisBuild / baseDirectory).value / "docs",
    mdocVariables := Map(
      "VERSION" -> version.value
    ),
    libraryDependencies ++= Seq(
      "org.http4s" %% "http4s-dsl"          % "0.21.0",
      "org.http4s" %% "http4s-blaze-server" % "0.21.0",
      "org.http4s" %% "http4s-blaze-client" % "0.21.0"
    )
  )

lazy val framework = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("modules/framework"))
  .dependsOn(core)
  .configure(WeaverPlugin.profile)
  .settings(WeaverPlugin.simpleLayout)
  .settings(
    libraryDependencies ++= Seq(
      "io.github.cquiroz" %%% "scala-java-time"      % "2.0.0" % Test,
      "io.github.cquiroz" %%% "scala-java-time-tzdb" % "2.0.0" % Test
    ),
    Test / scalacOptions ~= (_ filterNot (_ == "-Xfatal-warnings")),
    scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.CommonJSModule) }
  )
  .jvmSettings(
    libraryDependencies ++= Seq(
      "org.scala-sbt"  % "test-interface" % "1.0",
      "org.scala-js" %%% "scalajs-stubs"  % "1.0.0" % "provided"
    )
  )
  .jsSettings(
    libraryDependencies ++= Seq(
      "org.scala-js" %% "scalajs-test-interface" % scalaJSVersion
    )
  )

lazy val frameworkJVM = framework.jvm
lazy val frameworkJS  = framework.js

lazy val scalacheck = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("modules/scalacheck"))
  .dependsOn(core, framework % "test->compile")
  .configure(WeaverPlugin.profile)
  .settings(WeaverPlugin.simpleLayout)
  .settings(
    libraryDependencies ++= Seq(
      "org.scalacheck"    %%% "scalacheck"      % "1.14.3",
      "io.github.cquiroz" %%% "scala-java-time" % "2.0.0" % Test
    ),
    scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.CommonJSModule) }
  )

lazy val scalacheckJVM = scalacheck.jvm
lazy val scalacheckJS  = scalacheck.js

lazy val zio = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("modules/zio"))
  .dependsOn(core, framework % "test->compile")
  .configure(WeaverPlugin.profile)
  .settings(WeaverPlugin.simpleLayout)
  .settings(
    libraryDependencies ++= Seq(
      "dev.zio"           %%% "zio-interop-cats" % "2.1.3.0-RC16",
      "io.github.cquiroz" %%% "scala-java-time"  % "2.0.0"
    ),
    scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.CommonJSModule) }
  )

lazy val zioJVM = zio.jvm
lazy val zioJS  = zio.js

lazy val versionDump =
  taskKey[Unit]("Dumps the version in a file named version")

versionDump := {
  val file = (ThisBuild / baseDirectory).value / "version"
  IO.write(file, (Compile / version).value)
}
