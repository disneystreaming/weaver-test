import $ivy.`com.lihaoyi::requests:0.6.5`
import $ivy.`com.lihaoyi::upickle:0.9.5`

// format: off

// Assumes mdoc has been run, pushes to gh-pages branch
@main
def main(): Unit = {

  if (!os.exists(os.pwd / "docs" / "target" / "mdoc"))
    sys.error("Have you run mdoc ?")

  val website = os.pwd / "website"

  website.yarn("install")
  // Freezing version

  val version     = sys.env("DRONE_TAG").dropWhile(_ == 'v')
  val siteConfig  = ujson.read(os.read(website / "siteConfig.json"))
  val orgName     = siteConfig("organizationName").str
  val projectName = siteConfig("projectName").str
  val redirectUrl = projectName + "/index.html"
  val html        = redirectHtml(redirectUrl)

  val currentBranch = git("rev-parse", "--abbrev-ref", "HEAD")
  val currentCommit = git("rev-parse", "HEAD")

  val siteBranch        = "gh-pages"
  val githubHost       = "github.com"
  val remote           = s"git@$githubHost:$orgName/$projectName.git"

  val currentRepoURL = git("config", "--get", "remote.origin.url")

  // Restoring frozen docs
  val frozenDocs = os.pwd / "target" / "frozen-docs"
  git("clone", "-–depth", "1", "--branch", "frozen-docs", remote, frozenDocs.toString())
  if(os.exists(frozenDocs / "versioned_docs")) os.copy(frozenDocs / "versioned_docs", website)
  if(os.exists(frozenDocs / "versioned_sidebars")) os.copy.into(frozenDocs / "versioned_sidebars", website)
  if(os.exists(frozenDocs / "versions.json")) os.copy.into(frozenDocs / "versions.json", website)

  // Freezing current version
  website.yarn("run", "version", version)

  // Caching frozen docs
  os.copy.into(website / "versioned_docs", frozenDocs, replaceExisting = true)
  os.copy.into(website / "versioned_sidebars", frozenDocs, replaceExisting = true)
  os.copy.into(website / "versions.json", frozenDocs, replaceExisting = true)

  println("pushing frozen docs")
  // TODO frozenDocs.git("push")

  website.yarn("run", "build")
  val build = website / "build"

  val cloneDir = build / siteBranch
  build.git("clone","-–depth", "1", "--branch", siteBranch, remote, siteBranch)
  cloneDir.git("rm", "-rf", ".")

  val from = build / projectName
  val to = cloneDir

  os.list(from).filterNot(_.baseName == ".DS_Store").filterNot(_ == cloneDir)
  cloneDir.git("commit", "-m", "Deploy website", "-m", s"Deploy website version based on $currentCommit")

  println("pushing to gh-pages")



}

def git(args: String*): String =
  os.proc("git" :: args.toList).call().out.text()

implicit class PathOps(path: os.Path) {
  def git(args: String*): String =
    os.proc("git" :: args.toList).call(path).out.text()

  def yarn(args: String*): String =
    os.proc("yarn" :: args.toList).call(path).out.text()
}

def redirectHtml(url: String): String = {
  s"""
       |<!DOCTYPE HTML>
       |<html lang="en-US">
       |    <head>
       |        <meta charset="UTF-8">
       |        <meta http-equiv="refresh" content="0; url=$url">
       |        <script type="text/javascript">
       |            window.location.href = "$url"
       |        </script>
       |        <title>Page Redirection</title>
       |    </head>
       |    <body>
       |        If you are not redirected automatically, follow this <a href='$url'>link</a>.
       |    </body>
       |</html>
      """.stripMargin
}
