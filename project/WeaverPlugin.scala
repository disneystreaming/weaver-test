// For getting Scoverage out of the generated POM
import scala.xml.Elem
import scala.xml.transform.{ RewriteRule, RuleTransformer }
import scalafix.sbt.ScalafixPlugin.autoImport._
import xerial.sbt.Sonatype.SonatypeKeys._

import sbt._
import sbt.Keys._
import com.jsuereth.sbtpgp.PgpKeys._
import sbtprojectmatrix.ProjectMatrixKeys.virtualAxes
import sbt.internal.ProjectMatrix

import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport.scalaJSLinkerConfig
import org.scalajs.linker.interface.ModuleKind
import org.scalajs.sbtplugin.ScalaJSPlugin
import scala.collection.immutable.Nil
import java.util.regex.MatchResult
import lmcoursier.definitions.Reconciliation.SemVer
import sbt.VirtualAxis.ScalaVersionAxis
import _root_.scalafix.sbt.ScalafixPlugin
import org.scalafmt.sbt.ScalafmtPlugin

case class CatsEffectAxis(idSuffix: String, directorySuffix: String)
    extends VirtualAxis.WeakAxis

/**
 * Common project settings.
 */
object WeaverPlugin extends AutoPlugin {

  val CatsEffect2Axis = CatsEffectAxis("_CE2", "ce2")
  val CatsEffect3Axis = CatsEffectAxis("_CE3", "ce3")

  implicit final class ProjectMatrixOps(pmx: ProjectMatrix) {
    type ConfigureX = ProjectMatrix => ProjectMatrix
    type Configure  = Project => Project

    val defaults = Seq[VirtualAxis](
      CatsEffect2Axis,
      VirtualAxis.jvm,
      VirtualAxis.scalaVersionAxis(WeaverPlugin.scala213, "2.13"))

    def addOne(
        scalaVersion: String,
        platform: VirtualAxis.PlatformAxis,
        catsEffectAxis: CatsEffectAxis): ConfigureX = {
      projectMatrix =>
        val addScalafix: Configure =
          if (scalaVersion == scala213)
            (_: Project).enablePlugins(ScalafixPlugin)
          else (_: Project).disablePlugins(ScalafixPlugin)

        val addScalafmt: Configure =
          if (scalaVersion == scala213)
            (_: Project).enablePlugins(ScalafmtPlugin)
          else (_: Project).disablePlugins(ScalafmtPlugin)

        val scalaJSSettings: Configure =
          if (platform == VirtualAxis.js) configureScalaJSProject else identity

        val ce3VersionOverride: Configure =
          if (catsEffectAxis == CatsEffect3Axis)
            _.settings(versionOverrideForCE3)
          else identity

        val configureProject =
          addScalafix andThen addScalafmt andThen scalaJSSettings andThen ce3VersionOverride

        projectMatrix.defaultAxes(defaults: _*).customRow(
          scalaVersions = List(scalaVersion),
          axisValues = Seq(catsEffectAxis, platform),
          configureProject
        )
    }

    def add(
        scalaVersions: Iterable[String],
        platform: VirtualAxis.PlatformAxis,
        catsEffectAxis: CatsEffectAxis): ConfigureX = {
      scalaVersions.map(addOne(_, platform, catsEffectAxis)).reduce(_ andThen _)
    }
    def full = sparse(true, true, true)

    def sparse(
        withCE3: Boolean,
        withJS: Boolean,
        withScala3: Boolean
    ): ProjectMatrix = {
      val defaultScalaVersions = supportedScala2Versions
      val defaultPlatform      = List(VirtualAxis.jvm)
      val defaultCE            = List(CatsEffect2Axis)

      val addJs     = if (withJS) List(VirtualAxis.js) else Nil
      val addScala3 = if (withScala3) List(scala3) else Nil
      val addCE3    = if (withCE3) List(CatsEffect3Axis) else Nil

      val configurators = for {
        scalaVersion <- defaultScalaVersions ++ addScala3
        platform     <- defaultPlatform ++ addJs
        catsEffect   <- defaultCE ++ addCE3
      } yield addOne(scalaVersion, platform, catsEffect)

      val configure: ConfigureX = configurators.reduce(_ andThen _)

      configure(pmx)
    }

  }

  lazy val versionOverrideForCE3: Seq[Def.Setting[_]] = Seq(
    version := {
      val regex = "^(\\d+).(\\d+).(\\d+).*$".r

      val original = version.value

      original match {
        case regex(major, minor, patch) =>
          original.replaceFirst(s"$major.$minor.$patch",
                                s"$major.${minor.toInt + 1}.$patch")
        case _ =>
          throw new RuntimeException(
            s"Version $original doesn't match SemVer format")
      }
    }
  )

