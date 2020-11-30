import _root_.sbtcrossproject.Platform
// shadow sbt-scalajs' crossProject and CrossType from Scala.js 0.6.x

import org.jetbrains.sbtidea.Keys.createRunnerProject
import sbtcrossproject.CrossPlugin.autoImport.{ CrossType, crossProject }

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
    "docs/docusaurusCreateSite",
    "coreJVM/publishLocal",
    "intellij/updateIntellij",
    "intellij/test"
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

// See https://github.com/JetBrains/sbt-idea-plugin/issues/76 for
// why this contrived sequence of actions exists ...
addCommandAlias(
  "packageIntellijPlugin",
  Seq(
    "project root",
    "intellij/packageArtifact",
    "intellij/doPatchPluginXml",
    "intellij/packageArtifactZip"
  ).mkString(";", ";", "")
)

ThisBuild / scalaVersion := WeaverPlugin.scala213

ThisBuild / scalafixDependencies += "com.github.liancheng" %% "organize-imports" % "0.4.4"

Global / (fork in Test) := true

lazy val root = project
  .in(file("."))
  .enablePlugins(ScalafixPlugin)
  .aggregate(allModules: _*)
  .configure(WeaverPlugin.profile)
  .settings(WeaverPlugin.doNotPublishArtifact)

lazy val allModules = Seq[ProjectReference](
  coreJVM,
  coreJS,
  frameworkJVM,
  frameworkJS,
  scalacheckJVM,
  scalacheckJS,
  specs2JVM,
  specs2JS,
  intellijRunnerJVM) ++ effectCores ++ effectFrameworks

lazy val core = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("modules/core"))
  .configure(WeaverPlugin.profile)
  .settings(WeaverPlugin.simpleLayout)
  .settings(
    libraryDependencies ++= Seq(
      "co.fs2"               %%% "fs2-core"               % "2.4.5",
      "org.typelevel"        %%% "cats-effect"            % "2.3.0",
      "com.eed3si9n.expecty" %%% "expecty"                % "0.14.1",
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
  .dependsOn(coreJVM,
             frameworkJVM,
             scalacheckJVM,
             zioJVM,
             monixJVM,
             monixBioJVM,
             specs2JVM)
  .settings(
    moduleName := "docs",
    watchSources += (ThisBuild / baseDirectory).value / "docs",
    mdocVariables := Map(
      "VERSION" -> version.value
    ),
    libraryDependencies ++= Seq(
      "org.http4s"  %% "http4s-dsl"          % "0.21.0",
      "org.http4s"  %% "http4s-blaze-server" % "0.21.0",
      "org.http4s"  %% "http4s-blaze-client" % "0.21.0",
      "com.lihaoyi" %% "fansi"               % "0.2.7"
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
    fork in Test := false
  )
  .jvmSettings(
    libraryDependencies ++= Seq(
      "org.scala-sbt"  % "test-interface" % "1.0",
      "org.scala-js" %%% "scalajs-stubs"  % "1.0.0" % "provided"
    )
  )
  .jsSettings(jsLinker)
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
  .dependsOn(core, cats % "test->compile")
  .configure(WeaverPlugin.profile)
  .settings(WeaverPlugin.simpleLayout)
  .jsSettings(jsLinker)
  .settings(
    testFrameworks := Seq(new TestFramework("weaver.framework.CatsEffect")),
    libraryDependencies ++= Seq(
      "org.scalacheck" %%% "scalacheck" % "1.15.1"
    )
  )

lazy val scalacheckJVM = scalacheck.jvm
lazy val scalacheckJS  = scalacheck.js

lazy val specs2 = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("modules/specs2"))
  .dependsOn(core, cats % "test->compile")
  .configure(WeaverPlugin.profile)
  .jsSettings(jsLinker)
  .settings(
    name := "specs2",
    libraryDependencies ++= Seq(
      "org.specs2" %%% "specs2-matcher" % "4.10.5"
    )
  )
  .settings(WeaverPlugin.simpleLayout)

lazy val specs2JVM = specs2.jvm
lazy val specs2JS  = specs2.js

// #################################################################################################
// Effect-specific cores
// #################################################################################################

lazy val effectCores: Seq[ProjectReference] = Seq(
  coreCatsJVM,
  coreCatsJS,
  coreMonixJVM,
  coreMonixJS,
  coreMonixBioJVM,
  coreMonixJS,
  coreZioJVM,
  coreZioJS
)

lazy val coreCats = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("modules/core/cats"))
  .dependsOn(core)
  .configure(WeaverPlugin.profile)
  .settings(WeaverPlugin.simpleLayout)
  .settings(name := "weaver-cats-core")

lazy val coreCatsJVM = coreCats.jvm
lazy val coreCatsJS  = coreCats.js

lazy val coreMonix = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("modules/core/monix"))
  .dependsOn(core)
  .configure(WeaverPlugin.profile)
  .settings(WeaverPlugin.simpleLayout)
  .settings(
    name := "weaver-monix-core",
    libraryDependencies ++= Seq(
      "io.monix" %%% "monix" % "3.3.0"
    )
  )

lazy val coreMonixJVM = coreMonix.jvm
lazy val coreMonixJS  = coreMonix.js

lazy val coreMonixBio = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("modules/core/monixBio"))
  .dependsOn(core)
  .configure(WeaverPlugin.profile)
  .settings(WeaverPlugin.simpleLayout)
  .settings(
    name := "weaver-monix-bio-core",
    libraryDependencies ++= Seq(
      "io.monix" %%% "monix-bio" % "1.1.0"
    )
  )

