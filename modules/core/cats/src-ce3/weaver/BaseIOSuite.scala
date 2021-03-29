package weaver

import cats.effect.IO

trait BaseIOSuite extends RunnableSuite[IO] {
  implicit protected def effectCompat: UnsafeRun[IO] = CatsUnsafeRun
}

trait BaseFunIOSuite extends RunnableSuite[IO] with FunSuiteAux[IO]
