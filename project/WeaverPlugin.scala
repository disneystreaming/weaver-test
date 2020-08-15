// For getting Scoverage out of the generated POM
import scala.xml.Elem
import scala.xml.transform.{ RewriteRule, RuleTransformer }
import sbtcrossproject.CrossPlugin.autoImport.{
  JVMPlatform,
  crossProjectPlatform
}
import scalafix.sbt.ScalafixPlugin.autoImport._
import xerial.sbt.Sonatype.SonatypeKeys._

import sbt._
import sbt.Keys._
import com.jsuereth.sbtpgp.PgpKeys._
import scalajscrossproject.JSPlatform

/**
 * Common project settings.
 */
object WeaverPlugin extends AutoPlugin {

  override def requires = plugins.JvmPlugin
  override def trigger  = allRequirements

  lazy val scala212               = "2.12.12"
  lazy val scala213               = "2.13.3"
  lazy val supportedScalaVersions = List(scala212, scala213)

  /** @see [[sbt.AutoPlugin]] */
  override val buildSettings = Seq(
    addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1"),
    addCompilerPlugin(
      "org.typelevel" %% "kind-projector" % "0.11.0" cross CrossVersion.full)
  )

  /** @see [[sbt.AutoPlugin]] */
  override val projectSettings = Seq(
    moduleName := s"weaver-${name.value}",
    crossScalaVersions := supportedScalaVersions,
    scalacOptions ++= compilerOptions(scalaVersion.value),
    // Turning off fatal warnings for ScalaDoc, otherwise we can't release.
    Compile / doc / scalacOptions ~= (_ filterNot (_ == "-Xfatal-warnings")),
    // ScalaDoc settings
    autoAPIMappings := true,
    ThisBuild / scalacOptions ++= Seq(
      // Note, this is used by the doc-source-url feature to determine the
      // relative path of a given source file. If it's not a prefix of a the
      // absolute path of the source file, the absolute path of that file
      // will be put into the FILE_SOURCE variable, which is
      // definitely not what we want.
      "-sourcepath",
      file(".").getAbsolutePath.replaceAll("[.]$", "")
    ),
    // https://github.com/sbt/sbt/issues/2654
    incOptions := incOptions.value.withLogRecompileOnMacro(false),
    testFrameworks := Seq(new TestFramework("weaver.framework.TestFramework")),
    // https://scalacenter.github.io/scalafix/docs/users/installation.html
    semanticdbEnabled := true,
    semanticdbVersion := scalafixSemanticdb.revision
  ) ++ coverageSettings ++ publishSettings

  def compilerOptions(scalaVersion: String) = {
    commonCompilerOptions ++ {
      if (priorTo2_13(scalaVersion)) compilerOptions2_12_Only
      else Seq.empty
    }
  }

  def priorTo2_13(scalaVersion: String): Boolean =
    CrossVersion.partialVersion(scalaVersion) match {
      case Some((2, minor)) if minor < 13 => true
      case _                              => false
    }

  lazy val commonCompilerOptions = Seq(
    "-deprecation", // Emit warning and location for usages of deprecated APIs.
    "-encoding",
    "utf-8",                         // Specify character encoding used by source files.
    "-explaintypes",                 // Explain type errors in more detail.
    "-feature",                      // Emit warning and location for usages of features that should be imported explicitly.
    "-language:existentials",        // Existential types (besides wildcard types) can be written and inferred
    "-language:experimental.macros", // Allow macro definition (besides implementation and application)
    "-language:higherKinds",         // Allow higher-kinded types
    "-language:implicitConversions", // Allow definition of implicit functions called views
    "-unchecked",                    // Enable additional warnings where generated code depends on assumptions.
    "-Xcheckinit",                   // Wrap field accessors to throw an exception on uninitialized access.
    "-Xlint:adapted-args",           // Warn if an argument list is modified to match the receiver.
    "-Xlint:constant",               // Evaluation of a constant arithmetic expression results in an error.
    "-Xlint:delayedinit-select",     // Selecting member of DelayedInit.
    "-Xlint:doc-detached",           // A Scaladoc comment appears to be detached from its element.
    "-Xlint:inaccessible",           // Warn about inaccessible types in method signatures.
    "-Xlint:infer-any",              // Warn when a type argument is inferred to be `Any`.
    "-Xlint:missing-interpolator",   // A string literal appears to be missing an interpolator id.
    "-Xlint:nullary-unit",           // Warn when nullary methods return Unit.
    "-Xlint:option-implicit",        // Option.apply used implicit view.
    "-Xlint:package-object-classes", // Class or object defined in package object.
    "-Xlint:poly-implicit-overload", // Parameterized overloaded implicit methods are not visible as view bounds.
    "-Xlint:private-shadow",         // A private field (or class parameter) shadows a superclass field.
    "-Xlint:stars-align",            // Pattern sequence wildcard must align with sequence component.
    "-Xlint:type-parameter-shadow",  // A local type parameter shadows a type already in scope.
    "-Ywarn-dead-code",              // Warn when dead code is identified.
    "-Ywarn-extra-implicit",         // Warn when more than one implicit parameter section is defined.
    "-Ywarn-numeric-widen",          // Warn when numerics are widened.
    "-Ywarn-unused:implicits",       // Warn if an implicit parameter is unused.
    "-Ywarn-unused:imports",         // Warn if an import selector is not referenced.
    "-Ywarn-unused:locals",          // Warn if a local definition is unused.
    "-Ywarn-unused:patvars",         // Warn if a variable bound in a pattern is unused.
    "-Ywarn-unused:privates",        // Warn if a private member is unused.
    "-Ywarn-value-discard",          // Warn when non-Unit expression results are unused.
    "-Xfatal-warnings"               // Fail the compilation if there are any warnings.
  )

