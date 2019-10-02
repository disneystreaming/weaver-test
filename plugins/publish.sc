import java.math.BigInteger
import java.security.MessageDigest

import $file.gitVersion
import ammonite.ops._
import mill._
import mill.api.Logger
import mill.define.Task
import mill.scalalib._
import mill.scalalib.publish.{ SonatypeHttpApi, _ }

trait WeaverPublishModule extends PublishModule
with gitVersion.GitVersionModule {

  def publishVersion = T { gitVersion() }

  override def sonatypeUri = WeaverPublishModule.uri

  override def sonatypeSnapshotUri = WeaverPublishModule.uri

  def pomSettings = PomSettings(
    description = "A test framework for sane testing",
    organization = "com.disneystreaming.oss",
    url = "https://github.bamtech.co/oss/weaver-test",
    licenses = Seq(),
    versionControl =
      VersionControl(Some("https://github.bamtech.co/oss/weaver-test")),
    developers = Seq()
  )

}

object WeaverPublishModule extends Module {
  val uri = "https://artifactory.us-east-1.bamgrid.net/artifactory/oss-maven/"

  def publishAll(
      username: String,
      password: String,
      publishArtifacts: mill.main.Tasks[PublishModule.PublishData]
  ) = T.command {

    val x: Seq[(Seq[(os.Path, String)], Artifact)] =
      Task.sequence(publishArtifacts.value)().map {
        case PublishModule.PublishData(a, s) =>
          (s.map { case (p, f) => (p.path, f) }, a)
      }
    new ArtifactoryPublisher(
      uri,
      uri,
      s"$username:$password",
      T.ctx().log
    ).publishAll(
      true,
      x: _*
    )
  }
}

// Basically a copy of https://github.com/lihaoyi/mill/blob/0.2.2/scalalib/src/mill/scalalib/publish/SonatypePublisher.scala
// to avoid requiring a gpg passphrase.
// Remove when https://github.com/lihaoyi/mill/issues/345 is resolved.
class ArtifactoryPublisher(
    uri: String,
    snapshotUri: String,
    credentials: String,
    log: Logger) {

  private val api = new SonatypeHttpApi(uri,
                                        credentials,
                                        readTimeout = 60000,
                                        connectTimeout = 5000)

  def publish(fileMapping: Seq[(Path, String)], artifact: Artifact): Unit = {
    publishAll(release = true, fileMapping -> artifact)
  }
  def publishAll(
      release: Boolean,
      artifacts: (Seq[(Path, String)], Artifact)*): Unit = {

    val mappings = for ((fileMapping0, artifact) <- artifacts) yield {
      val publishPath = Seq(
        artifact.group.replace(".", "/"),
        artifact.id,
        artifact.version
      ).mkString("/")
      val fileMapping = fileMapping0.map {
        case (file, name) => (file, publishPath + "/" + name)
      }

      artifact -> fileMapping.flatMap {
        case (file, name) =>
          val content = read.bytes(file)

          Seq(
            name             -> content,
            (name + ".md5")  -> md5hex(content),
            (name + ".sha1") -> sha1hex(content)
          )
      }
    }

    val (snapshots, releases) = mappings.partition(_._1.isSnapshot)
    if (snapshots.nonEmpty) {
      doPublish(snapshots.flatMap(_._2), snapshots.map(_._1), snapshotUri)
    }
    val releaseGroups = releases.groupBy(_._1.group)
    for ((group, groupReleases) <- releaseGroups) {
      doPublish(groupReleases.flatMap(_._2), releases.map(_._1), uri)
    }
  }

  private def doPublish(
      payloads: Seq[(String, Array[Byte])],
      artifacts: Seq[Artifact],
      uri: String
  ): Unit = {

    val publishResults = payloads.map {
      case (fileName, data) =>
        log.info(s"Uploading $fileName")
        val resp = api.upload(s"$uri/$fileName", data)
        resp
    }
    reportPublishResults(publishResults, artifacts)
  }

  private def reportPublishResults(
      publishResults: Seq[requests.Response],
      artifacts: Seq[Artifact]
  ) = {
    if (publishResults.forall(_.is2xx)) {
      log.info(s"Published ${artifacts.map(_.id).mkString(", ")} to Sonatype")
    } else {
      val errors = publishResults.filterNot(_.is2xx).map { response =>
        s"Code: ${response.statusCode}, message: ${response.text}"
      }
      throw new RuntimeException(
        s"Failed to publish ${artifacts.map(_.id).mkString(", ")} to Sonatype. Errors: \n${errors.mkString("\n")}"
      )
    }
  }

  private def md5hex(bytes: Array[Byte]): Array[Byte] =
    hexArray(md5.digest(bytes)).getBytes

  private def sha1hex(bytes: Array[Byte]): Array[Byte] =
    hexArray(sha1.digest(bytes)).getBytes

  private def md5 = MessageDigest.getInstance("md5")

  private def sha1 = MessageDigest.getInstance("sha1")

  private def hexArray(arr: Array[Byte]) =
    String.format("%0" + (arr.length << 1) + "x", new BigInteger(1, arr))

}
