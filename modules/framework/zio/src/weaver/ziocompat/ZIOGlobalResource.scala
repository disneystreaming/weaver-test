package weaver
package ziocompat

import cats.effect.Resource

import weaver.GlobalResource

import zio._
import zio.interop.catz._

trait ZIOGlobalResource extends GlobalResource[T] {

  def share(store: ZIOGlobalResource.Write): RManaged[ZEnv, Unit]

  final def sharedResources(store: GlobalResource.Write[T]): Resource[T, Unit] =
    share(ZIOGlobalResource.toZIO(store)).toResourceZIO

}

object ZIOGlobalResource {

  trait Write {
    def put[A](value: A, label: Option[String] = None)(implicit
    rt: ResourceTag[A]): RIO[ZEnv, Unit]

    def putM[A](value: A, label: Option[String] = None)(
        implicit rt: ResourceTag[A]): RManaged[ZEnv, Unit] =
      ZManaged.fromEffect(put(value, label))
  }

  private def toZIO(store: GlobalResource.Write[T]): Write = new Write {
    def put[A](value: A, label: Option[String])(implicit
    rt: ResourceTag[A]): RIO[ZEnv, Unit] = store.put(value, label)
  }

}
