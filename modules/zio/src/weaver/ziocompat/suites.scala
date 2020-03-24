package weaver
package ziocompat

import weaver.EffectSuite
import weaver.Expectations
import weaver.TestOutcome

import zio._

import fs2._
import cats.effect.ExitCase

abstract class MutableZIOSuite[Res <: Has[_]](implicit tag: Tagged[Res])
    extends EffectSuite[Task] {

  val sharedLayer: ZLayer[ZEnv, Throwable, Res]

  def maxParallelism: Int = 10000

  implicit val runtime: Runtime[ZEnv] = zio.Runtime.default
  implicit def effect                 = zio.interop.catz.taskEffectInstance

  private[this] type Test = ZIO[Env[Res], Nothing, TestOutcome]

  protected def registerTest(name: String)(test: Test): Unit =
    synchronized {
      if (isInitialized) throw initError()
      testSeq = testSeq :+ (name -> test)
    }

  def pureTest(name: String)(run: => Expectations): Unit =
    registerTest(name)(Test(name, ZIO(run)))

  def test(name: String)(
      run: ZIO[PerTestEnv[Res], Throwable, Expectations]): Unit =
    registerTest(name)(Test(name, run))

  override def spec(args: List[String]): Stream[Task, TestOutcome] =
    synchronized {
      if (!isInitialized) isInitialized = true
      val argsFilter = filterTests(this.name)(args)
      val filteredTests = testSeq.collect {
        case (name, test) if argsFilter(name) => test
      }
      if (filteredTests.isEmpty) Stream.empty // no need to allocate resources
      else {
        val baseEnv    = ZLayer.succeedMany(runtime.environment)
        val suiteLayer = baseEnv >>> sharedLayer.passthrough
        for {
          reservation <- Stream.eval(suiteLayer.build.reserve)
          resource <- Stream.bracketCase(reservation.acquire)((_, exitCase) =>
            reservation.release(fromCats(exitCase)).unit)
          result <- Stream
            .emits(filteredTests)
            .lift[Task]
            .parEvalMap(math.max(1, maxParallelism))(_.provide(resource))
        } yield result
      }
    }

  private[this] var testSeq       = Seq.empty[(String, Test)]
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

trait SimpleMutableZIOSuite extends MutableZIOSuite[Has[Unit]] {
  override val sharedLayer: zio.ZLayer[ZEnv, Throwable, Has[Unit]] =
    ZLayer.fromEffect(UIO.unit)
}
