package weaver

import cats.effect.implicits._
import cats.effect.{
  ConcurrentEffect,
  ContextShift,
  Effect,
  IO,
  Resource,
  Timer
}
import cats.syntax.applicative._
import cats.syntax.applicativeError._

import fs2.Stream
import org.portablescala.reflect.annotation.EnableReflectiveInstantiation

// Just a non-parameterized marker trait to help SBT's test detection logic.
@EnableReflectiveInstantiation
trait BaseSuiteClass {}

trait Suite[F[_]] extends BaseSuiteClass {
  def name: String
  def spec(args: List[String]): Stream[F, TestOutcome]
}

// format: off
trait EffectSuite[F[_]] extends Suite[F] with Expectations.Helpers { self =>

  implicit def effect : Effect[F]

  /**
   * Raise an error that leads to the running test being tagged as "cancelled".
   */
  def cancel(reason: String)(implicit pos: SourceLocation): F[Nothing] =
    effect.raiseError(new CanceledException(Some(reason), pos))

  /**
   * Raises an error that leads to the running test being tagged as "ignored"
   */
  def ignore(reason: String)(implicit pos: SourceLocation): F[Nothing] =
    effect.raiseError(new IgnoredException(Some(reason), pos))

  /**
   * Expect macro
   */
  def expect = new Expect
  def assert = new Expect

  override def name : String = self.getClass.getName.replace("$", "")

  protected def adaptRunError: PartialFunction[Throwable, Throwable] = PartialFunction.empty

  def run(args : List[String])(report : TestOutcome => IO[Unit]) : IO[Unit] =
    spec(args).evalMap(testOutcome => effect.liftIO(report(testOutcome))).compile.drain.toIO.adaptErr(adaptRunError)

  implicit def singleExpectationConversion(e: SingleExpectation)(implicit loc: SourceLocation): F[Expectations] =
    Expectations.fromSingle(e).pure[F]

  implicit def expectationsConversion(e: Expectations): F[Expectations] =
    e.pure[F]
}

trait ConcurrentEffectSuite[F[_]] extends EffectSuite[F] {
  implicit def effect : ConcurrentEffect[F]
}

trait BaseIOSuite { self : ConcurrentEffectSuite[IO] =>
  val ec = scala.concurrent.ExecutionContext.global
  implicit def timer : Timer[IO] = IO.timer(ec)
  implicit def cs : ContextShift[IO] = IO.contextShift(ec)
  implicit def effect : ConcurrentEffect[IO] = IO.ioConcurrentEffect
}

trait PureIOSuite extends ConcurrentEffectSuite[IO] with BaseIOSuite {

  def pureTest(name: String)(run : => Expectations) : IO[TestOutcome] = Test[IO](name, IO(run))
  def simpleTest(name:  String)(run : IO[Expectations]) : IO[TestOutcome] = Test[IO](name, run)
  def loggedTest(name: String)(run : Log[IO] => IO[Expectations]) : IO[TestOutcome] = Test[IO](name, run)

}

trait MutableFSuite[F[_]] extends ConcurrentEffectSuite[F]  {

  type Res
  def sharedResource : Resource[F, Res]

  def maxParallelism : Int = 10000
  implicit def timer: Timer[F]

  protected def registerTest(id: TestId)(f: Res => F[TestOutcome]): Unit =
    synchronized {
      if (isInitialized) throw initError()
      testSeq = testSeq :+ ((id, f))
    }

  def pureTest(id: TestId)(run : => Expectations) :  Unit = registerTest(id)(_ => Test(id.name, effect.delay(run)))
  def simpleTest(id:  TestId)(run: => F[Expectations]) : Unit = registerTest(id)(_ => Test(id.name, effect.suspend(run)))
  def loggedTest(id: TestId)(run: Log[F] => F[Expectations]) : Unit = registerTest(id)(_ => Test(id.name, log => run(log)))
  def test(id: TestId) : PartiallyAppliedTest = new PartiallyAppliedTest(id)

  class PartiallyAppliedTest(id : TestId) {
    def apply(run: => F[Expectations]) : Unit = registerTest(id)(_ => Test(name, run))
    def apply(run : Res => F[Expectations]) : Unit = registerTest(id)(res => Test(name, run(res)))
    def apply(run : (Res, Log[F]) => F[Expectations]) : Unit = registerTest(id)(res => Test(name, log => run(res, log)))
  }

  override def spec(args: List[String]) : Stream[F, TestOutcome] =
    synchronized {
      if (!isInitialized) isInitialized = true
      val argsFilter = filterTests(this.name)(args)
      val filteredTests = testSeq.collect { case (id, test) if argsFilter(id) => test }
      val parallism = math.max(1, maxParallelism)
      if (filteredTests.isEmpty) Stream.empty // no need to allocate resources
      else for {
        resource <- Stream.resource(sharedResource)
        tests = filteredTests.map(_.apply(resource))
        testStream = Stream.emits(tests).lift[F]
        result <- if (parallism > 1 ) testStream.parEvalMap(parallism)(identity)
                  else testStream.evalMap(identity)
      } yield result
    }

  private[this] var testSeq = Seq.empty[(TestId, Res => F[TestOutcome])]
  private[this] var isInitialized = false

  private[this] def initError() =
    new AssertionError(
      "Cannot define new tests after TestSuite was initialized"
    )

}

trait MutableIOSuite extends MutableFSuite[IO] with BaseIOSuite

trait SimpleMutableIOSuite extends MutableIOSuite{
  type Res = Unit
  def sharedResource: Resource[IO, Unit] = Resource.pure(())
}
