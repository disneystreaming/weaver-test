import ammonite.ops._
import mill._
import mill.define.{ Sources, Task }

import scala.util.Try

// Credit to Dale Wijnand for the regexes (see https://github.com/dwijnand/sbt-dynver)
trait GitVersionModule extends Module {

  val DRONE_TAG = "DRONE_TAG"

  def gitVersion: T[String] = T.input {
    implicit val path: Path = pwd

    T.ctx.env.get(DRONE_TAG) match {
      case Some(tag) =>
        parseVersion(tag)
      case None =>
        val describeResult = %%(
          "git",
          "describe",
          "--long",
          "--tags",
          "--abbrev=8",
          "--match",
          "v[0-9]*",
          "--always",
          "--dirty=+dirty"
        ).out.string.replaceAll("-([0-9]+)-g([0-9a-f]{8})", "+$1-$2")
        parseVersion(describeResult)
    }
  }

  def masterBranch: T[String] = "master"

  def latestTag: T[Option[String]] = T {
    implicit val path: Path = pwd
    val branch              = masterBranch()
    T.ctx.env.get(DRONE_TAG).orElse {
      Try(%%("git", "describe", branch, "--abbrev=0", "--tags").out.lines.head).toOption
    }
  }

  def isSemVer: T[Boolean] = T {
    gitVersion().matches(SemVer)
  }

  protected val Tag         = """v([0-9][^+]*)""".r
  protected val Distance    = """\+([0-9]+)""".r
  protected val Sha         = """([0-9a-f]{8})""".r
  protected val Untagged    = s"""($Distance-$Sha)""".r
  protected val DirtySuffix = """(\+dirty)""".r
  protected val SemVer      = """^[0-9]*.[0-9]*.[0-9]*$$"""

  protected val TagRegex  = s"""^$Tag$Untagged?$DirtySuffix?$$""".r
  protected val ShaRegex  = s"""^$Sha$DirtySuffix?$$""".r
  protected val HeadRegex = s"""^HEAD$DirtySuffix$$""".r

  object distanceToTag {
    def unapply(s: String): Option[Int] =
      Option(s).flatMap(s => Try(s.toInt).toOption).orElse(Some(0))
  }

  object maybeSha {
    def unapply(s: String): Option[String] = Option(s).orElse(Some(""))
  }

  object nonCommittedCode {
    def unapply(s: String): Option[Boolean] = Some(Option(s).isDefined)
  }

  def parseVersion(s: String): String = s.trim match {
    case TagRegex(tag,
                  _,
                  distanceToTag(dist),
                  maybeSha(sha),
                  nonCommittedCode(dirty)) =>
      version(Some(tag), dist, sha, dirty)
    case ShaRegex(maybeSha(sha), nonCommittedCode(dirty)) =>
      version(None, 0, sha, dirty)
    case HeadRegex(nonCommittedCode(dirty)) =>
      version(None, 0, "", dirty)
  }

  def version(
      maybeTag: Option[String],
      distance: Int,
      sha: String,
      dirty: Boolean): String = {
    val untagged    = if (sha.isEmpty) "" else s"+$distance-$sha"
    val tagged      = if (distance > 0) s"+$distance-$sha" else ""
    val version     = maybeTag.fold(s"0.0.0$untagged")(tag => s"$tag$tagged")
    val dirtySuffix = if (dirty) "-SNAPSHOT" else ""
    s"$version$dirtySuffix"
  }

}
