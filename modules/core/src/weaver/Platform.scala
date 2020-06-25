package weaver

sealed abstract class Platform(val name: String)

object Platform {
  def isJVM: Boolean = PlatformCompat.platform == JVM
  def isJS: Boolean  = PlatformCompat.platform == JS

  case object JS  extends Platform("js")
  case object JVM extends Platform("jvm")
}
