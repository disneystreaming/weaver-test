import $ivy.`com.lihaoyi::requests:0.6.5`
import $ivy.`com.lihaoyi::upickle:0.9.5`

// Assumes static html files have been built, pushes to gh-pages branch
@main
def main(): Unit = {

  val website = os.pwd / "website"

  val gitUser       = sys.env("GIT_USER")
  val currentBranch = git("rev-parse", "--abbrev-ref", "HEAD")

  val siteConfig  = ujson.read(os.read(website / "siteConfig.json"))
  val orgName     = siteConfig("organizationName").str
  val projectName = siteConfig("projectName").str

  val deploymentBranch = "gh-pages"
  val githubHost       = "github.com"
  val remoteBranch     = s"git@$githubHost:$orgName/$projectName.git"

  val currentRepoURL = git("config", "--get", "remote.origin.url")
  if (currentBranch == deploymentBranch) {
    sys.error(s"Cannot deploy from $deploymentBranch, only to it")
  }

  val currentCommit = git("rev-parse", "HEAD")

  val build        = website / "build"
  val cloneDirName = s"$projectName-$deploymentBranch"
  build.git("clone", remoteBranch, s"$projectName-$deploymentBranch")
  val cloneDir = build / cloneDirName
  cloneDir.git("checkout", s"origin/$deploymentBranch")

  // TODO remove everything but versioning here

  val from = build / projectName

}

def git(args: String*): String =
  os.proc("git" :: args.toList).call().out.text()

implicit class PathOps(path: os.Path) {
  def git(args: String*): String =
    os.proc("git" :: args.toList).call(path).out.text()
}
