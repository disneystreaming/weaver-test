package weaver
package ziocompat

import weaver.EffectSuite
import weaver.Expectations
import weaver.TestOutcome

import zio._
import zio.clock.Clock
import zio.console.Console
import zio.system.System
import zio.random.Random

import fs2._
import cats.effect.ExitCase

trait MutableZIOSuite extends EffectSuite[Task] {

  type Res
  def sharedResource: Managed[Throwable, Res]
  def maxParallelism: Int = 10000

  val ec = scala.concurrent.ExecutionContext.global
  implicit val runtime: Runtime[BaseEnv] =
    new DefaultRuntime {}
  implicit def effect = zio.interop.catz.taskEffectInstance

  def registerTest[D >: LogModule with Env[Res]](name: String)(
      run: ZIO[D, Throwable, Expectations]): Unit =
    synchronized {
      if (isInitialized) throw initError()
      testSeq = testSeq :+ name -> Test[Res](name)(run)
    }

  def pureTest(name: String)(run: => Expectations): Unit =
    registerTest(name)(ZIO(run))

  def test[D >: LogModule with Env[Res]](name: String)(
      run: ZIO[D, Throwable, Expectations]): Unit =
    registerTest(name)(run)

  override def spec(args: List[String]): Stream[Task, TestOutcome] =
    synchronized {
      if (!isInitialized) isInitialized = true
      val argsFilter = filterTests(this.name)(args)
      val filteredTests = testSeq.collect {
        case (name, test) if argsFilter(name) => test
      }
      if (filteredTests.isEmpty) Stream.empty // no need to allocate resources
      else
        for {
          reservation <- Stream.eval(sharedResource.reserve)
          resource <- Stream.bracketCase(reservation.acquire)((_, exitCase) =>
            reservation.release(fromCats(exitCase)).unit)
          result <- Stream
            .emits(filteredTests)
            .lift[Task]
            .parEvalMap(math.max(1, maxParallelism))(
              _.compile.provide(new Clock.Live with Console.Live
              with System.Live with Random.Live with SharedResourceModule[Res] {
                val sharedResource = resource
              }))
        } yield result
    }

  private[this] var testSeq       = Seq.empty[(String, Test[Res])]
  private[this] var isInitialized = false

  private[this] def initError() =
    new AssertionError(
      "Cannot define new tests after TestSuite was initialized"
    )

  private def fromCats[A](exitCase: ExitCase[Throwable]): Exit[Throwable, _] =
    exitCase match {
      case ExitCase.Canceled  => Exit.interrupt(Fiber.Id.None)
      case ExitCase.Completed => Exit.succeed(())
      case ExitCase.Error(e)  => Exit.fail(e)
    }

}

trait SimpleMutableZIOSuite extends MutableZIOSuite {
  type Res = Unit
  def sharedResource: zio.Managed[Throwable, Unit] = zio.Managed.unit
}
