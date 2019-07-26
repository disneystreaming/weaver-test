package weaver
package testkit

import cats.~>
import cats.effect.{ Effect, Timer, IO }
import fs2.Stream

import scala.scalajs.reflect.annotation.EnableReflectiveInstantiation

trait Suite[F[_]] {
  def name: String
  def spec: Stream[F, TestOutcome]
}

// format: off
@EnableReflectiveInstantiation
trait EffectSuite[F[_]] extends Suite[F] with Assertion.Helpers { self =>

  implicit def effect : Effect[F]
  implicit def timer: Timer[F]

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

  override def name : String = self.getClass.getName.replace("$", "")

  private val toIOK : F ~> IO = new (F ~> IO){
    def apply[A](fa : F[A]) : IO[A] = effect.toIO(fa)
  }
  private[weaver] def ioSpec : fs2.Stream[IO, Event] = spec.translate(toIOK)
}

trait PureIOSuite extends EffectSuite[IO]{
  val ec = scala.concurrent.ExecutionContext.global
  implicit def timer = IO.timer(ec)
  implicit def cs = IO.contextShift(ec)
  implicit def effect = IO.ioEffect

  def simpleTest(name: String)(run : => Assertion) : IO[TestOutcome] = Test[IO](name)(_ => IO(run)).compile
  def test(name:  String)(run : IO[Assertion]) : IO[TestOutcome] = Test[IO](name)(_ => run).compile
  def loggedTest(name: String)(run : Log[IO] => IO[Assertion]) : IO[TestOutcome] = Test[IO](name)(run).compile

}

trait MutableIOSuite extends EffectSuite[IO] {
  val ec = scala.concurrent.ExecutionContext.global
  implicit def timer = IO.timer(ec)
  implicit def cs = IO.contextShift(ec)
  implicit def effect = IO.ioEffect

  def maxParallelism : Int = 10000

  def registerTest(name: String)(f: Log[IO] => IO[Assertion]): Unit =
    synchronized {
      if (isInitialized) throw initError()
      propertiesSeq = propertiesSeq :+ Test[IO](name)(f)
    }

  def simpleTest(name: String)(run : => Assertion) :  Unit = registerTest(name)(_ => IO(run))
  def test(name:  String)(run : IO[Assertion]) : Unit = registerTest(name)(_ => run)
  def loggedTest(name: String)(run : Log[IO] => IO[Assertion]) : Unit = registerTest(name)(run)

  lazy val spec: Stream[IO, TestOutcome] =
    synchronized {
      if (!isInitialized) isInitialized = true
      Stream.emits(propertiesSeq).lift[IO].parEvalMap(math.max(1, maxParallelism))(_.compile)
    }

  private[this] var propertiesSeq = Seq.empty[Test[IO]]
  private[this] var isInitialized = false

  private[this] def initError() =
    new AssertionError(
      "Cannot define new tests after TestSuite was initialized"
    )

}