  def configureScalaJSProject(proj: Project): Project = {

    val linkerConfig = Seq(Test / scalaJSLinkerConfig ~= {
      _.withModuleKind(ModuleKind.CommonJSModule)
    })

    // on CI, use linker's batch mode:
    // https://github.com/scala-js/scala-js/blob/6622d0b8f99bec4dbe1b29c125d111fdea246d34/linker-interface/shared/src/main/scala/org/scalajs/linker/interface/StandardConfig.scala#L51
    // When you run a lot of linkers in parallel
    // they will retain intermediate state (in case you want incremental compilation)
    // on CI we don't want that
    val batchOnCi =
      if (sys.env.contains("CI")) Seq(scalaJSLinkerConfig ~= {
        _.withBatchMode(true)
      })
      else Seq.empty

    proj.enablePlugins(ScalaJSPlugin)
      .settings((linkerConfig ++ batchOnCi): _*)
      .settings(
        Test / fork := false
      )
  }

  override def requires = plugins.JvmPlugin
  override def trigger  = allRequirements

  lazy val scala212               = "2.12.13"
  lazy val scala213               = "2.13.6"
  lazy val scala3                 = "3.0.0-RC2"
  lazy val supportedScalaVersions = List(scala212, scala213, scala3)

  lazy val supportedScala2Versions = List(scala212, scala213)

  /** @see [[sbt.AutoPlugin]] */
  override val projectSettings = Seq(
    moduleName := s"weaver-${name.value}",
    scalacOptions ++= compilerOptions(scalaVersion.value),
    Test / scalacOptions ~= (_ filterNot (_ == "-Xfatal-warnings")),
    // Turning off fatal warnings for ScalaDoc, otherwise we can't release.
    Compile / doc / scalacOptions ~= (_ filterNot (_ == "-Xfatal-warnings")),
    // ScalaDoc settings
    autoAPIMappings := true,
    ThisBuild / scalacOptions ++= {
      if (!(ThisBuild / scalacOptions).value.contains("-sourcepath"))
        Seq(
          // Note, this is used by the doc-source-url feature to determine the
          // relative path of a given source file. If it's not a prefix of a the
          // absolute path of the source file, the absolute path of that file
          // will be put into the FILE_SOURCE variable, which is
          // definitely not what we want.
          "-sourcepath",
          file(".").getAbsolutePath.replaceAll("[.]$", "")
        )
      else Seq.empty
    },
    // https://github.com/sbt/sbt/issues/2654
    incOptions := incOptions.value.withLogRecompileOnMacro(false),
    // https://scalacenter.github.io/scalafix/docs/users/installation.html
    semanticdbEnabled := !scalaVersion.value.startsWith("3.0"),
    semanticdbVersion := scalafixSemanticdb.revision,
    libraryDependencies ++= {
      if (scalaVersion.value.startsWith("3.")) Seq.empty
      else Seq(
        compilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1"),
        compilerPlugin(
          "org.typelevel" %% "kind-projector" % "0.13.0" cross CrossVersion.full
        )
      )
    },
    pushRemoteCacheTo := Some(MavenCache("local-cache",
                                         file("/tmp/remote-cache")))
  ) ++ coverageSettings ++ publishSettings

  def artifactName(nm: String, axes: Seq[VirtualAxis]) = {
    nm + axes.sortBy[Int] {
      case _: VirtualAxis.ScalaVersionAxis => 0
      case _: VirtualAxis.PlatformAxis     => 1
      case _: VirtualAxis.StrongAxis       => 2
      case _: VirtualAxis.WeakAxis         => 3
    }.map(_.idSuffix).mkString("-", "-", "")
  }

  lazy val remoteCacheSettings = Seq(
    Compile / packageCache / moduleName := artifactName(
      moduleName.value,
      virtualAxes.value)
  )

