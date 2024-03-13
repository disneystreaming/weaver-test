import sbt.librarymanagement.Configurations.ScalaDocTool

// https://typelevel.org/sbt-typelevel/faq.html#what-is-a-base-version-anyway
ThisBuild / tlBaseVersion := "0.0" // your current series x.y

ThisBuild / startYear        := Some(2019)
ThisBuild / licenses         := Seq(License.Apache2)
ThisBuild / developers := List(
  tlGitHubDev("baccata", "Olivier Mélois"),
  tlGitHubDev("keynmol", "Anton Sviridov"),
  tlGitHubDev("valencik", "Andrew Valencik")
)

ThisBuild / tlCiHeaderCheck := false

// publish to s01.oss.sonatype.org (set to true to publish to oss.sonatype.org instead)
ThisBuild / tlSonatypeUseLegacyHost := false

// use JDK 11
ThisBuild / githubWorkflowJavaVersions := Seq(JavaSpec.temurin("11"))

val scala212 = "2.12.17"
val scala213 = "2.13.10"
ThisBuild / crossScalaVersions := Seq(scala212, scala213, "3.3.1")
ThisBuild / scalaVersion       := scala213 // the default Scala

val Version = new {
  val catsEffect             = "3.5.2"
  val catsLaws               = "2.9.0"
  val discipline             = "1.5.1"
  val expecty                = "0.16.0"
  val fs2                    = "3.5.0"
  val junit                  = "4.13.2"
  val portableReflect        = "1.1.2"
  val scalaJavaTime          = "2.4.0"
  val scalacheck             = "1.17.0"
  val scalajsMacroTask       = "1.1.1"
  val scalajsStubs           = "1.1.0"
  val testInterface          = "1.0"
  val scalacCompatAnnotation = "0.1.4"
}

lazy val root = tlCrossRootProject.aggregate(core,
                                             framework,
                                             coreCats,
                                             cats,
                                             scalacheck,
                                             discipline)

lazy val core = crossProject(JVMPlatform, JSPlatform, NativePlatform)
  .in(file("modules/core"))
  .settings(
    name := "core",
    libraryDependencies ++= Seq(
      "co.fs2"               %%% "fs2-core"    % Version.fs2,
      "org.typelevel"        %%% "cats-effect" % Version.catsEffect,
      "com.eed3si9n.expecty" %%% "expecty"     % Version.expecty,
      // https://github.com/portable-scala/portable-scala-reflect/issues/23
      "org.portable-scala" %%% "portable-scala-reflect" % Version.portableReflect cross CrossVersion.for3Use2_13,
      "org.typelevel" %% "scalac-compat-annotation" % Version.scalacCompatAnnotation
    )
  )

lazy val coreJVM = core.jvm
  .settings(
    libraryDependencies ++= Seq(
      "org.scala-js" %%% "scalajs-stubs" % Version.scalajsStubs % "provided" cross CrossVersion.for3Use2_13,
      "junit" % "junit" % Version.junit % Optional,
      if (scalaVersion.value.startsWith("3."))
        "org.scala-lang" % "scala-reflect" % scala213
      else
        "org.scala-lang" % "scala-reflect" % scalaVersion.value
    )
  )

lazy val framework = crossProject(JVMPlatform, JSPlatform, NativePlatform)
  .in(file("modules/framework"))
  .dependsOn(core)
  .settings(
    name := "framework",
    libraryDependencies ++= Seq(
      "junit" % "junit" % Version.junit
    )
  )

lazy val frameworkJVM = framework.jvm
  .settings(
    libraryDependencies ++= Seq(
      "org.scala-sbt" % "test-interface" % Version.testInterface,
      "org.scala-js" %%% "scalajs-stubs" % Version.scalajsStubs % "provided" cross CrossVersion.for3Use2_13
    )
  )

lazy val frameworkJS = framework.js
  .settings(
    libraryDependencies ++= Seq(
      "org.scala-js" %% "scalajs-test-interface" % scalaJSVersion cross CrossVersion.for3Use2_13
    )
  )

lazy val frameworkNative = framework.native
  .settings(
    libraryDependencies ++= Seq(
      "org.scala-native" %%% "test-interface" % nativeVersion
    )
  )

lazy val coreCats = crossProject(JVMPlatform, JSPlatform, NativePlatform)
  .in(file("modules/core-cats"))
  .dependsOn(core)
  .settings(
    libraryDependencies ++= Seq(
      "junit" % "junit" % Version.junit % ScalaDocTool
    )
  )
  .settings(name := "cats-core")

lazy val coreCatsJS = coreCats.js
  .settings(
    libraryDependencies ++= Seq(
      "org.scala-js" %%% "scala-js-macrotask-executor" % Version.scalajsMacroTask)
  )

lazy val cats = crossProject(JVMPlatform, JSPlatform, NativePlatform)
  .in(file("modules/framework-cats"))
  .dependsOn(framework, coreCats)
  .settings(
    name           := "cats",
    testFrameworks := Seq(new TestFramework("weaver.framework.CatsEffect"))
  )

lazy val scalacheck = crossProject(JVMPlatform, JSPlatform, NativePlatform)
  .in(file("modules/scalacheck"))
  .dependsOn(core, cats % "test->compile")
  .settings(
    testFrameworks := Seq(new TestFramework("weaver.framework.CatsEffect")),
    libraryDependencies ++= Seq(
      "org.scalacheck" %%% "scalacheck"          % Version.scalacheck,
      "org.typelevel"  %%% "cats-effect-testkit" % Version.catsEffect % Test)
  )

lazy val discipline = crossProject(JVMPlatform, JSPlatform, NativePlatform)
  .in(file("modules/discipline"))
  .dependsOn(core, cats)
  .settings(
    name           := "discipline",
    testFrameworks := Seq(new TestFramework("weaver.framework.CatsEffect")),
    libraryDependencies ++= Seq(
      "org.typelevel" %%% "discipline-core" % Version.discipline,
      "org.typelevel" %%% "cats-laws"       % Version.catsLaws % Test
    )
  )
