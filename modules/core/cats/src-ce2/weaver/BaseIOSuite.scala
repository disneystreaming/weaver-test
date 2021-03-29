package weaver

import cats.effect.IO
import cats.effect.Temporal

trait BaseIOSuite extends RunnableSuite[IO] {
  implicit protected def effectCompat: UnsafeRun[IO] = CatsUnsafeRun
  final implicit protected def contextShift: ContextShift[IO] =
    effectCompat.contextShift
  final implicit protected def timer: Temporal[IO] = effectCompat.timer
}
