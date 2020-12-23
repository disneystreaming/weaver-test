package weaver.ziocompat

import java.util.concurrent.TimeUnit

import scala.concurrent.duration._
import scala.util.control.NonFatal

import weaver.{ Expectations, Result, TestOutcome }

import zio._

object Test {

  def apply[R <: Has[_]](
      name: String,
      f: ZIO[Env[R], Throwable, Expectations]
  ): ZIO[Env[R], Nothing, TestOutcome] =
    for {
      start <- zio.clock.currentTime(TimeUnit.MILLISECONDS)
      res <- f
        .unrefine { case NonFatal(e) => e }
        .fold(Result.from, Result.fromAssertion)
      end  <- zio.clock.currentTime(TimeUnit.MILLISECONDS)
      logs <- LogModule.logs
    } yield TestOutcome(name, (end - start).millis, res, logs)

}
