package weaver

sealed abstract class Platform(val name: String)

object Platform {
  def isJVM: Boolean    = PlatformCompat.platform == JVM
  def isJS: Boolean     = PlatformCompat.platform == JS
  def isNative: Boolean = PlatformCompat.platform == Native
  def isScala3: Boolean = ScalaCompat.isScala3

  case object JS     extends Platform("js")
  case object JVM    extends Platform("jvm")
  case object Native extends Platform("native")
}
