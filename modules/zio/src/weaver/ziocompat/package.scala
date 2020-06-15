package weaver

import zio._

package object ziocompat {

  type Env[R <: Has[_]]        = ZEnv with R
  type LogModule               = Has[Log[UIO]]
  type PerTestEnv[R <: Has[_]] = Env[R] with LogModule

  val unitTag = implicitly[Tag[Unit]]
  type ZIOSuite[R <: Has[_]] = MutableZIOSuite[R]
  type SimpleZIOSuite        = SimpleMutableZIOSuite

}
