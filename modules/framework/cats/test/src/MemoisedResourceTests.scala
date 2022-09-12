package weaver
package framework
package test

import scala.concurrent.duration._

import cats.effect._
import cats.syntax.all._

object MemoisedResourceTests extends SimpleIOSuite {

  test("""|Memoised resources should be:
          | * lazily allocated,
          | * shared when accessed concurrently
          | * not finalised until all uses are finished
          | * re-allocated on demand after being finalised""".stripMargin) {
    for {
      initialised <- Ref[IO].of(0)
      finalised   <- Ref[IO].of(0)
      used        <- Ref[IO].of(0)
      resource =
        Resource.make(initialised.update(_ + 1))(_ => finalised.update(_ + 1))
      use <- MemoisedResource(resource).map(r =>
        r.use(_ => IO.sleep(250.millis) *> used.update(_ + 1)))
      _         <- use
      _         <- List.fill(10)(use).parSequence
      _         <- use
      initCount <- initialised.get
      finCount  <- finalised.get
      useCount  <- used.get
    } yield expect.all(initCount == 3, finCount == 3, useCount == 12)
  }

  object Boom extends Exception("Boom")

  test("""|Memoised resources reset when allocation fails""".stripMargin) {
    for {
      fail <- Ref[IO].of(true)
      allocate = fail.modify[IO[String]] {
        case true  => (false, IO.raiseError(Boom))
        case false => (false, IO.pure("hello"))
      }.flatten
      resource = Resource.eval(allocate)
      res <- MemoisedResource(resource)
      use = res.use(r => IO.pure(r))
      firstResult  <- use.attempt
      secondResult <- use
    } yield expect.all(firstResult == Left(Boom), secondResult == "hello")
  }

}
