package weaver

import java.util.concurrent.TimeUnit

import cats.data.Chain

import zio._
import zio.clock.Clock
import zio.interop.catz._

package object ziocompat {

  object LogModule {
    abstract class Service(clock: Clock.Service)
        extends Log[UIO](clock.currentTime(TimeUnit.MILLISECONDS)) {
      def logs: UIO[Chain[Log.Entry]]
    }
    def logs: URIO[LogModule, Chain[Log.Entry]] = ZIO.accessM(_.get.logs)
  }
  type LogModule = Has[LogModule.Service]
  type Live      = Has[Live.Service]

  type T[A]             = RIO[ZEnv, A]
  type Env[R <: Has[_]] = ZEnv with Live with R with LogModule

  val unitTag = implicitly[Tag[Unit]]
  type ZIOSuite[R <: Has[_]] = MutableZIOSuite[R]
  type SimpleZIOSuite        = SimpleMutableZIOSuite
  type GlobalResource        = ZIOGlobalResource
  type GlobalRead            = GlobalResourceF.Read[T]
  type GlobalWrite           = ZIOGlobalResource.Write
  type FunSuite              = FunZIOSuite

  implicit class GlobalReadExt(private val read: GlobalRead) extends AnyVal {
    def getManaged[A](label: Option[String] = None)(
        implicit rt: ResourceTag[A]): RManaged[ZEnv, A] =
      ZManaged.fromEffect(read.getOrFail[A](label))
    def getLayer[A](label: Option[String] = None)(
        implicit rt: Tag[A]): RLayer[ZEnv, Has[A]] =
      ZLayer.fromEffect(read.getOrFail[A](label))
  }

  implicit def resourceTagFromTag[A](implicit A: Tag[A]): ResourceTag[A] =
    LTTResourceTag(A.tag)

}
