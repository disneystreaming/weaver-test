import $ivy.`com.lihaoyi::requests:0.6.5`
import $ivy.`com.lihaoyi::upickle:0.9.5`

@main
def release(
    uploadedFile: os.Path,
    tagName: String = sys.env("DRONE_TAG"),
    authKey: String = sys.env("GITHUB_TOKEN")): String = {

  val uploadName: String = "weaver-intellij.zip"
  println("upload.apply")
  println(uploadedFile)
  println(tagName)
  println(uploadName)
  println(authKey)
  val body = requests.get(
    "https://api.github.com/repos/disneystreaming/weaver-test/releases/tags/" + tagName,
    headers = Seq("Authorization" -> s"token $authKey")
  )

  val parsed = ujson.read(body.text)

  println(body)

  val snapshotReleaseId = parsed("id").num.toInt

  val uploadUrl =
    s"https://uploads.github.com/repos/disneystreaming/weaver-test/releases/" +
      s"$snapshotReleaseId/assets?name=$uploadName"

  val res = requests.post(
    uploadUrl,
    headers = Seq(
      "Content-Type"  -> "application/octet-stream",
      "Authorization" -> s"token $authKey"
    ),
    connectTimeout = 5000,
    readTimeout = 60000,
    data = os.read.bytes(uploadedFile)
  ).text

  println(res)

  val longUrl = ujson.read(res)("browser_download_url").str

  longUrl

  // assumes pwd is the root of the project
  val repoTemplate =
    os.read(os.pwd / "modules" / "intellij" / "updatePlugins_template.xml")

  val repo = repoTemplate.replace("{url_template}", longUrl).replace(
    "{version}",
    tagName)

  // writes intellij repo data. Doesn't upload to github as it's done during
  // docusaurus publish.
  os.write.over(os.pwd / "website" / "static" / "intellij.xml",
                repo,
                createFolders = true)

  longUrl
}
