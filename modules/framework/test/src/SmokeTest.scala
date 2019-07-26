package weaver.test

import weaver.testkit._
import cats.implicits._
import cats.effect.IO
import fs2.Stream

object SmokeTest extends PureIOSuite {

  def spec: Stream[IO, TestOutcome] =
    Stream {
      simpleTest("hello") {
        not(1 must_> 1)
      }
    }.lift[IO].parEvalMap(5)(identity)

}

object SmokeTest2 extends MutableIOSuite {
  simpleTest("hello") {
    throw (new Exception("Boom"))
  }
  test("hello 2") {
    IO(not(verify(false) and verify(true)))
  }
}
