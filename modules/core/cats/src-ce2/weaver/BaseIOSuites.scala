package weaver

import cats.effect.{ ContextShift, IO, Timer }

trait BaseIOSuite extends BaseCatsSuite { self: RunnableSuite[IO] =>
  implicit protected def effectCompat: UnsafeRun[EffectType] = CatsUnsafeRun
  def unsafeRun: UnsafeRun[EffectType]                       = CatsUnsafeRun
  final implicit protected def contextShift: ContextShift[IO] =
    effectCompat.contextShift
  final implicit protected def timer: Timer[IO] = effectCompat.timer
}

trait BaseFunIOSuite extends FunSuiteF[IO] with BaseCatsSuite {
  override implicit protected def effectCompat: UnsafeRun[EffectType] =
    CatsUnsafeRun
}
