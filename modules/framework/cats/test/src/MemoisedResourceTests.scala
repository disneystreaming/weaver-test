package weaver
package framework
package test

import cats.effect.concurrent.Ref
import cats.effect._
import cats.syntax.all._

import scala.concurrent.duration._

object MemoisedResourceTests extends SimpleIOSuite {

  test("""|Memoised resources should be:
          | * lazily allocated,
          | * shared when accessed concurrently
          | * not finalised until all uses are finished
          | * re-allocated on demand after being finalised""".stripMargin) {
    for {
      initalised <- Ref[IO].of(0)
      finalised  <- Ref[IO].of(0)
      used       <- Ref[IO].of(0)
      resource =
        Resource.make(initalised.update(_ + 1))(_ => finalised.update(_ + 1))
      use <- MemoisedResource(resource).map(r =>
        r.use(_ => IO.sleep(100.millis) *> used.update(_ + 1)))
      _         <- use
      _         <- List.fill(10)(use).parSequence
      _         <- use
      initCount <- initalised.get
      finCount  <- finalised.get
      useCount  <- used.get
    } yield expect.all(initCount == 3, finCount == 3, useCount == 12)
  }

}
