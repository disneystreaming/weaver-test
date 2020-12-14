import WeaverPlugin._

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
    "coreJVM/publishLocal"
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

ThisBuild / scalafixDependencies += "com.github.liancheng" %% "organize-imports" % "0.4.4"

Global / (Test / fork) := true
Global / (Test / testOptions) += Tests.Argument("--quickstart")

Global / concurrentRestrictions += Tags.limit(Tags.Test, 4)

lazy val root = project
  .in(file("."))
  .enablePlugins(ScalafixPlugin)
  .aggregate(allModules: _*)
  .configure(WeaverPlugin.profile)
  .settings(WeaverPlugin.doNotPublishArtifact)

lazy val allModules = Seq(
  core.projectRefs,
  framework.projectRefs,
  scalacheck.projectRefs,
  specs2.projectRefs,
  intellijRunner.projectRefs).flatten ++ effectCores ++ effectFrameworks

def catsEffectDependencies(proj: Project): Project = {
  proj.settings(
    libraryDependencies ++= {
      if (virtualAxes.value.contains(CatsEffect2Axis))
        Seq(
          "co.fs2"        %%% "fs2-core"    % "2.4.6",
          "org.typelevel" %%% "cats-effect" % "2.3.0"
        )
      else
        Seq(
          "co.fs2"        %%% "fs2-core"    % "3.0.0-M6",
          "org.typelevel" %%% "cats-effect" % "3.0.0-M4"
        )
    }
  )
}

lazy val core = projectMatrix
  .in(file("modules/core"))
  .configure(WeaverPlugin.profile)
  .settings(WeaverPlugin.simpleLayout)
  .crossCatsEffect
  .configure(catsEffectDependencies)
  .settings(
    libraryDependencies ++= Seq(
      "com.eed3si9n.expecty" %%% "expecty"                % "0.14.1",
      "org.portable-scala"   %%% "portable-scala-reflect" % "1.0.0"
    ),
    libraryDependencies ++= {
      if (virtualAxes.value.contains(VirtualAxis.jvm))
        Seq(
          "org.scala-js" %%% "scalajs-stubs" % "1.0.0" % "provided"
        )
      else {
        Seq(
          "io.github.cquiroz" %%% "scala-java-time" % "2.0.0"
        )
      }
    }
  )

lazy val docs = projectMatrix
  .in(file("modules/docs"))
  .jvmPlatform(WeaverPlugin.supportedScalaVersions)
  .enablePlugins(DocusaurusPlugin, MdocPlugin)
  .dependsOn(core, scalacheck, cats, zio, monix, monixBio, specs2)
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

lazy val framework = projectMatrix
  .in(file("modules/framework"))
  .dependsOn(core)
  .crossCatsEffect
  .settings(
    libraryDependencies ++= {
      if (virtualAxes.value.contains(VirtualAxis.jvm))
        Seq(
          "org.scala-sbt"  % "test-interface" % "1.0",
          "org.scala-js" %%% "scalajs-stubs"  % "1.0.0" % "provided"
        )
      else
        Seq(
          "org.scala-js"       %% "scalajs-test-interface" % scalaJSVersion,
          "io.github.cquiroz" %%% "scala-java-time-tzdb"   % "2.0.0" % Test
        )
    }
  )
  .configure(WeaverPlugin.profile)
  .settings(WeaverPlugin.simpleLayout)

lazy val scalacheck = projectMatrix
  .in(file("modules/scalacheck"))
  .dependsOn(core, cats % "test->compile")
  .configure(WeaverPlugin.profile)
  .settings(WeaverPlugin.simpleLayout)
  .jvmPlatform(WeaverPlugin.supportedScalaVersions)
  .jsPlatform(WeaverPlugin.supportedScalaVersions, jsLinker)
  .settings(
    testFrameworks := Seq(new TestFramework("weaver.framework.CatsEffect")),
    libraryDependencies ++= Seq(
      "org.scalacheck" %%% "scalacheck" % "1.15.1"
    )
  )

lazy val specs2 = projectMatrix
  .in(file("modules/specs2"))
  .jvmPlatform(WeaverPlugin.supportedScalaVersions)
  .jsPlatform(WeaverPlugin.supportedScalaVersions, jsLinker)
  .dependsOn(core, cats % "test->compile")
  .configure(WeaverPlugin.profile)
  .settings(
    name := "specs2",
    libraryDependencies ++= Seq(
      "org.specs2" %%% "specs2-matcher" % "4.10.5"
    )
  )
  .settings(WeaverPlugin.simpleLayout)

// #################################################################################################
// Effect-specific cores
// #################################################################################################

lazy val effectCores: Seq[ProjectReference] =
  coreCats.projectRefs ++ coreMonix.projectRefs ++ coreZio.projectRefs ++ coreMonixBio.projectRefs

