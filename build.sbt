import WeaverPlugin._

ThisBuild / commands += Command.command("ci") { state =>
  "versionDump" ::
    "scalafmtCheckAll" ::
    "scalafix --check" ::
    "test:scalafix --check" ::
    "clean" ::
    "test:compile" ::
    "test" ::
    "docs/docusaurusCreateSite" ::
    "core/publishLocal" :: state
}

ThisBuild / commands += Command.command("fix") { state =>
  "scalafmtAll" ::
    "scalafmtSbt" ::
    "scalafix" ::
    "test:scalafix" :: state
}

ThisBuild / commands += Command.command("release") { state =>
  "publishSigned" ::
    "sonatypeBundleRelease" :: state
}

ThisBuild / scalaVersion := WeaverPlugin.scala213

ThisBuild / scalafixDependencies += "com.github.liancheng" %% "organize-imports" % "0.4.4"

Global / (Test / fork) := true
Global / (Test / testOptions) += Tests.Argument("--quickstart")

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
  intellijRunner.projectRefs,
  effectCores,
  effectFrameworks
).flatten

lazy val catsEffect3Version = "3.0.0-M5"

def catsEffectDependencies(proj: Project): Project = {
  proj.settings(
    libraryDependencies ++= {
      if (virtualAxes.value.contains(CatsEffect2Axis))
        Seq(
          "co.fs2"        %%% "fs2-core"    % "2.5.0",
          "org.typelevel" %%% "cats-effect" % "2.3.1"
        )
      else
        Seq(
          "co.fs2"        %%% "fs2-core"    % "3.0.0-M7",
          "org.typelevel" %%% "cats-effect" % catsEffect3Version
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
      "com.eed3si9n.expecty" %%% "expecty"                % "0.14.1-SNAPSHOT",
      ("org.portable-scala"  %%% "portable-scala-reflect" % "1.0.0").withDottyCompat(
        scalaVersion.value)
    ),
    libraryDependencies ++= {
      if (virtualAxes.value.contains(VirtualAxis.jvm))
        Seq(
          ("org.scala-js" %%% "scalajs-stubs" % "1.0.0" % "provided").withDottyCompat(
            scalaVersion.value)
        )
      else {
        Seq(
          ("io.github.cquiroz" %%% "scala-java-time" % "2.0.0").withDottyCompat(
            scalaVersion.value)
        )
      }
    }
  )

lazy val projectsWithAxes = Def.task {
  (name.value, virtualAxes.value, version.value)
}

val allEffectCoresFilter: ScopeFilter =
  ScopeFilter(
    inProjects(effectFrameworks: _*),
    inConfigurations(Compile)
  )

val allIntegrationsCoresFilter: ScopeFilter =
  ScopeFilter(
    inProjects((scalacheck.projectRefs ++ specs2.projectRefs): _*),
    inConfigurations(Compile)
  )

lazy val docs = projectMatrix
  .in(file("modules/docs"))
  .jvmPlatform(WeaverPlugin.suppertedScala2Versions)
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
    ),
    sourceGenerators in Compile += Def.taskDyn {
      val filePath =
        sourceManaged.in(Compile).value / "BuildMatrix.scala"

      def q(s: String) = '"' + s + '"'

      def process(f: Iterable[(String, Seq[VirtualAxis], String)]) = f.map {
        case (name, axes, ver) =>
          val isJVM = axes.contains(VirtualAxis.jvm)
          val isJS  = axes.contains(VirtualAxis.js)
          val scalaVersion = axes.collectFirst {
            case a: VirtualAxis.ScalaVersionAxis => a
          }.get.scalaVersion

          val CE = axes.collectFirst {
            case CatsEffect2Axis => "CE2"
            case CatsEffect3Axis => "CE3"
          }.get

          List(
            s"name = ${q(name)}",
            s"jvm = $isJVM",
            s"js = $isJS",
            s"scalaVersion = ${q(scalaVersion)}",
            s"catsEffect = $CE",
            s"version = ${q(ver)}"
          ).mkString("Artifact(", ",", ")")
      }.mkString("List(", ",\n", ")")

      val effects = process(projectsWithAxes.all(allEffectCoresFilter).value)
      val integrations =
        process(projectsWithAxes.all(allIntegrationsCoresFilter).value)

      IO.write(
        filePath,
        s"""
        | package weaver.docs
        |
        | object BuildMatrix {
        |    val catsEffect3Version = ${q(catsEffect3Version)}
        |    val effects = $effects
        |    val integrations = $integrations
        | }
        """.stripMargin
      )

      Def.task(Seq(filePath))
    }
  )

