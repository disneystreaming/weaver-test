package weaver

import cats.effect.IO

trait BaseIOSuite extends RunnableSuite[IO] {
  implicit protected def effectCompat: UnsafeRun[IO] = CatsUnsafeRun
  final implicit protected def contextShift          = effectCompat.contextShift
  final implicit protected def timer                 = effectCompat.timer
}
