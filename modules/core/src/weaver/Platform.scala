package weaver

sealed abstract class Platform(val name: String)

object Platform {
  case object JS  extends Platform("js")
  case object JVM extends Platform("jvm")
}
