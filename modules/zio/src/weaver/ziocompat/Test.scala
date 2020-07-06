package weaver.ziocompat

import java.util.concurrent.TimeUnit

import scala.concurrent.duration._
import scala.util.control.NonFatal

import cats.data.Chain

import weaver.{ Expectations, Log, Result, TestOutcome }

import zio._

object Test {

  def apply[R <: Has[_]](
      name: String,
      f: ZIO[PerTestEnv[R], Throwable, Expectations]
  ): ZIO[Env[R], Nothing, TestOutcome] =
    for {
      ref   <- Ref.make(Chain.empty[Log.Entry])
      start <- zio.clock.currentTime(TimeUnit.MILLISECONDS)
      res <- f
        .provideSomeLayer[Env[R]](ZLayer.succeed(RefLog(ref)))
        .unrefine { case NonFatal(e) => e }
        .fold(Result.from, Result.fromAssertion)
      end  <- zio.clock.currentTime(TimeUnit.MILLISECONDS)
      logs <- ref.get
    } yield TestOutcome(name, (end - start).millis, res, logs)

}