  def compilerOptions(scalaVersion: String) = {
    val allowed =
      if (scalaVersion.startsWith("3."))
        commonCompilerOptions.filterNot(flg =>
          flg.contains("explaintypes") || flg.contains(
            "-Xlint") || flg.contains(
            "-Ywarn-") || flg.contains("-Xcheckinit"))
      else commonCompilerOptions

    allowed ++ {
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
  val simpleLayout: Seq[Setting[_]] = {
    /*
      Project matrix will override baseDirectory, making it look like this:
      /Users/velvetbaldmime/Personal/weaver-test/.sbt/matrix/src

      Which means we can't use it to identify sources layout.

      Instead, we're going to use `scalaSource` and go 3 levels up from it:

      sbt:root> show catsJS/scalaSource
      [info] .../modules/framework/cats/src/main/scala
     */
    val moduleBase =
      Def.setting((Compile / scalaSource).value.getParentFile().getParentFile().getParentFile())

    def suffixes(axes: Seq[VirtualAxis]) = axes.collect {
      case VirtualAxis.js  => List("", "-js")
      case VirtualAxis.jvm => List("", "-jvm")
      case CatsEffect3Axis => List("", "-ce3")
      case CatsEffect2Axis => List("", "-ce2")
      case ScalaVersionAxis(ver, _) =>
        if (ver.startsWith("3.")) List("", "-scala-3")
        else List("", "-scala-2")
    }.toList

    def sequence[A](ll: List[List[A]]): List[List[A]] =
      ll.foldRight(List(List.empty[A])) {
        case (listA, listListA) =>
          listA.flatMap(a => listListA.map(a :: _))
      }

    def combos(axes: Seq[VirtualAxis]): List[String] =
      sequence(suffixes(axes)).map(_.mkString("src", "", ""))

    Seq(
      Compile / unmanagedSourceDirectories :=
        combos(virtualAxes.value).map(moduleBase.value / _),
      Test / unmanagedSourceDirectories :=
        combos(virtualAxes.value).map(moduleBase.value / "test" / _),
      Test / unmanagedResourceDirectories := Seq(
        moduleBase.value / "test" / "resources"
      ),
      Test / fork := (virtualAxes.value.contains(VirtualAxis.jvm))
    ) ++ remoteCacheSettings
  }

  lazy val publishSettings = Seq(
    organization := "com.disneystreaming",
    version := sys.env.get("GITHUB_REF")
      .filter(_.startsWith("refs/tags/v"))
      .map(_.drop("refs/tags/v".length))
      .getOrElse(version.value),
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

  def createBuildCommands(projects: Seq[ProjectReference]) = {
    case class Triplet(ce: String, scala: String, platform: String)

    val scala3Suffix   = VirtualAxis.scalaABIVersion(scala3).idSuffix
    val scala213Suffix = VirtualAxis.scalaABIVersion(scala213).idSuffix
    val scala212Suffix = VirtualAxis.scalaABIVersion(scala212).idSuffix
    val jsSuffix       = VirtualAxis.js.idSuffix
    val ce3Suffix      = CatsEffect3Axis.idSuffix
    val ce2Suffix      = CatsEffect2Axis.idSuffix

    val all: List[(Triplet, Seq[String])] =
      projects.collect {
        case lp: LocalProject =>
          var projectId = lp.project

          val scalaAxis =
            if (projectId.endsWith(scala3Suffix)) {
              projectId = projectId.dropRight(scala3Suffix.length)
              "3_0"
            } else if (projectId.endsWith(scala212Suffix)) {
              projectId = projectId.dropRight(scala212Suffix.length)
              "2_12"
            } else
              "2_13"

          val platformAxis =
            if (projectId.endsWith(jsSuffix)) {
              projectId = projectId.dropRight(jsSuffix.length)

              "js"
            } else "jvm"

          val ceAxis =
            if (projectId.endsWith(ce3Suffix)) {
              projectId = projectId.dropRight(ce3Suffix.length)
              "CE3"
            } else "CE2"

          Triplet(ceAxis, scalaAxis, platformAxis) -> lp.project
      }.groupBy(_._1).mapValues(_.map(_._2)).toList

    // some commands, like test and compile, are setup for all modules
    val any = (t: Triplet) => true
    // things like scalafix and scalafmt are only enabled on jvm 2.13 projects
    val jvm2_13 = (t: Triplet) => t.scala == "2_13" && t.platform == "jvm"

    val desiredCommands: Map[String, (String, Triplet => Boolean)] = Map(
      "test"            -> ("test", any),
      "compile"         -> ("compile", any),
      "publishLocal"    -> ("publishLocal", any),
      "pushRemoteCache" -> ("pushRemoteCache", any),
      "scalafix"        -> ("scalafix --check", jvm2_13),
      "scalafixTests"   -> ("Test/scalafix --check", jvm2_13),
      "scalafmt"        -> ("scalafmtCheckAll", jvm2_13)
    )

    val cmds = all.flatMap {
      case (triplet, projects) =>
        desiredCommands.filter(_._2._2(triplet)).map { case (name, (cmd, _)) =>
          Command.command(
            s"${name}_${triplet.ce}_${triplet.scala}_${triplet.platform}") {
            state =>
              projects.foldLeft(state) { case (st, proj) =>
                s"$proj/$cmd" :: st
              }
          }
        }
    }

    cmds
  }

}
