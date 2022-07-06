package weaver

import cats.effect.{ Blocker, Resource }

trait PlatformEffectCompat[F[_]] { self: EffectCompat[F] =>

  private[weaver] def blocker[T](
      f: BlockerCompat[F] => T): Resource[F, T] =
    Blocker[F].map(blocker =>
      new BlockerCompat[F] {
        def block[A](thunk: => A): F[A] = blocker.delay(thunk)
      }).map(f)

}