  lazy val compilerOptions2_12_Only =
    // These are unrecognized for Scala 2.13.
    Seq(
      "-Xfuture",                         // Turn on future language features.
      "-Xlint:by-name-right-associative", // By-name parameter of right associative operator.
      "-Xlint:nullary-override",          // Warn when non-nullary `def f()' overrides nullary `def f'.
      "-Xlint:unsound-match",             // Pattern match may not be typesafe.
      "-Yno-adapted-args",                // Do not adapt an argument list (either by inserting () or creating a tuple) to match the receiver.
      "-Ypartial-unification",            // Enable partial unification in type constructor inference
      "-Ywarn-inaccessible",              // Warn about inaccessible types in method signatures.
      "-Ywarn-infer-any",                 // Warn when a type argument is inferred to be `Any`.
      "-Ywarn-nullary-override",          // Warn when non-nullary `def f()' overrides nullary `def f'.
      "-Ywarn-nullary-unit"               // Warn when nullary methods return Unit.
    )

  lazy val coverageSettings = Seq(
    // For evicting Scoverage out of the generated POM
    // See: https://github.com/scoverage/sbt-scoverage/issues/153
    pomPostProcess := { (node: xml.Node) =>
      new RuleTransformer(new RewriteRule {
        override def transform(node: xml.Node): Seq[xml.Node] = node match {
          case e: Elem
              if e.label == "dependency" && e.child.exists(child =>
                child.label == "groupId" && child.text == "org.scoverage") =>
            Nil
          case _ => Seq(node)
        }
      }).transform(node).head
    }
  )

  lazy val doNotPublishArtifact = Seq(
    publishArtifact := false,
    Compile / packageDoc / publishArtifact := false,
    Compile / packageSrc / publishArtifact := false,
    Compile / packageBin / publishArtifact := false
  )

  def profile: Project => Project = pr => {
    val withCoverage = sys.env.getOrElse("SBT_PROFILE", "") match {
      case "coverage" => pr
      case _          => pr.disablePlugins(scoverage.ScoverageSbtPlugin)
    }
    withCoverage
  }

  // Mill-like simple layout
  val simpleLayout: Seq[Setting[_]] = Seq(
    Compile / unmanagedSourceDirectories := Seq(
      baseDirectory.value.getParentFile / "src") ++ {
      if (crossProjectPlatform.value == JVMPlatform)
        Seq(baseDirectory.value.getParentFile / "src-jvm")
      else if (crossProjectPlatform.value == JSPlatform)
        Seq(baseDirectory.value.getParentFile / "src-js")
      else
        Seq.empty
    },
    Test / unmanagedSourceDirectories := Seq(
      baseDirectory.value.getParentFile / "test" / "src"
    ) ++ {
      if (crossProjectPlatform.value == JVMPlatform)
        Seq(baseDirectory.value.getParentFile / "test" / "src-jvm")
      else if (crossProjectPlatform.value == JSPlatform)
        Seq(baseDirectory.value.getParentFile / "test" / "src-js")
      else
        Seq.empty
    }
  )

  lazy val publishSettings = Seq(
    organization := "com.disneystreaming",
    version := sys.env
      .getOrElse("DRONE_TAG", version.value)
      .dropWhile(_ == 'v'),
    publishTo := sonatypePublishToBundle.value,
    publishMavenStyle := true,
    licenses := Seq(
      "Apache" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
    homepage := Some(url("https://github.com/disneystreaming")),
    scmInfo := Some(
      ScmInfo(
        url("https://github.com/disneystreaming/weaver-test"),
        "scm:git@github.com:disneystreaming/weaver-test.git"
      )
    ),
    developers := List(
      Developer(
        id = "Olivier Mélois",
        name = "Olivier Mélois",
        email = "olivier.melois@disneystreaming.com",
        url = url("https://github.com/baccata")
      ),
      Developer(
        id = "Anton Sviridov",
        name = "Anton Sviridov",
        email = "anton.sviridov@disneystreaming.com",
        url = url("https://github.com/keynmol")
      )
    ),
    credentials ++=
      sys.env
        .get("SONATYPE_USER")
        .zip(sys.env.get("SONATYPE_PASSWORD"))
        .map {
          case (username, password) =>
            Credentials(
              "Sonatype Nexus Repository Manager",
              "oss.sonatype.org",
              username,
              password
            )
        }
        .toSeq
  )

}
