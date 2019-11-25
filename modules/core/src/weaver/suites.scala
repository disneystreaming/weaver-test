package weaver

import cats.~>
import cats.effect.{ Effect, Timer, IO }
import fs2.Stream

import scala.scalajs.reflect.annotation.EnableReflectiveInstantiation
import cats.effect.Resource
import cats.effect.ContextShift

// Just a non-parameterized marker trait to help SBT's test detection logic.
trait BaseSuiteClass {}

trait Suite[F[_]] extends BaseSuiteClass {
  def name: String
  def spec: Stream[F, TestOutcome]
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

  private val toIOK : F ~> IO = new (F ~> IO){
    def apply[A](fa : F[A]) : IO[A] = effect.toIO(fa)
  }
  private[weaver] def ioSpec : fs2.Stream[IO, TestOutcome] = spec.translate(toIOK)
}

trait PureIOSuite extends EffectSuite[IO]{
  val ec = scala.concurrent.ExecutionContext.global
  implicit def timer = IO.timer(ec)
  implicit def cs = IO.contextShift(ec)
  implicit def effect = IO.ioEffect

  def pureTest(name: String)(run : => Expectations) : IO[TestOutcome] = Test[IO](name)(_ => IO(run)).compile
  def simpleTest(name:  String)(run : IO[Expectations]) : IO[TestOutcome] = Test[IO](name)(_ => run).compile
  def loggedTest(name: String)(run : Log[IO] => IO[Expectations]) : IO[TestOutcome] = Test[IO](name)(run).compile

}

trait MutableIOSuite[Res] extends EffectSuite[IO] {

  val ec = scala.concurrent.ExecutionContext.global
  implicit def timer : Timer[IO] = IO.timer(ec)
  implicit def cs : ContextShift[IO] = IO.contextShift(ec)
  implicit def effect : Effect[IO] = IO.ioEffect

  def sharedResource : Resource[IO, Res]

  def maxParallelism : Int = 10000

  def registerTest(name: String)(f: Res => Log[IO] => IO[Expectations]): Unit =
    synchronized {
      if (isInitialized) throw initError()
      testSeq = testSeq :+ ((res : Res) => Test[IO](name)(f(res)))
    }

  def pureTest(name: String)(run : => Expectations) :  Unit = registerTest(name)(_ => _ => IO(run))
  def simpleTest(name:  String)(run: => IO[Expectations]) : Unit = registerTest(name)(_ => _ => IO.suspend(run))
  def loggedTest(name: String)(run: Log[IO] => IO[Expectations]) : Unit = registerTest(name)(_ => log => run(log))
  def test(name: String)(run : (Res, Log[IO]) => IO[Expectations]) : Unit = registerTest(name)(run.curried)

  lazy val spec: Stream[IO, TestOutcome] =
    synchronized {
      if (!isInitialized) isInitialized = true
      for {
        resource <- Stream.resource(sharedResource)
        tests = testSeq.map(_.apply(resource))
        result <- Stream.emits(tests).lift[IO].parEvalMap(math.max(1, maxParallelism))(_.compile)
      } yield result
    }

  private[this] var testSeq = Seq.empty[Res => Test[IO]]
  private[this] var isInitialized = false

  private[this] def initError() =
    new AssertionError(
      "Cannot define new tests after TestSuite was initialized"
    )

}

trait SimpleMutableIOSuite extends MutableIOSuite[Unit]{
  def sharedResource: Resource[IO, Unit] = Resource.pure(())
}
