import $ivy.`com.lihaoyi::requests:0.6.5`
import $ivy.`com.lihaoyi::upickle:0.9.5`

import scala.jdk.CollectionConverters._
import java.nio.file.attribute.PosixFilePermission.OWNER_EXECUTE

// format: off

// Assumes mdoc has been run.
@main
def main(): Unit = {

  if(sys.env.get("GITHUB_DEPLOY_KEY").nonEmpty) doInstallSSH()

  if (!os.exists(os.pwd / "modules" / "docs" / "target" / "jvm-2.13" / "mdoc"))
    sys.error("Have you run mdoc ?")

  val website = os.pwd / "website"
  val versionedDocs = "versioned_docs"
  val versionedSidebars = "versioned_sidebars"
  val versionsJson = "versions.json"

  // Cleanup previous runs
  if(os.exists(website / versionedDocs)) os.remove.all(website / versionedDocs)
  if(os.exists(website / versionedSidebars)) os.remove.all(website / versionedSidebars)
  if(os.exists(website / versionsJson)) os.remove(website / versionsJson)
  if(os.exists(website / "pages" / "en" / "versions.js")) os.remove(website / "pages" / "en" / "versions.js")

  website.yarn("install")
  // Freezing version

  val version = sys.env.get("GITHUB_REF")
      .filter(_.startsWith("refs/tags/v"))
      .map(_.drop("refs/tags/v".length))
      .getOrElse(throw new Exception("GITHUB REF doesn't contain a tag!"))
  val siteConfig  = ujson.read(os.read(website / "siteConfig.json"))
  val orgName     = siteConfig("organizationName").str
  val projectName = siteConfig("projectName").str
  val redirectUrl = projectName + "/index.html"
  val html        = redirectHtml(redirectUrl)

  val currentBranch = git("rev-parse", "--abbrev-ref", "HEAD")
  val currentCommit = git("rev-parse", "HEAD")

  val siteBranch        = "gh-pages"
  val frozenDocsBranch  = "frozen-docs"
  val githubHost       = "github.com"
  val remote           = s"git@$githubHost:$orgName/$projectName.git"

  val currentRepoURL = git("config", "--get", "remote.origin.url")

  // Restoring frozen docs
  log("Restoring frozen docs")
  val frozenDocs = os.pwd / "target" / frozenDocsBranch


  // Dirty hack to avoid docusaurusCreatePages crashing in branch builds
  os.copy.over(website / "pages" / "en" / "versions.js_", website / "pages" / "en" / "versions.js")

  if (os.exists(frozenDocs)) os.remove.all(frozenDocs)

  git("clone", "--depth", "1", "--branch", frozenDocsBranch, remote, frozenDocs.toString())
  if(os.exists(frozenDocs / versionedDocs)) os.copy.over(frozenDocs / versionedDocs, website / versionedDocs)
  if(os.exists(frozenDocs / versionedSidebars)) os.copy.over(frozenDocs / versionedSidebars, website / versionedSidebars)
  if(os.exists(frozenDocs / versionsJson)) os.copy.over(frozenDocs / versionsJson, website / versionsJson)

  // Freezing current version
  log("Freezing current version")
  website.yarn("run", "version", version)

  // Caching frozen docs
  os.copy.over(website / versionedDocs, frozenDocs / versionedDocs)
  os.copy.over(website / versionedSidebars, frozenDocs / versionedSidebars)
  os.copy.over(website / versionsJson, frozenDocs / versionsJson)

  log(s"pushing to $frozenDocsBranch")
  frozenDocs.git("add", "*")
  frozenDocs.git("commit", "-m", s"Pushing docs for $version")
  frozenDocs.git("push", "origin", frozenDocsBranch)

  log("building site")
  website.yarn("run", "build")
  val build = website / "build"
  os.write(build / "index.html", html)

  log(s"cloning $siteBranch")
  val cloneDir = build / siteBranch
  if (os.exists(cloneDir)) os.remove.all(cloneDir)
  build.git("clone","--depth", "1", "--branch", siteBranch, remote, cloneDir.toString())
  if (os.list(cloneDir).filterNot(_.last == ".git").nonEmpty) cloneDir.git("rm", "-rf", ".")

  val from = build / projectName
  val to = cloneDir

  os.list(from).filterNot(_.baseName == ".DS_Store").filterNot(_ == cloneDir).foreach(os.copy.into(_, cloneDir))
  cloneDir.git("add", "*")
  cloneDir.git("commit", "-m", "Deploy website", "-m", s"Deploy website version based on $currentCommit")

  log(s"pushing to $siteBranch")
  cloneDir.git("push", "origin", siteBranch)

  log("Done !")
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

def log(s: String) = System.err.println(s"[INFO] $s")

val installSSHScript: String =
    """|#!/usr/bin/env bash
         |
         |set -eu
         |
         |set-up-ssh() {
         |  echo "Setting up ssh..."
         |  mkdir -p $HOME/.ssh
         |  ssh-keyscan -t rsa github.com >> ~/.ssh/known_hosts
         |  git config --local user.name "Docusaurus bot"
         |  git config --local user.email "${MDOC_EMAIL:-mdoc@docusaurus}"
         |  git config --local push.default simple
         |  DEPLOY_KEY_FILE=$HOME/.ssh/id_rsa
         |  echo "$GITHUB_DEPLOY_KEY" | base64 --decode > ${DEPLOY_KEY_FILE}
         |  chmod 600 ${DEPLOY_KEY_FILE}
         |  eval "$(ssh-agent -s)"
         |  ssh-add ${DEPLOY_KEY_FILE}
         |}
         |DEPLOY_KEY=${GITHUB_DEPLOY_KEY:-}
         |
         |if [[ -n "$DEPLOY_KEY" ]]; then
         |  set-up-ssh
         |fi
      """.stripMargin

def doInstallSSH() =  {
  val tmp = os.temp(contents = installSSHScript,
                    dir = os.pwd,
                    prefix = "docusaurus",
                    suffix = "install_ssh.sh")
  os.proc.apply(Seq("chmod", "+x", tmp.toString))
  os.proc.apply(Seq("bash", tmp.toString)).call(cwd = os.pwd)
}
