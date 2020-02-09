package weaver

import cats.~>
import cats.implicits._
import cats.effect.{ ContextShift, Effect, Timer, IO, Resource }
import fs2.Stream

import scala.scalajs.reflect.annotation.EnableReflectiveInstantiation
import cats.effect.ConcurrentEffect

// Just a non-parameterized marker trait to help SBT's test detection logic.
trait BaseSuiteClass {}

trait Suite[F[_]] extends BaseSuiteClass {
  def name: String
  def spec(args: List[String]): Stream[F, TestOutcome]
}

// format: off
@EnableReflectiveInstantiation
trait EffectSuite[F[_]] extends Suite[F] with Expectations.Helpers { self =>

  implicit def effect : Effect[F]

  /**
   * Raise an error that leads to the running test being tagged as "cancelled".
   */
  def cancel(reason: String)(pos: SourceLocation): F[Unit] =
    effect.raiseError(new CanceledException(Some(reason), pos))

  /**
   * Raises an error that leads to the running test being tagged as "ignored"
   */
  def ignore(reason: String)(pos: SourceLocation): F[Unit] =
    effect.raiseError(new IgnoredException(Some(reason), pos))

  /**
   * Expect macro
   */
  def expect = new Expect
  def assert = new Expect

  override def name : String = self.getClass.getName.replace("$", "")

   val toIOK : F ~> IO = new (F ~> IO){
    def apply[A](fa : F[A]) : IO[A] = effect.toIO(fa)
  }
  private[weaver] def ioSpec(args : List[String]) : fs2.Stream[IO, TestOutcome] = spec(args).translate(toIOK)
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


  def pureTest(name: String)(run : => Expectations) : IO[TestOutcome] = Test[IO](name)(_ => IO(run)).compile
  def simpleTest(name:  String)(run : IO[Expectations]) : IO[TestOutcome] = Test[IO](name)(_ => run).compile
  def loggedTest(name: String)(run : Log[IO] => IO[Expectations]) : IO[TestOutcome] = Test[IO](name)(run).compile

}

trait MutableFSuite[F[_]] extends ConcurrentEffectSuite[F]  {

  type Res
  def sharedResource : Resource[F, Res]

  def maxParallelism : Int = 10000
  implicit def timer: Timer[F]

  def registerTest(name: String)(f: Res => Log[F] => F[Expectations]): Unit =
    synchronized {
      if (isInitialized) throw initError()
      val test = (res : Res) => Test[F](name)(f(res))
      testSeq = testSeq :+ (name -> test)
    }

  def pureTest(name: String)(run : => Expectations) :  Unit = registerTest(name)(_ => _ => effect.delay(run))
  def simpleTest(name:  String)(run: => F[Expectations]) : Unit = registerTest(name)(_ => _ => effect.suspend(run))
  def loggedTest(name: String)(run: Log[F] => F[Expectations]) : Unit = registerTest(name)(_ => log => run(log))
  def test(name: String)(run : (Res, Log[F]) => F[Expectations]) : Unit = registerTest(name)(run.curried)

  implicit def singleExpectationConversion(e: SingleExpectation)(implicit loc: SourceLocation): F[Expectations] =
    Expectations.fromSingle(e).pure[F]

  implicit def expectationsConversion(e: Expectations): F[Expectations] =
    e.pure[F]

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
        result <- if (parallism > 1 ) testStream.parEvalMap(parallism)(_.compile)
                  else testStream.evalMap(_.compile)
      } yield result
    }

  private[this] var testSeq = Seq.empty[(String, Res => Test[F])]
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
