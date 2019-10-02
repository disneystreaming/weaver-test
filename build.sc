import $ivy.`com.lihaoyi:mill-contrib-bloop_2.12:0.5.1`
import $file.plugins.publish

import mill._
import mill.scalalib._
import mill.scalalib.publish._
import mill.scalajslib.ScalaJSModule

import os._

import plugins.publish.WeaverPublishModule

val scalaVersions = Map("2.12" -> "2.12.10", "2.13" -> "2.13.1")

object core extends mill.Cross[CoreModule](scalaVersions.keys.toSeq: _*)
class CoreModule(crossVersion: String)
    extends WeaverCrossPlatformModule(crossVersion) {
  shared =>
  override def crossPlatformIvyDeps = Agg(
    ivy"com.eed3si9n.expecty::expecty::0.13.0"
  )
  object jvm extends shared.JVM {
    override def compileIvyDeps = Agg(
      ivy"org.scala-js::scalajs-stubs:${scalaJSVersion()}"
    )
  }
  object js extends shared.JS
}

object framework
    extends mill.Cross[FrameworkModule](scalaVersions.keys.toSeq: _*)
class FrameworkModule(crossVersion: String)
    extends WeaverCrossPlatformModule(crossVersion) {
  shared =>

  override def crossPlatformModuleDeps = Seq(core(crossVersion))

  object jvm extends shared.JVM {
    override def compileIvyDeps = Agg(
      ivy"org.scala-js::scalajs-stubs:${scalaJSVersion()}"
    )
    override def ivyDeps = super.ivyDeps() ++ Agg(
      ivy"org.scala-sbt:test-interface:1.0"
    )

    object test extends Tests
  }

  object js extends shared.JS {
    override def ivyDeps = Agg(
      ivy"org.scala-js::scalajs-test-interface:${scalaJSVersion()}"
    )
    object test extends Tests
  }
}

object zio extends mill.Cross[ZioModule](scalaVersions.keys.toSeq: _*)
class ZioModule(crossVersion: String)
    extends WeaverCrossPlatformModule(crossVersion) { shared =>

  override def crossPlatformModuleDeps = Seq(core(crossVersion))
  override def crossPlatformIvyDeps =
    Agg(ivy"dev.zio::zio-interop-cats:2.0.0.0-RC4")
  object jvm extends shared.JVM {
    object test extends Tests
  }
}
//##############################################################################
// COMMON SETTINGS
//##############################################################################

abstract class WeaverCrossPlatformModule(crossVersion: String) extends Module {
  shared =>

  def artifactName =
    s"weaver-${millModuleSegments.parts.filterNot(_ == crossVersion).mkString("-")}"
  def crossPlatformIvyDeps: T[Agg[Dep]]    = T { Agg.empty[Dep] }
  def crossPlatformModuleDeps: Seq[Module] = Seq()

  override val millSourcePath = {
    val relPath = super.millSourcePath.relativeTo(os.pwd)
    os.pwd / "modules" / relPath / os.up
  }

  trait JVM extends WeaverModule { self =>

    def millSourcePath = shared.millSourcePath
    def scalaVersion   = T { scalaVersions(crossVersion) }

    override def moduleDeps: Seq[PublishModule] =
      shared.crossPlatformModuleDeps.flatMap {
        case m: WeaverCrossPlatformModule =>
          m.millModuleDirectChildren.collect {
            case child: PublishModule if !child.isInstanceOf[ScalaJSModule] =>
              child
          }
        case m: PublishModule if !m.isInstanceOf[ScalaJSModule] => Seq(m)
        case _                                                  => Seq()
      }

    override def artifactName = shared.artifactName

    override def ivyDeps = super.ivyDeps() ++ shared.crossPlatformIvyDeps()
    trait Tests extends super.Tests {
      override def moduleDeps =
        super.moduleDeps ++ Seq(framework(crossVersion).jvm)
      override def testFrameworks = Seq(
        "weaver.framework.TestFramework"
      )
    }
  }

