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

ThisBuild / scalafixDependencies += "com.github.liancheng" %% "organize-imports" % "0.5.0"

Global / (Test / fork) := true
Global / (Test / testOptions) += Tests.Argument("--quickstart")

sonatypeCredentialHost := "s01.oss.sonatype.org"

val Version = new {
  object CE3 {
    val fs2  = "3.3.0"
    val cats = "3.3.14"
  }

  val expecty          = "0.16.0"
  val portableReflect  = "1.1.2"
  val junit            = "4.13.2"
  val scalajsStubs     = "1.1.0"
  val discipline       = "1.5.1"
  val catsLaws         = "2.8.0"
  val scalacheck       = "1.16.0"
  val testInterface    = "1.0"
  val scalaJavaTime    = "2.4.0"
  val scalajsMacroTask = "1.0.0"
}

lazy val root = project
  .in(file("."))
  .aggregate(allModules: _*)
  .settings(WeaverPlugin.doNotPublishArtifact)

lazy val allModules = Seq(
  core.projectRefs,
  framework.projectRefs,
  scalacheck.projectRefs,
  discipline.projectRefs,
  intellijRunner.projectRefs,
  effectCores,
  effectFrameworks
).flatten

def catsEffectDependencies(proj: Project): Project = {
  proj.settings(
    libraryDependencies ++=
      Seq(
        "co.fs2"        %%% "fs2-core"    % Version.CE3.fs2,
        "org.typelevel" %%% "cats-effect" % Version.CE3.cats
      )
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
      (scalacheck.projectRefs ++ discipline.projectRefs): _*),
    inConfigurations(Compile)
  )

lazy val docs = projectMatrix
  .in(file("modules/docs"))
  .jvmPlatform(Seq(WeaverPlugin.scala213))
  .enablePlugins(DocusaurusPlugin, MdocPlugin)
  .dependsOn(core, scalacheck, cats, discipline)
  .settings(
    moduleName := "docs",
    watchSources += (ThisBuild / baseDirectory).value / "docs",
    mdocVariables := Map(
      "VERSION" -> version.value
    ),
    libraryDependencies ++= Seq(
      "org.http4s"    %% "http4s-dsl"          % "0.23.12",
      "org.http4s"    %% "http4s-blaze-server" % "0.23.12",
      "org.http4s"    %% "http4s-blaze-client" % "0.23.12",
      "com.lihaoyi"   %% "fansi"               % "0.2.7",
      "org.typelevel" %% "cats-kernel-laws"    % "2.8.0"
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

          List(
            s"name = ${q(name)}",
            s"jvm = $isJVM",
            s"js = $isJS",
            s"scalaVersion = ${q(scalaVersion)}",
            s"version = ${q(ver)}"
          ).mkString("Artifact(", ",", ")")
      }.mkString("List(", ",\n", ")")

      val effects = process(projectsWithAxes.all(allEffectCoresFilter).value)
      val integrations =
        process(projectsWithAxes.all(allIntegrationsCoresFilter).value)

      val artifactsCE3Version = (cats.jvm(scala213) / version).value

      IO.write(
        filePath,
        s"""
        | package weaver.docs
        |
        | object BuildMatrix {
        |    val catsEffect3Version = ${q(Version.CE3.cats)}
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
      else if (virtualAxes.value.contains(VirtualAxis.js))
        Seq(
          "org.scala-js" %% "scalajs-test-interface" % scalaJSVersion cross CrossVersion.for3Use2_13
        )
      else if (virtualAxes.value.contains(VirtualAxis.native))
        Seq(
          "org.scala-native" %%% "test-interface" % nativeVersion
        )
      else Seq.empty
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
      "org.scalacheck" %%% "scalacheck"          % Version.scalacheck,
      "org.typelevel"  %%% "cats-effect-testkit" % Version.CE3.cats % Test)
  )

lazy val discipline = projectMatrix
  .in(file("modules/discipline"))
  .sparse(withJS = true, withScala3 = true)
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
  coreCats.projectRefs

lazy val coreCats = projectMatrix
  .in(file("modules/core/cats"))
  .full
  .dependsOn(core)
  .settings(WeaverPlugin.simpleLayout)
  .settings(scalaJSMacroTask)
  .settings(name := "cats-core")

// #################################################################################################
// Effect-specific frameworks
// #################################################################################################

lazy val effectFrameworks: Seq[ProjectReference] = cats.projectRefs

lazy val cats = projectMatrix
  .in(file("modules/framework/cats"))
  .dependsOn(framework, coreCats)
  .full
  .settings(WeaverPlugin.simpleLayout)
  .settings(
    name           := "cats",
    testFrameworks := Seq(new TestFramework("weaver.framework.CatsEffect"))
  )

// #################################################################################################
// Intellij
// #################################################################################################

lazy val intellijRunner = projectMatrix
  .sparse(withJS = false, withScala3 = false)
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

ThisBuild / concurrentRestrictions ++= {
  if (!sys.env.contains("CI")) {
    Seq(
      Tags.limitAll(4),
      Tags.limit(ScalaJSPlugin.autoImport.ScalaJSTags.Link, 1)
    )
  } else Seq.empty
}

lazy val scalaJSMacroTask: Seq[Def.Setting[_]] = Seq(
  libraryDependencies ++= {
    if (virtualAxes.value.contains(VirtualAxis.js))
      Seq("org.scala-js" %%% "scala-js-macrotask-executor" % Version.scalajsMacroTask)
    else
      Seq.empty
  }
)
