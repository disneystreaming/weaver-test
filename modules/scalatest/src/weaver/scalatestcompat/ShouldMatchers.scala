package weaver.scalatestcompat

import cats.effect.IO
import org.scalatest.matchers.should
import weaver.EffectSuite

trait ShouldMatchers[F[_]] extends Matchers[F] with should.Matchers {
  self: EffectSuite[F] =>
}

trait IOShouldMatchers extends ShouldMatchers[IO] {
  self: EffectSuite[IO] =>
}
