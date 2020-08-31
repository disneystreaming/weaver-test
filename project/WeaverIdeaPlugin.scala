import org.jetbrains.sbtidea.Keys._
import org.jetbrains.sbtidea.SbtIdeaPlugin
import org.jetbrains.sbtidea.packaging.PackagingKeys._
import sbt.Keys._
import sbt._

object WeaverIdeaPlugin extends AutoPlugin {

  // We need to have the same version as org.intellij.scala
  val scalaPluginScalaVersion = "2.12.7"

  override def trigger = allRequirements

  override def requires: Plugins = SbtIdeaPlugin

  override def extraProjects: Seq[Project] = Seq(ideaScala)

  override def buildSettings: Seq[Def.Setting[_]] = Seq(
    intellijBuild in ThisBuild := "202.6948.69"
  )

  override def projectSettings: Seq[Def.Setting[_]] = Seq(
    scalaVersion := scalaPluginScalaVersion,
    version := "2020.2.2",
    intellijPluginName := "weaver-intelliJ",
    intellijPlatform := IntelliJPlatform.IdeaUltimate,
    packageMethod := PackagingMethod.Standalone(),
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

  override def projectConfigurations: Seq[Configuration] =
    super.projectConfigurations

  def runnerSettings(scalaV: String) = List(
    scalaVersion := scalaV,
    packageMethod := PackagingMethod.Standalone(static = true),
    unmanagedSourceDirectories in Compile += baseDirectory.value / "src",
    unmanagedSourceDirectories in Test += baseDirectory.value / "test",
    unmanagedResourceDirectories in Compile += baseDirectory.value / "resources",
    unmanagedResourceDirectories in Test += baseDirectory.value / "testResources",
    libraryDependencies ++= Seq(
      "com.disneystreaming" %% "weaver-core" % "0.4.1"
    ),
    target := file(s"target/ideaRunner_$scalaV")
  )

  lazy val ideaScala =
    Project("intellij-scala", file("modules/intellij/intellij-scala/scala"))
      .settings(
        scalaVersion := WeaverIdeaPlugin.scalaPluginScalaVersion,
        packageMethod := PackagingMethod.Skip(),
        managedSourceDirectories in Compile ++=
          List(
            baseDirectory.value / "scala-api/src",
            baseDirectory.value / "scala-impl/src",
            baseDirectory.value / "runners/src"
          )
      ).disablePlugins(WeaverPlugin)

}
