package weaver
package ziocompat

import scala.util.Try

import cats.effect.ConcurrentEffect

import fs2._
import zio._
import zio.interop.catz._

abstract class MutableZIOSuite[Res <: Has[_]](implicit tag: Tag[Res])
    extends ConcurrentEffectSuite[Task] {

  val sharedLayer: ZLayer[ZEnv, Throwable, Res]

  def maxParallelism: Int = 10000

  implicit val runtime: Runtime[ZEnv] = zio.Runtime.default
  implicit def effect: ConcurrentEffect[Task] =
    zio.interop.catz.taskEffectInstance

  private[this] type Test = ZIO[Env[Res], Nothing, TestOutcome]

  protected def registerTest(
      name: String,
      testIndicator: TestIndicator)(test: Test): Unit =
    synchronized {
      if (isInitialized) throw initError()
      testSeq = testSeq :+ ((name, testIndicator, test))
    }

  def pureTest(name: String)(run: => Expectations)(implicit
  ti: TestIndicator): Unit =
    registerTest(name, ti)(Test(name, ZIO(run)))

  def test(name: String)(
      run: => ZIO[PerTestEnv[Res], Throwable, Expectations])(implicit
  ti: TestIndicator): Unit =
    registerTest(name, ti)(Test(name, ZIO.fromTry(Try { run }).flatten))

  override def spec(args: List[String]): Stream[Task, TestOutcome] =
    synchronized {
      if (!isInitialized) isInitialized = true
      val argsFilter = filterTests(this.name)(args)
      val filteredTests = testSeq.collect {
        case (name, testIndicator, test) if argsFilter((name, testIndicator)) =>
          test
      }
      if (filteredTests.isEmpty) Stream.empty // no need to allocate resources
      else {
        val baseEnv    = ZLayer.succeedMany(runtime.environment)
        val suiteLayer = baseEnv >>> sharedLayer.passthrough
        for {
          resource <- Stream.resource(suiteLayer.build.toResourceZIO)
          result <- Stream
            .emits(filteredTests)
            .lift[Task]
            .parEvalMap(math.max(1, maxParallelism))(_.provide(resource))
        } yield result
      }
    }

  private[this] var testSeq       = Seq.empty[(String, TestIndicator, Test)]
  private[this] var isInitialized = false

  private[this] def initError() =
    new AssertionError(
      "Cannot define new tests after TestSuite was initialized"
    )

  override protected def adaptRunError: PartialFunction[Throwable, Throwable] = {
    case FiberFailure(cause) => cause.asInstanceOf[Cause[Throwable]].squash
  }
}

trait SimpleMutableZIOSuite extends MutableZIOSuite[Has[Unit]] {
  override val sharedLayer: zio.ZLayer[ZEnv, Throwable, Has[Unit]] =
    ZLayer.fromEffect(UIO.unit)
}
