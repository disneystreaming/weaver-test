package weaver
package ziocompat

import zio.LightTypeTag

case class LTTResourceTag[A](tag: LightTypeTag) extends ResourceTag[A] {
  def description: String = tag.repr

  def cast(obj: Any): Option[A] = {
    try { Some(obj.asInstanceOf[A]) }
    catch {
      case _: Throwable => None
    }
  }
}
