package weaver

import cats.effect.Resource
import cats.effect.Sync

trait PlatformEffectCompat[F[_]] { self: EffectCompat[F] =>

  private[weaver] def blocker[T](
      f: BlockerCompat[F] => T): Resource[F, T] =
    Resource.unit[F].map(blocker =>
      new BlockerCompat[F] {
        def block[A](thunk: => A): F[A] = Sync[F].blocking(thunk)
      }).map(f)

}
