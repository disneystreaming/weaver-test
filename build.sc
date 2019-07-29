import $ivy.`com.lihaoyi:mill-contrib-bloop_2.12:0.5.0`

import mill._
import mill.scalalib._
import mill.scalalib.publish._
import mill.scalajslib.ScalaJSModule
import $file.gitVersion
import os._

def millSourcePath: Path = pwd / 'modules

object core extends WeaverCrossPlatformModule { shared =>
  override def crossPlatformIvyDeps = Agg(
    ivy"com.eed3si9n.expecty::expecty::0.13.0"
  )
  object jvm extends shared.JVM {
    override def compileIvyDeps = Agg(
      ivy"org.scala-js::scalajs-stubs:${scalaJSVersion()}"
    )
  }
  object js  extends shared.JS
}

object framework extends WeaverCrossPlatformModule { shared =>

  override def crossPlatformModuleDeps = Seq(core)

  object jvm extends shared.JVM {
    override def compileIvyDeps = Agg(
      ivy"org.scala-js::scalajs-stubs:${scalaJSVersion()}"
    )
    override def ivyDeps = super.ivyDeps() ++ Agg(
      ivy"org.scala-sbt:test-interface:1.0"
    )

    object test extends Tests {
      override def testFrameworks = Seq(
        "weaver.framework.TestFramework"
      )
    }
  }

  object js extends shared.JS {
    override def ivyDeps = Agg(
      ivy"org.scala-js::scalajs-test-interface:${scalaJSVersion()}"
    )
    object test extends Tests {
      override def testFrameworks = Seq(
        "weaver.framework.TestFramework"
      )
    }
  }
}
//##############################################################################
// COMMON SETTINGS
//##############################################################################

trait WeaverCrossPlatformModule extends Module { shared =>

  def artifactName                           = s"weaver-${millModuleSegments.parts.mkString("-")}"
  def crossPlatformIvyDeps: T[Agg[Dep]] = T { Agg.empty[Dep] }
  def crossPlatformModuleDeps: Seq[Module]   = Seq()

  trait JVM extends WeaverModule { self =>

    override def millSourcePath = shared.millSourcePath

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
      override def moduleDeps = super.moduleDeps ++ Seq(core.jvm)
      override def sources = T.sources {
        shared.millModuleBasePath.value / 'test / 'src
      }
    }
  }

  trait JS extends WeaverModule with ScalaJSModule { self =>

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
    override def sources = T.sources(
      shared.millSourcePath / 'src
    )

    override def ivyDeps = super.ivyDeps() ++ shared.crossPlatformIvyDeps()
    trait Tests extends super.Tests {
      override def moduleDeps = super.moduleDeps ++ Seq(core.js)
      override def sources = T.sources {
        shared.millModuleBasePath.value / 'test / 'src
      }
    }
  }

}

trait WeaverModule extends WeaverPublishModule {

  override def ivyDeps = Agg {
    ivy"co.fs2::fs2-core::1.0.4"
  }

}

trait WeaverPublishModule extends WeaverCommonModule with PublishModule
with gitVersion.GitVersionModule {

  override def artifactName = s"weaver-${super.artifactName()}"

  def pomSettings: mill.T[mill.scalalib.publish.PomSettings] = PomSettings(
    """
      |Melting pot of library helping with testing, aws, and other things
      |""".stripMargin.trim,
    "io.github.baccata",
    "http://github.com/baccata/weaver",
    Seq(License.Common.Apache2),
    VersionControl(browsableRepository =
                     Some("http://github.com/baccata/weaver"),
                   tag = latestTag()),
    Seq(Developer("baccata", "Olivier JJ Melois", "http://github.com/baccata"))
  )
  def publishVersion: mill.T[String] = gitVersion
}

trait WeaverCommonModule extends ScalaModule {

  def scalaVersion   = T("2.12.8")
  def scalaJSVersion = T("0.6.28")

  override def scalacOptions = Seq(
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
    "-Xfuture",                         // Turn on future language features.
    "-Xlint:adapted-args",              // Warn if an argument list is modified to match the receiver.
    "-Xlint:by-name-right-associative", // By-name parameter of right associative operator.
    "-Xlint:constant",                  // Evaluation of a constant arithmetic expression results in an error.
    "-Xlint:delayedinit-select",        // Selecting member of DelayedInit.
    "-Xlint:doc-detached",              // A Scaladoc comment appears to be detached from its element.
    "-Xlint:inaccessible",              // Warn about inaccessible types in method signatures.
    "-Xlint:infer-any",                 // Warn when a type argument is inferred to be `Any`.
    "-Xlint:missing-interpolator",      // A string literal appears to be missing an interpolator id.
    "-Xlint:nullary-override",          // Warn when non-nullary `def f()' overrides nullary `def f'.
    "-Xlint:nullary-unit",              // Warn when nullary methods return Unit.
    "-Xlint:option-implicit",           // Option.apply used implicit view.
    "-Xlint:package-object-classes",    // Class or object defined in package object.
    "-Xlint:poly-implicit-overload",    // Parameterized overloaded implicit methods are not visible as view bounds.
    "-Xlint:private-shadow",            // A private field (or class parameter) shadows a superclass field.
    "-Xlint:stars-align",               // Pattern sequence wildcard must align with sequence component.
    "-Xlint:type-parameter-shadow",     // A local type parameter shadows a type already in scope.
    "-Xlint:unsound-match",             // Pattern match may not be typesafe.
    "-Yno-adapted-args",                // Do not adapt an argument list (either by inserting () or creating a tuple) to match the receiver.
    "-Ypartial-unification",            // Enable partial unification in type constructor inference
    "-Ywarn-dead-code",                 // Warn when dead code is identified.
    "-Ywarn-extra-implicit",            // Warn when more than one implicit parameter section is defined.
    "-Ywarn-inaccessible",              // Warn about inaccessible types in method signatures.
    "-Ywarn-infer-any",                 // Warn when a type argument is inferred to be `Any`.
    "-Ywarn-nullary-override",          // Warn when non-nullary `def f()' overrides nullary `def f'.
    "-Ywarn-nullary-unit",              // Warn when nullary methods return Unit.
    "-Ywarn-numeric-widen",             // Warn when numerics are widened.
    "-Ywarn-unused:implicits",          // Warn if an implicit parameter is unused.
    "-Ywarn-unused:imports",            // Warn if an import selector is not referenced.
    "-Ywarn-unused:locals",             // Warn if a local definition is unused.
    "-Ywarn-unused:params",             // Warn if a value parameter is unused.
    "-Ywarn-unused:patvars",            // Warn if a variable bound in a pattern is unused.
    "-Ywarn-unused:privates",           // Warn if a private member is unused.
    "-Ywarn-value-discard"              // Warn when non-Unit expression results are unused.
  )

  override def scalacPluginIvyDeps = Agg(
    ivy"org.spire-math::kind-projector:0.9.6",
    ivy"com.olegpy::better-monadic-for:0.3.0-M4",
    ivy"org.scalamacros:paradise_${scalaVersion()}:2.1.0"
  )

}
