// https://typelevel.org/sbt-typelevel/faq.html#what-is-a-base-version-anyway
ThisBuild / tlBaseVersion := "0.0" // your current series x.y

ThisBuild / organization := "weaver"
ThisBuild / organizationName := "Typelevel"
ThisBuild / startYear := Some(2019)
ThisBuild / licenses := Seq(License.Apache2)
ThisBuild / developers := List(
  tlGitHubDev("baccata", "Olivier Mélois"),
  tlGitHubDev("keynmol", "Anton Sviridov"),
  tlGitHubDev("valencik", "Andrew Valencik"),
)

// publish to s01.oss.sonatype.org (set to true to publish to oss.sonatype.org instead)
ThisBuild / tlSonatypeUseLegacyHost := false

// use JDK 11
ThisBuild / githubWorkflowJavaVersions := Seq(JavaSpec.temurin("11"))

val scala212 = "2.12.17"
val scala213 = "2.13.10"
ThisBuild / crossScalaVersions := Seq(scala212, scala213, "3.3.1")
ThisBuild / scalaVersion := scala213 // the default Scala

val Version = new {
  val catsEffect       = "3.5.2"
  val catsLaws         = "2.9.0"
  val discipline       = "1.5.1"
  val expecty          = "0.16.0"
  val fs2              = "3.5.0"
  val junit            = "4.13.2"
  val portableReflect  = "1.1.2"
  val scalaJavaTime    = "2.4.0"
  val scalacheck       = "1.17.0"
  val scalajsMacroTask = "1.1.1"
  val scalajsStubs     = "1.1.0"
  val testInterface    = "1.0"
}

lazy val root = tlCrossRootProject.aggregate(core, framework)

lazy val core = crossProject(JVMPlatform, JSPlatform, NativePlatform)
  .in(file("modules/core"))
  .settings(
    name := "core",
    libraryDependencies ++= Seq(
      "co.fs2"        %%% "fs2-core"    % Version.fs2,
      "org.typelevel" %%% "cats-effect" % Version.catsEffect,
      "com.eed3si9n.expecty" %%% "expecty" % Version.expecty,
      // https://github.com/portable-scala/portable-scala-reflect/issues/23
      "org.portable-scala" %%% "portable-scala-reflect" % Version.portableReflect cross CrossVersion.for3Use2_13,
    ),
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
