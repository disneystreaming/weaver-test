package weaver

import cats.effect.Resource
import cats.syntax.all._

import fs2.Stream
import org.junit.runner.RunWith
import org.portablescala.reflect.annotation.EnableReflectiveInstantiation

// Just a non-parameterized marker trait to help SBT's test detection logic.
@EnableReflectiveInstantiation
trait BaseSuiteClass {}

trait Suite[F[_]] extends BaseSuiteClass {
  def name: String
  def spec(args: List[String]): Stream[F, TestOutcome]
}

// A version of EffectSuite that has a type member instead of a type parameter.
protected[weaver] trait EffectSuiteAux {
  type EffectType[A]
  implicit protected def effect: CECompat.Effect[EffectType]
}

// format: off
trait EffectSuite[F[_]] extends Suite[F] with EffectSuiteAux with SourceLocation.Here { self =>

  final type EffectType[A] = F[A]
  implicit protected def effectCompat: EffectCompat[F]
  implicit final protected def effect: CECompat.Effect[F] = effectCompat.effect

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

  final def run(args : List[String])(report : TestOutcome => F[Unit]) : F[Unit] =
    spec(args).evalMap(report).compile.drain.adaptErr(adaptRunError)
}

sealed trait TestEvent
case class TestStarted(testName: TestName) extends TestEvent
case class TestEnded(outcome: TestOutcome) extends TestEvent

@RunWith(classOf[weaver.junit.WeaverRunner])
abstract class RunnableSuite[F[_]] extends EffectSuite[F] {
  implicit protected def effectCompat: UnsafeRun[EffectType]
  def plan : List[TestName]
  private[weaver] def runUnsafe(args: List[String])(report: TestOutcome => Unit) : Unit =
    effectCompat.sync(run(args)(outcome => effectCompat.effect.delay(report(outcome))))
}

abstract class MutableFSuite[F[_]] extends RunnableSuite[F]  {

  type Res
  def sharedResource : Resource[F, Res]

  def maxParallelism : Int = 10000

  protected def registerTest(name: TestName)(f: Res => F[TestOutcome]): Unit =
    synchronized {
      if (isInitialized) throw initError()
      testSeq = testSeq :+ (name -> f)
    }

  def pureTest(name: TestName)(run : => Expectations) :  Unit = registerTest(name)(_ => Test(name.name, effectCompat.effect.delay(run)))
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
      val filteredTests = if (testSeq.exists(_._1.tags("only"))){
        testSeq.filter(_._1.tags("only")).map { case (_, test) => (res: Res) => test(res)}
      } else testSeq.collect {
        case (name, test) if argsFilter(name) => (res : Res) => test(res)
      }
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

  def plan: List[TestName] = testSeq.map(_._1).toList

  private[this] var isInitialized = false

  private[this] def initError() =
    new AssertionError(
      "Cannot define new tests after TestSuite was initialized"
    )

}

abstract class FunSuiteAux[F[_]] extends RunnableSuite[F] { self =>
  def test(name: TestName)(run: => Expectations): Unit = synchronized {
    if(isInitialized) throw initError
    testSeq = testSeq :+ (name -> (() => Test.pure(name.name)(() => run)))
  }

  override def name : String = self.getClass.getName.replace("$", "")
  private def pureSpec(args: List[String]) = synchronized {
    if(!isInitialized) isInitialized = true
      val argsFilter = Filters.filterTests(this.name)(args)
      val filteredTests = testSeq.collect { case (name, test) if argsFilter(name) => test }
      fs2.Stream.emits(filteredTests.map(execute => execute()))
  }

  override def spec(args: List[String]) = pureSpec(args).covary[F]

  override def runUnsafe(args: List[String])(report: TestOutcome => Unit) =
    pureSpec(args).compile.toVector.foreach(report)


  private[this] var testSeq = Seq.empty[(TestName, () => TestOutcome)]
  def plan: List[TestName] = testSeq.map(_._1).toList

  private[this] var isInitialized = false
}

private[weaver] object initError extends AssertionError(
      "Cannot define new tests after TestSuite was initialized"
    )

