package weaver.ziocompat

import weaver.Expectations
import weaver.Log
import weaver.Result
import weaver.TestOutcome

import cats.data.Chain
import zio._
import zio.clock._
import java.util.concurrent.TimeUnit

import zio.clock.Clock
import zio.console.Console
import zio.system.System
import zio.random.Random

import scala.concurrent.duration._

class Test[R](
    val name: String,
    val f: ZIO[LogModule with Env[R], Throwable, Expectations]) {

  def compile: ZIO[Env[R], Nothing, TestOutcome] =
    for {
      ref   <- Ref.make(Chain.empty[Log.Entry])
      start <- zio.clock.currentTime(TimeUnit.MILLISECONDS)
      res <- f
        .provideSome[Env[R]] { env =>
          new LogModule with SharedResourceModule[R] with Clock with Console
          with System with Random {
            val log            = new RefLog(ref)
            val console        = env.console
            val random         = env.random
            val system         = env.system
            val clock          = env.clock
            val sharedResource = env.sharedResource,
          }
        }
        .fold(Result.from, Result.fromAssertion)
      end  <- zio.clock.currentTime(TimeUnit.MILLISECONDS)
      logs <- ref.get
    } yield TestOutcome(name, (end - start).millis, res, logs)
}

object Test {

  def apply[R](name: String)(
      f: ZIO[LogModule with Env[R], Throwable, Expectations]): Test[R] =
    new Test(name, f)

}
