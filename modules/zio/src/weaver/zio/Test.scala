package weaver.zio

import weaver.Expectations
import weaver.Log
import weaver.Result
import weaver.TestOutcome

import cats.data.Chain
import zio._
import zio.clock._
import java.util.concurrent.TimeUnit

import scala.concurrent.duration._

class Test[R](
    val name: String,
    val f: ZIO[LogModule with SharedResourceModule[R], Throwable, Expectations]) {

  def compile: ZIO[Clock with SharedResourceModule[R], Nothing, TestOutcome] =
    for {
      ref   <- Ref.make(Chain.empty[Log.Entry])
      start <- zio.clock.currentTime(TimeUnit.MILLISECONDS)
      res <- f
        .provideSome[SharedResourceModule[R]] { s =>
          val refLog = new RefLog(ref)
          new refLog.lift with s.lift
        }
        .fold(Result.from, Result.fromAssertion)
      end  <- zio.clock.currentTime(TimeUnit.MILLISECONDS)
      logs <- ref.get
    } yield TestOutcome(name, (end - start).millis, res, logs)
}

object Test {

  def apply[R](name: String)(
      f: ZIO[LogModule with SharedResourceModule[R], Throwable, Expectations]): Test[R] =
    new Test(name, f)

}