lazy val coreMonixBioJVM = coreMonixBio.jvm
lazy val coreMonixBioJS  = coreMonixBio.js

lazy val coreZio = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("modules/core/zio"))
  .dependsOn(core)
  .configure(WeaverPlugin.profile)
  .settings(WeaverPlugin.simpleLayout)
  .settings(
    name := "weaver-zio-core",
    libraryDependencies ++= Seq(
      "dev.zio" %%% "zio-interop-cats" % "2.2.0.1"
    )
  )

lazy val coreZioJVM = coreZio.jvm
lazy val coreZioJS  = coreZio.js

// #################################################################################################
// Effect-specific frameworks
// #################################################################################################

lazy val effectFrameworks: Seq[ProjectReference] = Seq(
  catsJVM,
  catsJS,
  monixJVM,
  monixJS,
  monixBioJVM,
  monixJS,
  zioJVM,
  zioJS
)

lazy val cats = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("modules/framework/cats"))
  .dependsOn(framework, coreCats)
  .configure(WeaverPlugin.profile)
  .settings(WeaverPlugin.simpleLayout)
  .jsSettings(jsLinker)
  .settings(
    name := "weaver-cats",
    testFrameworks := Seq(new TestFramework("weaver.framework.CatsEffect"))
  )

lazy val catsJVM = cats.jvm
lazy val catsJS  = cats.js

lazy val monix = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("modules/framework/monix"))
  .dependsOn(framework, coreMonix)
  .configure(WeaverPlugin.profile)
  .settings(WeaverPlugin.simpleLayout)
  .jsSettings(jsLinker)
  .settings(
    name := "weaver-monix",
    testFrameworks := Seq(new TestFramework("weaver.framework.Monix"))
  )

lazy val monixJVM = monix.jvm
lazy val monixJS  = monix.js

lazy val monixBio = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("modules/framework/monix-bio"))
  .dependsOn(framework, coreMonixBio)
  .configure(WeaverPlugin.profile)
  .settings(WeaverPlugin.simpleLayout)
  .jsSettings(jsLinker)
  .settings(
    name := "weaver-monix-bio",
    testFrameworks := Seq(new TestFramework("weaver.framework.MonixBIO"))
  )

lazy val monixBioJVM = monixBio.jvm
lazy val monixBioJS  = monixBio.js

lazy val zio = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("modules/framework/zio"))
  .dependsOn(framework, coreZio)
  .configure(WeaverPlugin.profile)
  .settings(WeaverPlugin.simpleLayout)
  .jsSettings(jsLinker)
  .settings(
    name := "weaver-zio",
    testFrameworks := Seq(new TestFramework("weaver.framework.ZIO"))
  )

lazy val zioJVM = zio.jvm
lazy val zioJS  = zio.js

// #################################################################################################
// Intellij
// #################################################################################################

lazy val intellijRunner = crossProject(JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("modules/intellij-runner"))
  .dependsOn(core, framework, framework % "test->compile")
  .configure(WeaverPlugin.profile)
  .settings(WeaverPlugin.simpleLayout)
  .settings(
    name := "intellij-runner"
  )

lazy val intellijRunnerJVM = intellijRunner.jvm

ThisBuild / intellijBuild := "202.6948.69"
ThisBuild / intellijPluginName := "weaver-intellij"

import org.jetbrains.sbtidea.Keys._
import org.jetbrains.sbtidea.SbtIdeaPlugin
import sbt.Keys._
import sbt._

// Prevents sbt-idea-plugin from automatically
// downloading intellij binaries on startup
ThisBuild / doProjectSetup := {}

lazy val intellij = (project in file("modules/intellij"))
  .enablePlugins(SbtIdeaPlugin, BuildInfoPlugin)
  .disablePlugins(WeaverPlugin)
  .settings(
    scalaVersion := "2.12.10",
    intellijPlugins := Seq(
      "com.intellij.java".toPlugin,
      "org.intellij.scala:2020.2.23".toPlugin
    ),
    libraryDependencies ++= Seq(
      "io.get-coursier" %% "coursier"        % "2.0.0-RC6-24",
      "com.novocode"     % "junit-interface" % "0.11" % Test
    ),
    patchPluginXml := pluginXmlOptions { xml =>
      xml.version = version.value
    },
    // packageArtifact in publishPlugin := packagePlugin.value,
    packageMethod := PackagingMethod.Standalone(),
    scalacOptions ++= (WeaverPlugin.commonCompilerOptions ++ WeaverPlugin.compilerOptions2_12_Only),
    buildInfoKeys := Seq[BuildInfoKey](name, version),
    buildInfoPackage := "weaver.build",
    semanticdbEnabled := true,
    semanticdbVersion := scalafixSemanticdb.revision,
    packageArtifactZip := {
      val dump   = (ThisBuild / baseDirectory).value / "intellijPlugin"
      val result = packageArtifactZip.value
      IO.write(dump, result.getAbsolutePath)
      result
    }
  )

// #################################################################################################
// Misc
// #################################################################################################

lazy val versionDump =
  taskKey[Unit]("Dumps the version in a file named version")

versionDump := {
  val file = (ThisBuild / baseDirectory).value / "version"
  IO.write(file, (Compile / version).value)
}

lazy val jsLinker = Seq(
  scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.CommonJSModule) }
)
