package weaver

import cats.effect.{ ContextShift, IO, Timer }

trait BaseIOSuite extends RunnableSuite[IO] {
  implicit protected def effectCompat: UnsafeRun[IO] = CatsUnsafeRun
  final implicit protected def contextShift: ContextShift[IO] =
    effectCompat.contextShift
  final implicit protected def timer: Timer[IO] = effectCompat.timer
}
