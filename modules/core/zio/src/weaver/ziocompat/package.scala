package weaver

import zio._

package object ziocompat {

  type T[A]                    = RIO[ZEnv, A]
  type Env[R <: Has[_]]        = ZEnv with R
  type LogModule               = Has[Log[UIO]]
  type PerTestEnv[R <: Has[_]] = Env[R] with LogModule

  val unitTag = implicitly[Tag[Unit]]
  type ZIOSuite[R <: Has[_]] = MutableZIOSuite[R]
  type SimpleZIOSuite        = SimpleMutableZIOSuite
  type GlobalResource        = ZIOGlobalResource
  type GlobalRead            = GlobalResourceF.Read[T]
  type GlobalWrite           = ZIOGlobalResource.Write

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
