package weaver

import scala.concurrent.duration.FiniteDuration

import cats.data.Chain
import cats.effect.{ Async, Resource }
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
  implicit protected def effect: Async[EffectType]
}

// format: off
trait EffectSuite[F[_]] extends Suite[F] with EffectSuiteAux with SourceLocation.Here { self =>

  final type EffectType[A] = F[A]
  implicit protected def effectCompat: EffectCompat[F]
  implicit final protected def effect: Async[F] = effectCompat.effect

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

object EffectSuite {

  trait Provider[F[_]]{
    def getSuite : EffectSuite[F]
  }

}

@RunWith(classOf[weaver.junit.WeaverRunner])
abstract class RunnableSuite[F[_]] extends EffectSuite[F] {
  implicit protected def effectCompat: UnsafeRun[EffectType]
  private[weaver] def getEffectCompat: UnsafeRun[EffectType] = effectCompat
  def plan : List[TestName]
  private[weaver] def runUnsafe(args: List[String])(report: TestOutcome => Unit) : Unit =
    effectCompat.unsafeRunSync(run(args)(outcome => effectCompat.effect.delay(report(outcome))))

  def isCI: Boolean = System.getenv("CI") == "true"

  private[weaver] def analyze[Res, F1[_]](testSeq: Seq[(TestName, Res => F1[TestOutcome])], args: List[String]): TagAnalysisResult[Res, F1] = {
    val testsNotIgnored: Seq[(TestName, Res => F1[TestOutcome])] =
      testSeq.filterNot(_._1.tags(TestName.Tags.ignore))

    val testsTaggedOnly: Seq[(TestName, Res => F1[TestOutcome])] =
      testSeq.filter(_._1.tags(TestName.Tags.only))

    val onlyTestsNotIgnored =
      testsTaggedOnly.filter(taggedOnly => testsNotIgnored.contains(taggedOnly))

    val filteredTests = if (onlyTestsNotIgnored.isEmpty) {
      val argsFilter = Filters.filterTests(this.name)(args)
      testsNotIgnored.collect {
        case (name, test) if argsFilter(name) => test
      }
    } else onlyTestsNotIgnored.map(_._2)

    if (testsTaggedOnly.nonEmpty && isCI) {
      val failureOutcomes = testsTaggedOnly.map(_._1).map(onlyNotOnCiFailure)
      TagAnalysisResult.Outcomes(failureOutcomes)
    } else TagAnalysisResult.FilteredTests(filteredTests)
  }


  private[this] def onlyNotOnCiFailure(test: TestName): TestOutcome = {
    val result = Result.Failure(
      msg = "'Only' tag is not allowed when `isCI=true`",
      source = None,
      location = List(test.location)
    )
    TestOutcome(
      name = test.name,
      duration = FiniteDuration(0, "ns"),
      result = result,
      log = Chain.empty
    )
  }

}

private[weaver] sealed trait TagAnalysisResult[Res, F[_]]
object TagAnalysisResult {
  case class Outcomes[Res, F[_]](outcomes: Seq[TestOutcome]) extends TagAnalysisResult[Res, F]
  case class FilteredTests[Res, F[_]](tests: Seq[Res => F[TestOutcome]]) extends TagAnalysisResult[Res, F]
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
  def loggedTest(name: TestName)(run: Log[F] => F[Expectations]) : Unit = registerTest(name)(_ => Test[F](name.name, log => run(log)))
  def test(name: TestName) : PartiallyAppliedTest = new PartiallyAppliedTest(name)

  class PartiallyAppliedTest(name : TestName) {
    def apply(run: => F[Expectations]) : Unit = registerTest(name)(_ => Test(name.name, run))
    def apply(run : Res => F[Expectations]) : Unit = registerTest(name)(res => Test(name.name, run(res)))
    def apply(run : (Res, Log[F]) => F[Expectations]) : Unit = registerTest(name)(res => Test[F](name.name, log => run(res, log)))

    // this alias helps using pattern matching on `Res`
    def usingRes(run : Res => F[Expectations]) : Unit = apply(run)
  }

  override def spec(args: List[String]): Stream[F, TestOutcome] =
    synchronized {
      if (!isInitialized) isInitialized = true
      val parallelism = math.max(1, maxParallelism)

      analyze(testSeq, args) match {
        case TagAnalysisResult.Outcomes(outcomes) => fs2.Stream.emits(outcomes)
        case TagAnalysisResult.FilteredTests(filteredTests)
            if filteredTests.isEmpty =>
          Stream.empty // no need to allocate resources
        case TagAnalysisResult.FilteredTests(filteredTests) => for {
            resource <- Stream.resource(sharedResource)
            tests      = filteredTests.map(_.apply(resource))
            testStream = Stream.emits(tests).covary[F]
            result <- if (parallelism > 1)
              testStream.parEvalMap(parallelism)(identity)(effectCompat.effect)
            else testStream.evalMap(identity)
          } yield result
      }
    }

  private[this] var testSeq: Seq[(TestName, Res => F[TestOutcome])] = Seq.empty

  def plan: List[TestName] = testSeq.map(_._1).toList

  private[this] var isInitialized = false

  private[this] def initError() =
    new AssertionError(
      "Cannot define new tests after TestSuite was initialized"
    )

}

trait FunSuiteAux {
  def test(name: TestName)(run: => Expectations): Unit
}

abstract class FunSuiteF[F[_]] extends RunnableSuite[F] with FunSuiteAux { self =>
  override def test(name: TestName)(run: => Expectations): Unit = synchronized {
    if(isInitialized) throw initError
    testSeq = testSeq :+ (name -> ((_: Unit) => Test.pure(name.name)(() => run)))
  }

  override def name : String = self.getClass.getName.replace("$", "")

  private def pureSpec(args: List[String]): fs2.Stream[fs2.Pure, TestOutcome] = synchronized {
    if(!isInitialized) isInitialized = true
    analyze[Unit, cats.Id](testSeq, args) match {
      case TagAnalysisResult.Outcomes(outcomes) => fs2.Stream.emits(outcomes)
      case TagAnalysisResult.FilteredTests(filteredTests) =>
        fs2.Stream.emits(filteredTests.map(execute => execute(())))
    }
  }

  override def spec(args: List[String]) = pureSpec(args).covary[F]

  override def runUnsafe(args: List[String])(report: TestOutcome => Unit) =
    pureSpec(args).compile.toVector.foreach(report)


  private[this] var testSeq = Seq.empty[(TestName, Unit => TestOutcome)]
  def plan: List[TestName] = testSeq.map(_._1).toList

  private[this] var isInitialized = false
}

private[weaver] object initError extends AssertionError(
      "Cannot define new tests after TestSuite was initialized"
    )