lazy val framework = projectMatrix
  .in(file("modules/framework"))
  .dependsOn(core)
  .crossCatsEffect
  .settings(
    libraryDependencies ++= {
      if (virtualAxes.value.contains(VirtualAxis.jvm))
        Seq(
          "org.scala-sbt"   % "test-interface" % "1.0",
          ("org.scala-js" %%% "scalajs-stubs"  % "1.0.0" % "provided").withDottyCompat(
            scalaVersion.value)
        )
      else
        Seq(
          ("org.scala-js" %% "scalajs-test-interface" % scalaJSVersion).withDottyCompat(
            scalaVersion.value),
          ("io.github.cquiroz" %%% "scala-java-time-tzdb" % "2.0.0" % Test).withDottyCompat(
            scalaVersion.value)
        )
    }
  )
  .configure(WeaverPlugin.profile)
  .settings(WeaverPlugin.simpleLayout)

lazy val scalacheck = projectMatrix
  .in(file("modules/scalacheck"))
  .crossCatsEffect
  .dependsOn(core, cats % "test->compile")
  .configure(WeaverPlugin.profile)
  .settings(WeaverPlugin.simpleLayout)
  .settings(
    testFrameworks := Seq(new TestFramework("weaver.framework.CatsEffect")),
    libraryDependencies ++= Seq(
      "org.scalacheck" %%% "scalacheck" % "1.15.2"
    )
  )

lazy val specs2 = projectMatrix
  .in(file("modules/specs2"))
  .crossCatsEffect
  .dependsOn(core, cats % "test->compile")
  .configure(WeaverPlugin.profile)
  .settings(
    name := "specs2",
    testFrameworks := Seq(new TestFramework("weaver.framework.CatsEffect")),
    libraryDependencies ++= Seq(
      ("org.specs2" %%% "specs2-matcher" % "4.10.5").withDottyCompat(
        scalaVersion.value)
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
  .crossCatsEffect
  .dependsOn(core)
  .configure(WeaverPlugin.profile)
  .settings(WeaverPlugin.simpleLayout)
  .settings(name := "cats-core")

lazy val coreMonix = projectMatrix
  .in(file("modules/core/monix"))
  .onlyCatsEffect2(onlyScala2 = true)
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
  .onlyCatsEffect2(onlyScala2 = true)
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
  .onlyCatsEffect2(onlyScala2 = true)
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
  .crossCatsEffect
  .configure(WeaverPlugin.profile)
  .settings(WeaverPlugin.simpleLayout)
  .settings(
    name := "cats",
    testFrameworks := Seq(new TestFramework("weaver.framework.CatsEffect")),
    libraryDependencies += {
      // if(virtualAxes.contains(VirtualAxis.js))
      ("io.github.cquiroz" %%% "scala-java-time-tzdb" % "2.0.0" % Test).withDottyCompat(
        scalaVersion.value)
    }
  )

lazy val monix = projectMatrix
  .in(file("modules/framework/monix"))
  .onlyCatsEffect2(onlyScala2 = true)
  .dependsOn(framework, coreMonix)
  .configure(WeaverPlugin.profile)
  .settings(WeaverPlugin.simpleLayout)
  .settings(
    name := "monix",
    testFrameworks := Seq(new TestFramework("weaver.framework.Monix"))
  )

lazy val monixBio = projectMatrix
  .in(file("modules/framework/monix-bio"))
  .onlyCatsEffect2(onlyScala2 = true)
  .dependsOn(framework, coreMonixBio)
  .configure(WeaverPlugin.profile)
  .settings(WeaverPlugin.simpleLayout)
  .settings(
    name := "monix-bio",
    testFrameworks := Seq(new TestFramework("weaver.framework.MonixBIO"))
  )

lazy val zio = projectMatrix
  .in(file("modules/framework/zio"))
  .onlyCatsEffect2(onlyScala2 = true)
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
  .onlyCatsEffect2(withJs = false, onlyScala2 = true)
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
