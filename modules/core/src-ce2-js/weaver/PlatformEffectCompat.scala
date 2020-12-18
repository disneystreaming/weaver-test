package weaver

trait PlatformEffectCompat[F[_]] { self: EffectCompat[F] => }
