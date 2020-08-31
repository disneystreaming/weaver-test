// shadow sbt-scalajs' crossProject and CrossType from Scala.js 0.6.x
import sbtcrossproject.CrossPlugin.autoImport.{ crossProject, CrossType }

addCommandAlias(
  "ci",
  Seq(
    "project root",
    "versionDump",
    "scalafmtCheckAll",
    "scalafix --check",
    "test:scalafix --check",
    "+clean",
    "+test:compile",
    "+test",
    "docs/docusaurusCreateSite"
  ).mkString(";", ";", "")
)

addCommandAlias(
  "fix",
  Seq(
    "root/scalafmtAll",
    "root/scalafmtSbt",
    "root/scalafix",
    "root/test:scalafix"
  ).mkString(";", ";", "")
)

addCommandAlias(
  "release",
  Seq(
    "project root",
    "+publishSigned",
    "sonatypeBundleRelease"
  ).mkString(";", ";", "")
)

ThisBuild / scalaVersion := WeaverPlugin.scala213

ThisBuild / scalafixDependencies += "com.github.liancheng" %% "organize-imports" % "0.4.0"

fork in Test := true

lazy val root = project
  .in(file("."))
  .enablePlugins(ScalafixPlugin)
  .aggregate(coreJVM,
             frameworkJVM,
             scalacheckJVM,
             zioJVM,
             specs2JVM,
             codecsJVM,
             cliJVM,
             coreJS,
             frameworkJS,
             scalacheckJS,
             zioJS,
             specs2JS)
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
      "co.fs2"               %%% "fs2-core"               % "2.4.4",
      "org.typelevel"        %%% "cats-effect"            % "2.1.4",
      "com.eed3si9n.expecty" %%% "expecty"                % "0.13.0",
      "org.portable-scala"   %%% "portable-scala-reflect" % "1.0.0"
    )
  )
  .jvmSettings(
    libraryDependencies ++= Seq(
      "org.scala-js" %%% "scalajs-stubs" % "1.0.0" % "provided"
    )
  )
  .jsSettings(
    libraryDependencies ++= Seq(
      "io.github.cquiroz" %%% "scala-java-time" % "2.0.0"
    )
  )

lazy val coreJVM = core.jvm
lazy val coreJS  = core.js

lazy val docs = project
  .in(file("modules/docs"))
  .enablePlugins(DocusaurusPlugin, MdocPlugin)
  .dependsOn(coreJVM, frameworkJVM, scalacheckJVM, zioJVM, specs2JVM)
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
      "org.scalacheck" %%% "scalacheck" % "1.14.3"
    ),
    scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.CommonJSModule) }
  )

lazy val scalacheckJVM = scalacheck.jvm
lazy val scalacheckJS  = scalacheck.js

lazy val specs2 = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("modules/specs2"))
  .dependsOn(core, framework % "test->compile")
  .configure(WeaverPlugin.profile)
  .settings(
    name := "specs2",
    libraryDependencies ++= Seq(
      "org.specs2" %%% "specs2-matcher" % "4.10.3"
    ),
    scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.CommonJSModule) }
  )
  .settings(WeaverPlugin.simpleLayout)

lazy val specs2JVM = specs2.jvm
lazy val specs2JS  = specs2.js

lazy val zio = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("modules/zio"))
  .dependsOn(core, framework % "test->compile")
  .configure(WeaverPlugin.profile)
  .settings(WeaverPlugin.simpleLayout)
  .settings(
    libraryDependencies ++= Seq(
      "dev.zio" %%% "zio-interop-cats" % "2.1.4.0"
    ),
    scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.CommonJSModule) }
  )

lazy val zioJVM = zio.jvm
lazy val zioJS  = zio.js

lazy val cli = crossProject(JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("modules/cli"))
  .dependsOn(core, framework, codecs, framework % "test->compile")
  .configure(WeaverPlugin.profile)
  .settings(WeaverPlugin.simpleLayout)
  .settings(
    libraryDependencies ++= Seq(
      "com.monovore"       %%% "decline-effect"         % "1.0.0",
      "org.portable-scala" %%% "portable-scala-reflect" % "1.0.0",
      "io.circe"           %%% "circe-parser"           % "0.13.0" % Test
    ),
    scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.CommonJSModule) }
  )

lazy val cliJVM = cli.jvm

// Json codecs for TestOutcome
lazy val codecs = crossProject(JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("modules/codecs"))
  .dependsOn(core, framework % "test->compile")
  .configure(WeaverPlugin.profile)
  .settings(WeaverPlugin.simpleLayout)
  .settings(
    name := "codecs",
    libraryDependencies ++= Seq(
      "io.circe" %%% "circe-core"   % "0.13.0",
      "io.circe" %%% "circe-parser" % "0.13.0" % Test
    ),
    scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.CommonJSModule) }
  )

lazy val codecsJVM = codecs.jvm

lazy val versionDump =
  taskKey[Unit]("Dumps the version in a file named version")

versionDump := {
  val file = (ThisBuild / baseDirectory).value / "version"
  IO.write(file, (Compile / version).value)
}
