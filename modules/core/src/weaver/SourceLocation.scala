package weaver

import java.nio.file.Path
import java.nio.file.Paths

// kudos to https://github.com/monix/minitest
final case class SourceLocation(
    filePath: String,
    fileRelativePath: String,
    line: Int
) {
  def fileName: Option[String] = filePath.split("/").lastOption
}

object SourceLocation extends SourceLocationMacro

object SourceLocationHelper {

  private[weaver] def sourcePath(settings: List[String]): Option[Path] = {
    val sourcePathSettingIndex = settings.indexOf("-sourcepath")
    if (sourcePathSettingIndex >= 0 && sourcePathSettingIndex < (settings.size - 1)) {
      Some(settings(sourcePathSettingIndex + 1)).flatMap { s =>
        try {
          Some(Paths.get(s))
        } catch {
          case _: Throwable => None
        }
      }
    } else None
  }

}
