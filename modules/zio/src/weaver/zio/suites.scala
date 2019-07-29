package weaver.zio

import weaver.EffectSuite
import weaver.Expectations
import weaver.TestOutcome

import zio._
import zio.clock.Clock
import zio.console.Console
import zio.system.System
import zio.random.Random

import fs2._

trait MutableZIOSuite[R] extends EffectSuite[Task] {

  def sharedResource: Managed[Throwable, R]
  def maxParallelism: Int = 10000

  val ec = scala.concurrent.ExecutionContext.global
  implicit val runtime: Runtime[Clock with Console with System with Random] =
    new DefaultRuntime {}
  implicit def effect = zio.interop.catz.taskEffectInstances

  type Eff[D >: LogModule with SharedResourceModule[R], A] =
    ZIO[D, Exception, A]

  def registerTest[D >: LogModule with SharedResourceModule[R]](name: String)(
      run: ZIO[D, Throwable, Expectations]): Unit =
    synchronized {
      if (isInitialized) throw initError()
      testSeq = testSeq :+ Test[R](name)(run)
    }

  def pureTest(name: String)(run: => Expectations): Unit =
    registerTest(name)(ZIO(run))

  def test[D >: LogModule with SharedResourceModule[R]](name: String)(
      run: ZIO[D, Throwable, Expectations]): Unit =
    registerTest(name)(run)

  lazy val spec: Stream[Task, TestOutcome] =
    synchronized {
      if (!isInitialized) isInitialized = true
      for {
        reservation <- Stream.eval(sharedResource.reserve)
        resource <- Stream.bracket(reservation.acquire)(_ =>
          reservation.release.unit)
        result <- Stream
          .emits(testSeq)
          .lift[Task]
          .parEvalMap(math.max(1, maxParallelism))(
            _.compile.provide(new Clock with SharedResourceModule[R] {
              val clock          = runtime.Environment.clock
              val sharedResource = resource
            }))
      } yield result
    }

  private[this] var testSeq       = Seq.empty[Test[R]]
  private[this] var isInitialized = false

  private[this] def initError() =
    new AssertionError(
      "Cannot define new tests after TestSuite was initialized"
    )

}

trait SimpleMutableZIOSuite extends MutableZIOSuite[Unit] {
  def sharedResource: zio.Managed[Throwable, Unit] = zio.Managed.unit
}
