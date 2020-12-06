package weaver
package ziocompat

import cats.effect.Resource

import zio._
import zio.interop.catz._

trait ZIOGlobalResource extends weaver.GlobalResourceF[T] {

  def share(global: GlobalWrite): RManaged[ZEnv, Unit]

  final def sharedResources(
      global: weaver.GlobalResourceF.Write[T]): Resource[T, Unit] =
    share(ZIOGlobalResource.toZIO(global)).toResourceZIO

}

object ZIOGlobalResource {

  trait Write {
    def put[A: Tag](value: A, label: Option[String] = None): RIO[ZEnv, Unit]

    def putM[A: Tag](
        value: A,
        label: Option[String] = None): RManaged[ZEnv, Unit] =
      ZManaged.fromEffect(put(value, label))
  }

  private def toZIO(global: weaver.GlobalResourceF.Write[T]): Write =
    new Write {
      def put[A: Tag](value: A, label: Option[String]): RIO[ZEnv, Unit] =
        global.put(value, label)
    }

}