lazy val coreCats = projectMatrix
  .in(file("modules/core/cats"))
  .jvmPlatform(WeaverPlugin.supportedScalaVersions)
  .jsPlatform(WeaverPlugin.supportedScalaVersions, jsLinker)
  .crossCatsEffect
  .dependsOn(core)
  .configure(WeaverPlugin.profile)
  .settings(WeaverPlugin.simpleLayout)
  .settings(name := "cats-core")

lazy val coreMonix = projectMatrix
  .in(file("modules/core/monix"))
  .jvmPlatform(WeaverPlugin.supportedScalaVersions)
  .jsPlatform(WeaverPlugin.supportedScalaVersions, jsLinker)
  .dependsOn(core)
  .configure(WeaverPlugin.profile)
  .settings(WeaverPlugin.simpleLayout)
  .settings(
    name := "monix-core",
    libraryDependencies ++= Seq(
      "io.monix" %%% "monix" % "3.3.0"
    )
  )

lazy val coreMonixBio = projectMatrix
  .in(file("modules/core/monixBio"))
  .jvmPlatform(WeaverPlugin.supportedScalaVersions)
  .jsPlatform(WeaverPlugin.supportedScalaVersions, jsLinker)
  .dependsOn(core)
  .configure(WeaverPlugin.profile)
  .settings(WeaverPlugin.simpleLayout)
  .settings(
    name := "monix-bio-core",
    libraryDependencies ++= Seq(
      "io.monix" %%% "monix-bio" % "1.1.0"
    )
  )

lazy val coreZio = projectMatrix
  .in(file("modules/core/zio"))
  .jvmPlatform(WeaverPlugin.supportedScalaVersions)
  .jsPlatform(WeaverPlugin.supportedScalaVersions, jsLinker)
  .dependsOn(core)
  .configure(WeaverPlugin.profile)
  .settings(WeaverPlugin.simpleLayout)
  .settings(
    name := "zio-core",
    libraryDependencies ++= Seq(
      "dev.zio" %%% "zio-interop-cats" % "2.2.0.1"
    )
  )

// #################################################################################################
// Effect-specific frameworks
// #################################################################################################

lazy val effectFrameworks: Seq[ProjectReference] = Seq(
  cats.projectRefs,
  monix.projectRefs,
  monixBio.projectRefs,
  zio.projectRefs
).flatten

lazy val cats = projectMatrix
  .in(file("modules/framework/cats"))
  .dependsOn(framework, coreCats)
  .jvmPlatform(WeaverPlugin.supportedScalaVersions)
  .jsPlatform(WeaverPlugin.supportedScalaVersions, jsLinker)
  .crossCatsEffect
  .configure(WeaverPlugin.profile)
  .settings(WeaverPlugin.simpleLayout)
  .settings(
    name := "cats",
    testFrameworks := Seq(new TestFramework("weaver.framework.CatsEffect")),
    libraryDependencies += "io.github.cquiroz" %%% "scala-java-time-tzdb" % "2.0.0" % Test
  )

lazy val monix = projectMatrix
  .in(file("modules/framework/monix"))
  .jvmPlatform(WeaverPlugin.supportedScalaVersions)
  .jsPlatform(WeaverPlugin.supportedScalaVersions, jsLinker)
  .dependsOn(framework, coreMonix)
  .configure(WeaverPlugin.profile)
  .settings(WeaverPlugin.simpleLayout)
  .settings(
    name := "monix",
    testFrameworks := Seq(new TestFramework("weaver.framework.Monix"))
  )

lazy val monixBio = projectMatrix
  .in(file("modules/framework/monix-bio"))
  .jvmPlatform(WeaverPlugin.supportedScalaVersions)
  .jsPlatform(WeaverPlugin.supportedScalaVersions, jsLinker)
  .dependsOn(framework, coreMonixBio)
  .configure(WeaverPlugin.profile)
  .settings(WeaverPlugin.simpleLayout)
  .settings(
    name := "monix-bio",
    testFrameworks := Seq(new TestFramework("weaver.framework.MonixBIO"))
  )

lazy val zio = projectMatrix
  .in(file("modules/framework/zio"))
  .jvmPlatform(WeaverPlugin.supportedScalaVersions)
  .jsPlatform(WeaverPlugin.supportedScalaVersions, jsLinker)
  .dependsOn(framework, coreZio)
  .configure(WeaverPlugin.profile)
  .settings(WeaverPlugin.simpleLayout)
  .settings(
    name := "zio",
    testFrameworks := Seq(new TestFramework("weaver.framework.ZIO"))
  )

// #################################################################################################
// Intellij
// #################################################################################################

lazy val intellijRunner = projectMatrix
  .jvmPlatform(WeaverPlugin.supportedScalaVersions)
  .in(file("modules/intellij-runner"))
  .dependsOn(core, framework, framework % "test->compile")
  .configure(WeaverPlugin.profile)
  .settings(WeaverPlugin.simpleLayout)
  .settings(
    name := "intellij-runner"
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
  Test / scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.CommonJSModule) }
)
