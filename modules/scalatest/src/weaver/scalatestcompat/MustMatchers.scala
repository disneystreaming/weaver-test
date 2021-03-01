package weaver.scalatestcompat

import cats.effect.IO
import org.scalatest.matchers.must
import weaver.EffectSuite

trait MustMatchers[F[_]] extends Matchers[F] with must.Matchers {
  self: EffectSuite[F] =>
}

trait IOMustMatchers extends MustMatchers[IO] {
  self: EffectSuite[IO] =>
}
