package weaver
package ziocompat

import zio.{ IO, URLayer, ZEnv, ZIO }

/**
 * Service used for getting the real environment during tests. This is useful
 * for timing or getting random values during tests. For example, getting a
 * random port for starting servers or timing an action with the real clock.
 * This pattern is inspired by ZIO-test
 */
object Live {
  trait Service {
    def live[E, A](zio: ZIO[ZEnv, E, A]): IO[E, A]
  }

  def live[E, A](zio: ZIO[ZEnv, E, A]): ZIO[Live, E, A] =
    ZIO.serviceWith[Service](_.live(zio))

  def apply(): URLayer[ZEnv, Live] = ZIO.environment[ZEnv].map(env =>
    new Service {
      override def live[E, A](zio: ZIO[ZEnv, E, A]): IO[E, A] = zio.provide(env)
    }).toLayer
}
