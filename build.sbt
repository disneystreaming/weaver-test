import WeaverPlugin._

ThisBuild / commands += Command.command("ci") { state =>
  "versionDump" ::
    "scalafmtCheckAll" ::
    "scalafix --check" ::
    "Test/scalafix --check" ::
    "clean" ::
    "Test/compile" ::
    "Test/fastLinkJS" :: // do this separately as it's memory intensive
    "test" ::
    "docs/docusaurusCreateSite" ::
    "core/publishLocal" :: state
}

ThisBuild / commands += Command.command("fix") { state =>
  "scalafix" ::
    "Test/scalafix" ::
    "scalafmtAll" ::
    "scalafmtSbt" :: state
}

ThisBuild / commands += Command.command("release") { state =>
  "publishSigned" ::
    "sonatypeBundleRelease" :: state
}

ThisBuild / commands ++= createBuildCommands(allModules)

ThisBuild / scalaVersion := WeaverPlugin.scala213

ThisBuild / scalafixDependencies += "com.github.liancheng" %% "organize-imports" % "0.4.4"

Global / (Test / fork) := true
Global / (Test / testOptions) += Tests.Argument("--quickstart")

val Version = new {
  object CE3 {
    val fs2        = "3.1.3"
    val cats       = "3.2.9"
    val zioInterop = "3.1.1.0"
  }

  object CE2 {
    val fs2        = "2.5.9"
    val cats       = "2.5.3"
    val zioInterop = "2.5.1.0"
  }

  val expecty         = "0.15.4"
  val portableReflect = "1.1.1"
  val junit           = "4.13.2"
  val scalajsStubs    = "1.1.0"
  val specs2          = "4.12.12"
  val discipline      = "1.1.5"
  val catsLaws        = "2.6.1"
  val scalacheck      = "1.15.4"
  val monix           = "3.4.0"
  val monixBio        = "1.2.0"
  val testInterface   = "1.0"
  val scalaJavaTime   = "2.3.0"
}

lazy val root = project
  .in(file("."))
  .aggregate(allModules: _*)
  .settings(WeaverPlugin.doNotPublishArtifact)

lazy val allModules = Seq(
  core.projectRefs,
  framework.projectRefs,
  scalacheck.projectRefs,
  specs2.projectRefs,
  discipline.projectRefs,
  intellijRunner.projectRefs,
  effectCores,
  effectFrameworks
).flatten

def catsEffectDependencies(proj: Project): Project = {
  proj.settings(
    libraryDependencies ++= {
      if (virtualAxes.value.contains(CatsEffect2Axis))
        Seq(
          "co.fs2"        %%% "fs2-core"    % Version.CE2.fs2,
          "org.typelevel" %%% "cats-effect" % Version.CE2.cats
        )
      else
        Seq(
          "co.fs2"        %%% "fs2-core"    % Version.CE3.fs2,
          "org.typelevel" %%% "cats-effect" % Version.CE3.cats
        )
    }
  )
}

