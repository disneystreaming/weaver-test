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
trait EffectSuite[F[_]] extends Suite[F]{ self =>

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

trait PureIOSuite extends ConcurrentEffectSuite[IO] with BaseIOSuite with Expectations.Helpers {

  def pureTest(name: String)(run : => Expectations) : IO[TestOutcome] = Test[IO](name, IO(run))
  def simpleTest(name:  String)(run : IO[Expectations]) : IO[TestOutcome] = Test[IO](name, run)
  def loggedTest(name: String)(run : Log[IO] => IO[Expectations]) : IO[TestOutcome] = Test[IO](name, run)

}

trait MutableFSuite[F[_]] extends ConcurrentEffectSuite[F]  {

  type Res
  def sharedResource : Resource[F, Res]

  def maxParallelism : Int = 10000
  implicit def timer: Timer[F]

  protected def registerTest(name: String)(f: Res => F[TestOutcome]): Unit =
    synchronized {
      if (isInitialized) throw initError()
      testSeq = testSeq :+ (name -> f)
    }

  def pureTest(name: String)(run : => Expectations) :  Unit = registerTest(name)(_ => Test(name, effect.delay(run)))
  def simpleTest(name:  String)(run: => F[Expectations]) : Unit = registerTest(name)(_ => Test(name, effect.suspend(run)))
  def loggedTest(name: String)(run: Log[F] => F[Expectations]) : Unit = registerTest(name)(_ => Test(name, log => run(log)))
  def test(name: String) : PartiallyAppliedTest = new PartiallyAppliedTest { val testName: String = name }

  trait PartiallyAppliedTest {
    def testName: String
    def apply(run: => F[Expectations]) : Unit = registerTest(testName)(_ => Test(testName, run))
    def apply(run : Res => F[Expectations]) : Unit = registerTest(testName)(res => Test(testName, run(res)))
    def apply(run : (Res, Log[F]) => F[Expectations]) : Unit = registerTest(testName)(res => Test(testName, log => run(res, log)))
  }

  override def spec(args: List[String]) : Stream[F, TestOutcome] =
    synchronized {
      if (!isInitialized) isInitialized = true
      val argsFilter = filterTests(this.name)(args)
      val filteredTests = testSeq.collect { case (name, test) if argsFilter(name) => test }
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

  private[this] var testSeq = Seq.empty[(String, Res => F[TestOutcome])]
  private[this] var isInitialized = false

  private[this] def initError() =
    new AssertionError(
      "Cannot define new tests after TestSuite was initialized"
    )

}

trait MutableIOSuite extends MutableFSuite[IO] with BaseIOSuite with Expectations.Helpers

trait SimpleMutableIOSuite extends MutableIOSuite {
  type Res = Unit
  def sharedResource: Resource[IO, Unit] = Resource.pure(())
}
