package weaver

import cats.effect.Resource
import cats.effect.implicits._
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
trait EffectSuite[F[_]] extends Suite[F] with SourceLocation.Here { self =>

  implicit protected def effectCompat: EffectCompat[F]

  /**
   * Raise an error that leads to the running test being tagged as "cancelled".
   */
  def cancel(reason: String)(implicit pos: SourceLocation): F[Nothing] =
    effectCompat.effect.raiseError(new CanceledException(Some(reason), pos))

  /**
   * Raises an error that leads to the running test being tagged as "ignored"
   */
  def ignore(reason: String)(implicit pos: SourceLocation): F[Nothing] =
    effectCompat.effect.raiseError(new IgnoredException(Some(reason), pos))

  override def name : String = self.getClass.getName.replace("$", "")

  protected def adaptRunError: PartialFunction[Throwable, Throwable] = PartialFunction.empty

  final def run(args : List[String])(report : TestOutcome => F[Unit]) : F[Unit] = {
    val compat = effectCompat
    import compat._
    spec(args).evalMap(report).compile.drain.adaptErr(adaptRunError)
  }

  implicit def expectationsConversion(e: Expectations): F[Expectations] =
    effectCompat.effect.pure(e)
}

trait RunnableSuite[F[_]] extends EffectSuite[F] {
  implicit protected def effectCompat: UnsafeRun[F]

  private[weaver] def runUnsafe(args: List[String])(report: TestOutcome => Unit) : Unit =
    effectCompat.sync(run(args)(outcome => effectCompat.effect.delay(report(outcome))))
}

trait MutableFSuite[F[_]] extends EffectSuite[F]  {

  type Res
  def sharedResource : Resource[F, Res]

  def maxParallelism : Int = 10000

  protected def registerTest(name: TestName)(f: Res => F[TestOutcome]): Unit =
    synchronized {
      if (isInitialized) throw initError()
      testSeq = testSeq :+ (name -> f)
    }

  def pureTest(name: TestName)(run : => Expectations) :  Unit = registerTest(name)(_ => Test(name.name, effectCompat.effect.delay(run)))
  def simpleTest(name:  TestName)(run: => F[Expectations]) : Unit = registerTest(name)(_ => Test(name.name, effectCompat.effect.suspend(run)))
  def loggedTest(name: TestName)(run: Log[F] => F[Expectations]) : Unit = registerTest(name)(_ => Test(name.name, log => run(log)))
  def test(name: TestName) : PartiallyAppliedTest = new PartiallyAppliedTest(name)

  class PartiallyAppliedTest(name : TestName) {
    def apply(run: => F[Expectations]) : Unit = registerTest(name)(_ => Test(name.name, run))
    def apply(run : Res => F[Expectations]) : Unit = registerTest(name)(res => Test(name.name, run(res)))
    def apply(run : (Res, Log[F]) => F[Expectations]) : Unit = registerTest(name)(res => Test(name.name, log => run(res, log)))
  }

  override def spec(args: List[String]) : Stream[F, TestOutcome] =
    synchronized {
      if (!isInitialized) isInitialized = true
      val argsFilter = Filters.filterTests(this.name)(args)
      val filteredTests = testSeq.collect { case (name, test) if argsFilter(name) => test }
      val parallism = math.max(1, maxParallelism)
      if (filteredTests.isEmpty) Stream.empty // no need to allocate resources
      else for {
        resource <- Stream.resource(sharedResource)
        tests = filteredTests.map(_.apply(resource))
        testStream = Stream.emits(tests).lift[F](effectCompat.effect)
        result <- if (parallism > 1 ) testStream.parEvalMap(parallism)(identity)(effectCompat.effect)
                  else testStream.evalMap(identity)
      } yield result
    }

  private[this] var testSeq = Seq.empty[(TestName, Res => F[TestOutcome])]
  private[this] var isInitialized = false

  private[this] def initError() =
    new AssertionError(
      "Cannot define new tests after TestSuite was initialized"
    )

}