lazy val core = projectMatrix
  .in(file("modules/core"))
  .settings(WeaverPlugin.simpleLayout)
  .full
  .configure(catsEffectDependencies)
  .settings(
    libraryDependencies ++= Seq(
      "com.eed3si9n.expecty" %%% "expecty" % Version.expecty,
      // https://github.com/portable-scala/portable-scala-reflect/issues/23
      "org.portable-scala" %%% "portable-scala-reflect" % Version.portableReflect cross CrossVersion.for3Use2_13
    ),
    libraryDependencies ++= {
      if (virtualAxes.value.contains(VirtualAxis.jvm))
        Seq(
          "org.scala-js" %%% "scalajs-stubs" % Version.scalajsStubs % "provided" cross CrossVersion.for3Use2_13,
          "junit" % "junit" % Version.junit % Optional
        )
      else Seq.empty
    },
    libraryDependencies ++= {
      if (virtualAxes.value.contains(VirtualAxis.jvm)) {
        if (scalaVersion.value.startsWith("3."))
          Seq("org.scala-lang" % "scala-reflect" % scala213)
        else
          Seq("org.scala-lang" % "scala-reflect" % scalaVersion.value)
      } else Seq.empty
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
    inProjects(
      (scalacheck.projectRefs ++ specs2.projectRefs ++ discipline.projectRefs): _*),
    inConfigurations(Compile)
  )

lazy val docs = projectMatrix
  .in(file("modules/docs"))
  .jvmPlatform(WeaverPlugin.supportedScala2Versions)
  .enablePlugins(DocusaurusPlugin, MdocPlugin)
  .dependsOn(core, scalacheck, cats, zio, monix, monixBio, specs2, discipline)
  .settings(
    moduleName := "docs",
    watchSources += (ThisBuild / baseDirectory).value / "docs",
    mdocVariables := Map(
      "VERSION" -> version.value
    ),
    libraryDependencies ++= Seq(
      "org.http4s"    %% "http4s-dsl"          % "0.21.0",
      "org.http4s"    %% "http4s-blaze-server" % "0.21.0",
      "org.http4s"    %% "http4s-blaze-client" % "0.21.0",
      "com.lihaoyi"   %% "fansi"               % "0.2.7",
      "org.typelevel" %% "cats-kernel-laws"    % "2.4.2"
    ),
    Compile / sourceGenerators += Def.taskDyn {
      val filePath =
        (Compile / sourceManaged).value / "BuildMatrix.scala"

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

      val artifactsCE2Version = (cats.finder(
        VirtualAxis.jvm,
        CatsEffect2Axis).apply(scala213) / version).value

      val artifactsCE3Version = (cats.finder(
        VirtualAxis.jvm,
        CatsEffect3Axis).apply(scala213) / version).value

      IO.write(
        filePath,
        s"""
        | package weaver.docs
        |
        | object BuildMatrix {
        |    val catsEffect3Version = ${q(Version.CE3.cats)}
        |    val artifactsCE2Version = ${q(artifactsCE2Version)}
        |    val artifactsCE3Version = ${q(artifactsCE3Version)}
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
  .full
  .settings(
    libraryDependencies ++= {
      if (virtualAxes.value.contains(VirtualAxis.jvm))
        Seq(
          "org.scala-sbt" % "test-interface" % Version.testInterface,
          "org.scala-js" %%% "scalajs-stubs" % Version.scalajsStubs % "provided" cross CrossVersion.for3Use2_13
        )
      else
        Seq(
          "org.scala-js" %% "scalajs-test-interface" % scalaJSVersion cross CrossVersion.for3Use2_13
        )
    } ++ Seq("junit" % "junit" % Version.junit)
  )
  .settings(WeaverPlugin.simpleLayout)

lazy val scalacheck = projectMatrix
  .in(file("modules/scalacheck"))
  .full
  .dependsOn(core, cats % "test->compile")
  .settings(WeaverPlugin.simpleLayout)
  .settings(
    testFrameworks := Seq(new TestFramework("weaver.framework.CatsEffect")),
    libraryDependencies ++= Seq(
      "org.scalacheck" %%% "scalacheck" % Version.scalacheck
    )
  )

lazy val specs2 = projectMatrix
  .in(file("modules/specs2"))
  .sparse(withCE3 = true, withJS = true, withScala3 = false)
  .dependsOn(core, cats % "test->compile")
  .settings(
    name           := "specs2",
    testFrameworks := Seq(new TestFramework("weaver.framework.CatsEffect")),
    libraryDependencies ++= Seq(
      "org.specs2" %%% "specs2-matcher" % Version.specs2
    )
  )
  .settings(WeaverPlugin.simpleLayout)

lazy val discipline = projectMatrix
  .in(file("modules/discipline"))
  .sparse(withCE3 = true, withJS = true, withScala3 = true)
  .dependsOn(core, cats)
  .settings(
    name           := "discipline",
    testFrameworks := Seq(new TestFramework("weaver.framework.CatsEffect")),
    libraryDependencies ++= Seq(
      "org.typelevel" %%% "discipline-core" % Version.discipline,
      "org.typelevel" %%% "cats-laws"       % Version.catsLaws % Test
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
  .full
  .dependsOn(core)
  .settings(WeaverPlugin.simpleLayout)
  .settings(name := "cats-core")

lazy val coreMonix = projectMatrix
  .in(file("modules/core/monix"))
  .sparse(withCE3 = false, withJS = true, withScala3 = false)
  .dependsOn(core)
  .settings(WeaverPlugin.simpleLayout)
  .settings(
    name := "monix-core",
    libraryDependencies ++= Seq(
      "io.monix" %%% "monix" % Version.monix
    )
  )

lazy val coreMonixBio = projectMatrix
  .in(file("modules/core/monixBio"))
  .sparse(withCE3 = false, withJS = true, withScala3 = false)
  .dependsOn(core)
  .settings(WeaverPlugin.simpleLayout)
  .settings(
    name := "monix-bio-core",
    libraryDependencies ++= Seq(
      "io.monix" %%% "monix-bio" % Version.monixBio
    )
  )

lazy val coreZio = projectMatrix
  .in(file("modules/core/zio"))
  .sparse(withCE3 = true, withJS = true, withScala3 = false)
  .dependsOn(core)
  .settings(WeaverPlugin.simpleLayout)
  .settings(
    name := "zio-core",
    libraryDependencies ++= {
      if (virtualAxes.value.contains(CatsEffect3Axis))
        Seq(
          "dev.zio" %%% "zio-interop-cats" % Version.CE3.zioInterop
        )
      else
        Seq(
          "dev.zio" %%% "zio-interop-cats" % Version.CE2.zioInterop
        )
    }
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
  .full
  .settings(WeaverPlugin.simpleLayout)
  .settings(
    name           := "cats",
    testFrameworks := Seq(new TestFramework("weaver.framework.CatsEffect"))
  )

lazy val monix = projectMatrix
  .in(file("modules/framework/monix"))
  .sparse(withCE3 = false, withJS = true, withScala3 = false)
  .dependsOn(framework, coreMonix)
  .settings(WeaverPlugin.simpleLayout)
  .settings(
    name           := "monix",
    testFrameworks := Seq(new TestFramework("weaver.framework.Monix"))
  )

lazy val monixBio = projectMatrix
  .in(file("modules/framework/monix-bio"))
  .sparse(withCE3 = false, withJS = true, withScala3 = false)
  .dependsOn(framework, coreMonixBio)
  .settings(WeaverPlugin.simpleLayout)
  .settings(
    name           := "monix-bio",
    testFrameworks := Seq(new TestFramework("weaver.framework.MonixBIO"))
  )

lazy val zio = projectMatrix
  .in(file("modules/framework/zio"))
  .sparse(withCE3 = true, withJS = true, withScala3 = false)
  .dependsOn(framework, coreZio, scalacheck % "test->compile")
  .settings(WeaverPlugin.simpleLayout)
  .settings(
    name           := "zio",
    testFrameworks := Seq(new TestFramework("weaver.framework.ZIO")),
    libraryDependencies += "io.github.cquiroz" %%% "scala-java-time" % Version.scalaJavaTime % Test
  )

// #################################################################################################
// Intellij
// #################################################################################################

lazy val intellijRunner = projectMatrix
  .sparse(withCE3 = false, withJS = false, withScala3 = false)
  .in(file("modules/intellij-runner"))
  .dependsOn(core, framework, framework % "test->compile")
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
