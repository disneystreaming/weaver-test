import WeaverPlugin.compilerOptions
import sbt._
import org.jetbrains.sbtidea.Keys._

// We need to have the same version as org.intellij.scala
val scalaPluginScalaVersion = "2.12.7"

lazy val plugin = (project in file("plugin"))
  .dependsOn(
    ideaScala % Configurations.Provided,
  )
  .enablePlugins(SbtIdeaPlugin)
  .disablePlugins(WeaverPlugin)
  .settings(
    scalaVersion := scalaPluginScalaVersion,
    version := "2020.2.2",
    intellijPluginName := "weaver-intelliJ",
    intellijBuild := "202.6397.94",
    intellijPlatform := IntelliJPlatform.IdeaUltimate,
    packageMethod := PackagingMethod.Standalone(),
    packageAdditionalProjects := Seq(runner_2_12, runner_2_13),
    SettingKey[Seq[String]]("ide-base-packages") := Seq(
      "org.jetbrains.plugins.scala.testingSupport.test"
    ),
    SettingKey[Option[File]]("ide-output-directory") := Some(
      file("out/production")
    ),
    packageLibraryMappings := Seq.empty, // allow scala-library
    patchPluginXml := pluginXmlOptions { xml =>
      xml.version = version.value
      xml.sinceBuild = "202.0"
      xml.untilBuild = "203.0"
    },
    intellijPlugins += "org.intellij.scala:2020.2.23".toPlugin,
    unmanagedSourceDirectories in Compile += baseDirectory.value / "src",
    unmanagedSourceDirectories in Test += baseDirectory.value / "test",
    unmanagedResourceDirectories in Compile += baseDirectory.value / "resources",
    unmanagedResourceDirectories in Test += baseDirectory.value / "testResources",
    projectDependencies ++= Seq(
//      crossPaths := false,
     "com.novocode" % "junit-interface" % "0.11" % "test"
    ),
    testOptions in Test := Seq(Tests.Argument(TestFrameworks.JUnit, "-a")),
    libraryDependencies ++= Seq(
      "com.novocode" % "junit-interface" % "0.11" % Test
    ),
    scalacOptions ++= WeaverPlugin.compilerOptions(scalaVersion.value)
  )

lazy val runner_2_13 = (project in file("runner")).settings(
  runnerSettings(WeaverPlugin.scala213) :_*
)

lazy val runner_2_12 = (project in file("runner")).settings(
  runnerSettings(WeaverPlugin.scala212) :_*
)

def runnerSettings(scalaV:String) = List(
  scalaVersion := scalaV,
  packageMethod := PackagingMethod.Standalone(static = true),
  unmanagedSourceDirectories in Compile += baseDirectory.value / "src",
  unmanagedSourceDirectories in Test += baseDirectory.value / "test",
  unmanagedResourceDirectories in Compile += baseDirectory.value / "resources",
  unmanagedResourceDirectories in Test += baseDirectory.value / "testResources",
  libraryDependencies ++= Seq(
    "com.disneystreaming" %% "weaver-core" % "0.4.1"
  ),
  target := file(s"target/runner_$scalaV")
)

lazy val ideaScala = (project in file("intellij-scala/scala"))
  .enablePlugins(SbtIdeaPlugin)
  .settings(
    scalaVersion := scalaPluginScalaVersion,
    packageMethod := PackagingMethod.Skip(),
  managedSourceDirectories in Compile ++=
    List(
      baseDirectory.value / "scala-api/src",
      baseDirectory.value / "scala-impl/src",
      baseDirectory.value / "runners/src",
    )
).disablePlugins(WeaverPlugin)


lazy val runnerProject = createRunnerProject(plugin, "weaver-runner")