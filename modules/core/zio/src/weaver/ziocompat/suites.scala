package weaver
package ziocompat

import scala.util.Try

import fs2._
import zio._
import zio.interop.catz._

abstract class BaseZIOSuite extends EffectSuite[T] with RunnableSuite[T] {
  val effectCompat: UnsafeRun[T] = ZIOUnsafeRun
}

abstract class BaseMutableZIOSuite[Res <: Has[_]](implicit tag: Tag[Res])
    extends BaseZIOSuite {

  val sharedLayer: ZLayer[ZEnv, Throwable, Res]

  def maxParallelism: Int = 10000

  private[this] type Test = ZIO[Env[Res], Nothing, TestOutcome]

  protected def registerTest(name: TestName)(test: Test): Unit =
    synchronized {
      if (isInitialized) throw initError()
      testSeq = testSeq :+ ((name, test))
    }

  def pureTest(name: TestName)(run: => Expectations): Unit =
    registerTest(name)(Test(name.name, ZIO(run)))

  def test(name: TestName)(
      run: => ZIO[PerTestEnv[Res], Throwable, Expectations]): Unit =
    registerTest(name)(Test(name.name, ZIO.fromTry(Try { run }).flatten))

  override def spec(args: List[String]): Stream[T, TestOutcome] =
    synchronized {
      if (!isInitialized) isInitialized = true
      val argsFilter = Filters.filterTests(this.name)(args)
      val filteredTests = testSeq.collect {
        case (name, test) if argsFilter(name) => test
      }
      if (filteredTests.isEmpty) Stream.empty // no need to allocate resources
      else {
        val suiteLayer = sharedLayer.passthrough
        for {
          resource <- Stream.resource(suiteLayer.build.toResourceZIO)
          result <- Stream
            .emits(filteredTests)
            .lift[Task]
            .parEvalMap(math.max(1, maxParallelism))(_.provide(resource))
        } yield result
      }
    }

  private[this] var testSeq       = Seq.empty[(TestName, Test)]
  private[this] var isInitialized = false

  private[this] def initError() =
    new AssertionError(
      "Cannot define new tests after TestSuite was initialized"
    )

  override protected def adaptRunError: PartialFunction[Throwable, Throwable] = {
    case FiberFailure(cause) => cause.asInstanceOf[Cause[Throwable]].squash
  }
}

abstract class MutableZIOSuite[Res <: Has[_]](implicit tag: Tag[Res])
    extends BaseMutableZIOSuite()(tag)
    with Expectations.Helpers

abstract class SimpleMutableZIOSuite extends MutableZIOSuite[Has[Unit]] {
  override val sharedLayer: zio.ZLayer[ZEnv, Throwable, Has[Unit]] =
    ZLayer.fromEffect(UIO.unit)
}
