package weaver

private[weaver] object FSCompat {

  def wd: String = java.nio.file.Paths.get("").toAbsolutePath.toString()

  def bestEffortPath(
      fileName: Option[String],
      filePath: Option[String]): Option[String] = {
    filePath.flatMap {
      case pathString =>
        val pwd  = java.nio.file.Paths.get("").toAbsolutePath
        val path = java.nio.file.Paths.get(pathString).toAbsolutePath()
        if (path.startsWith(pwd)) Some(pwd.relativize(path).toString())
        else fileName
    }
  }
}