  trait JS extends WeaverModule with ScalaJSModule { self =>

    def millSourcePath = shared.millSourcePath
    def scalaVersion   = T { scalaVersions(crossVersion) }

    override def moduleDeps: Seq[PublishModule] =
      shared.crossPlatformModuleDeps.flatMap {
        case m: WeaverCrossPlatformModule =>
          m.millModuleDirectChildren.collect {
            case child: PublishModule if child.isInstanceOf[ScalaJSModule] =>
              child
          }
        case m: PublishModule if m.isInstanceOf[ScalaJSModule] => Seq(m)
        case _                                                 => Seq()
      }

    override def artifactName = shared.artifactName

    override def ivyDeps = super.ivyDeps() ++ shared.crossPlatformIvyDeps()
    trait Tests extends super.Tests {
      override def moduleDeps =
        super.moduleDeps ++ Seq(framework(crossVersion).js)
      override def testFrameworks = Seq(
        "weaver.framework.TestFramework"
      )
    }
  }

}

trait WeaverModule extends WeaverCommonModule with WeaverPublishModule {

  override def ivyDeps = Agg {
    ivy"co.fs2::fs2-core::2.0.1"
  }

}

trait WeaverCommonModule extends ScalaModule {

  def scalaJSVersion = T("0.6.29")

  override def scalacOptions = T {
    val specific2_12 =
      if (scalaVersion().contains("2.12"))
        Seq(
          "-Yno-adapted-args",                // Do not adapt an argument list (either by inserting () or creating a tuple) to match the receiver.
          "-Ypartial-unification",            // Enable partial unification in type constructor inference
          "-Ywarn-inaccessible",              // Warn about inaccessible types in method signatures.
          "-Ywarn-infer-any",                 // Warn when a type argument is inferred to be `Any`.
          "-Ywarn-nullary-override",          // Warn when non-nullary `def f()' overrides nullary `def f'.
          "-Ywarn-nullary-unit",              // Warn when nullary methods return Unit.
          "-Xlint:by-name-right-associative", // By-name parameter of right associative operator.
          "-Xlint:unsound-match",             // Pattern match may not be typesafe.
          "-Xfuture"                          // Turn on future language features.
        )
      else Seq()

    Seq(
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
      // "-Xfatal-warnings", // Fail the compilation if there are any warnings.
      "-Xlint:adapted-args",           // Warn if an argument list is modified to match the receiver.
      "-Xlint:constant",               // Evaluation of a constant arithmetic expression results in an error.
      "-Xlint:delayedinit-select",     // Selecting member of DelayedInit.
      "-Xlint:doc-detached",           // A Scaladoc comment appears to be detached from its element.
      "-Xlint:inaccessible",           // Warn about inaccessible types in method signatures.
      "-Xlint:infer-any",              // Warn when a type argument is inferred to be `Any`.
      "-Xlint:missing-interpolator",   // A string literal appears to be missing an interpolator id.
      "-Xlint:nullary-override",       // Warn when non-nullary `def f()' overrides nullary `def f'.
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
      "-Ywarn-unused:params",          // Warn if a value parameter is unused.
      "-Ywarn-unused:patvars",         // Warn if a variable bound in a pattern is unused.
      "-Ywarn-unused:privates",        // Warn if a private member is unused.
      "-Ywarn-value-discard"           // Warn when non-Unit expression results are unused.
    ) ++ specific2_12
  }

  override def scalacPluginIvyDeps = T {
    val macroParadise =
      if (scalaVersion().contains("2.12"))
        Agg(ivy"org.scalamacros:paradise_${scalaVersion()}:2.1.0")
      else Agg()

    Agg(
      ivy"org.typelevel::kind-projector:0.10.3",
      ivy"com.olegpy::better-monadic-for:0.3.1"
    ) ++ macroParadise
  }

}

def publishAll(
    username: String,
    password: String,
    publishArtifacts: mill.main.Tasks[PublishModule.PublishData]
) =
  T.command(
    WeaverPublishModule.publishAll(username, password, publishArtifacts))
