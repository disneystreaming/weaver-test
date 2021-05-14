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

lazy val root = project
  .in(file("."))
  .aggregate(allModules: _*)
  .configure(WeaverPlugin.profile)
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

lazy val catsEffect3Version = "3.1.0"

def catsEffectDependencies(proj: Project): Project = {
  proj.settings(
    libraryDependencies ++= {
      if (virtualAxes.value.contains(CatsEffect2Axis))
        Seq(
          "co.fs2"        %%% "fs2-core"    % "2.5.5",
          "org.typelevel" %%% "cats-effect" % "2.5.0"
        )
      else
        Seq(
          "co.fs2"        %%% "fs2-core"    % "3.0.2",
          "org.typelevel" %%% "cats-effect" % catsEffect3Version
        )
    }
  )
}

lazy val core = projectMatrix
  .in(file("modules/core"))
  .configure(WeaverPlugin.profile)
  .settings(WeaverPlugin.simpleLayout)
  .full
  .configure(catsEffectDependencies)
  .settings(
    libraryDependencies ++= Seq(
      "com.eed3si9n.expecty" %%% "expecty" % "0.15.2",
      // https://github.com/portable-scala/portable-scala-reflect/issues/23
      "org.portable-scala" %%% "portable-scala-reflect" % "1.1.1" cross CrossVersion.for3Use2_13
    ),
    libraryDependencies ++= {
      if (virtualAxes.value.contains(VirtualAxis.jvm))
        Seq(
          "org.scala-js" %%% "scalajs-stubs" % "1.0.0"  % "provided" cross CrossVersion.for3Use2_13,
          "junit"          % "junit"         % "4.13.2" % Optional
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
        |    val catsEffect3Version = ${q(catsEffect3Version)}
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
          "org.scala-sbt"  % "test-interface" % "1.0",
          "org.scala-js" %%% "scalajs-stubs"  % "1.0.0" % "provided" cross CrossVersion.for3Use2_13
        )
      else
        Seq(
          "org.scala-js" %% "scalajs-test-interface" % scalaJSVersion cross CrossVersion.for3Use2_13
        )
    } ++ Seq("junit" % "junit" % "4.13.2")
  )
  .configure(WeaverPlugin.profile)
  .settings(WeaverPlugin.simpleLayout)

lazy val scalacheck = projectMatrix
  .in(file("modules/scalacheck"))
  .full
  .dependsOn(core, cats % "test->compile")
  .configure(WeaverPlugin.profile)
  .settings(WeaverPlugin.simpleLayout)
  .settings(
    testFrameworks := Seq(new TestFramework("weaver.framework.CatsEffect")),
    libraryDependencies ++= Seq(
      "org.scalacheck" %%% "scalacheck" % "1.15.3"
    )
  )

lazy val specs2 = projectMatrix
  .in(file("modules/specs2"))
  .sparse(withCE3 = true, withJS = true, withScala3 = false)
  .dependsOn(core, cats % "test->compile")
  .configure(WeaverPlugin.profile)
  .settings(
    name := "specs2",
    testFrameworks := Seq(new TestFramework("weaver.framework.CatsEffect")),
    libraryDependencies ++= Seq(
      "org.specs2" %%% "specs2-matcher" % "4.10.6"
    )
  )
  .settings(WeaverPlugin.simpleLayout)

lazy val discipline = projectMatrix
  .in(file("modules/discipline"))
  .sparse(withCE3 = true, withJS = true, withScala3 = true)
  .dependsOn(core, cats)
  .configure(WeaverPlugin.profile)
  .settings(
    name := "discipline",
    testFrameworks := Seq(new TestFramework("weaver.framework.CatsEffect")),
    libraryDependencies ++= Seq(
      "org.typelevel" %%% "discipline-core" % "1.1.4",
      "org.typelevel" %%% "cats-laws"       % "2.6.0" % Test
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
  .configure(WeaverPlugin.profile)
  .settings(WeaverPlugin.simpleLayout)
  .settings(name := "cats-core")

lazy val coreMonix = projectMatrix
  .in(file("modules/core/monix"))
  .sparse(withCE3 = false, withJS = true, withScala3 = false)
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
  .sparse(withCE3 = false, withJS = true, withScala3 = false)
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
  .sparse(withCE3 = true, withJS = true, withScala3 = false)
  .dependsOn(core)
  .configure(WeaverPlugin.profile)
  .settings(WeaverPlugin.simpleLayout)
  .settings(
    name := "zio-core",
    libraryDependencies ++= {
      if (virtualAxes.value.contains(CatsEffect3Axis))
        Seq(
          "dev.zio" %%% "zio-interop-cats" % "3.0.2.0"
        )
      else
        Seq(
          "dev.zio" %%% "zio-interop-cats" % "2.4.1.0"
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
  .configure(WeaverPlugin.profile)
  .settings(WeaverPlugin.simpleLayout)
  .settings(
    name := "cats",
    testFrameworks := Seq(new TestFramework("weaver.framework.CatsEffect"))
  )

lazy val monix = projectMatrix
  .in(file("modules/framework/monix"))
  .sparse(withCE3 = false, withJS = true, withScala3 = false)
  .dependsOn(framework, coreMonix)
  .configure(WeaverPlugin.profile)
  .settings(WeaverPlugin.simpleLayout)
  .settings(
    name := "monix",
    testFrameworks := Seq(new TestFramework("weaver.framework.Monix"))
  )

lazy val monixBio = projectMatrix
  .in(file("modules/framework/monix-bio"))
  .sparse(withCE3 = false, withJS = true, withScala3 = false)
  .dependsOn(framework, coreMonixBio)
  .configure(WeaverPlugin.profile)
  .settings(WeaverPlugin.simpleLayout)
  .settings(
    name := "monix-bio",
    testFrameworks := Seq(new TestFramework("weaver.framework.MonixBIO"))
  )

lazy val zio = projectMatrix
  .in(file("modules/framework/zio"))
  .sparse(withCE3 = true, withJS = true, withScala3 = false)
  .dependsOn(framework, coreZio, scalacheck % "test->compile")
  .configure(WeaverPlugin.profile)
  .settings(WeaverPlugin.simpleLayout)
  .settings(
    name := "zio",
    testFrameworks := Seq(new TestFramework("weaver.framework.ZIO")),
    libraryDependencies += "io.github.cquiroz" %%% "scala-java-time" % "2.2.2" % Test
  )

// #################################################################################################
// Intellij
// #################################################################################################

lazy val intellijRunner = projectMatrix
  .sparse(withCE3 = false, withJS = false, withScala3 = false)
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
