package weaver

import cats.effect.IO

trait BaseIOSuite extends RunnableSuite[IO] with BaseCatsSuite {
  implicit protected def effectCompat: UnsafeRun[IO] = CatsUnsafeRun
  def getSuite: EffectSuite[IO]                      = this
}

trait BaseFunIOSuite extends FunSuiteF[IO] with BaseCatsSuite {
  implicit protected def effectCompat: UnsafeRun[EffectType] = CatsUnsafeRun
  def getSuite: EffectSuite[IO]                              = this
}
