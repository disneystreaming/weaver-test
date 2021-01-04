package weaver
package scalacheck

import cats.effect.IO
import cats.syntax.all._
import cats.{Applicative, Defer, Show}

import org.scalacheck.rng.Seed
import org.scalacheck.{ Arbitrary, Gen }

import CECompat.Ref

trait IOCheckers extends Checkers[IO] {
  self: EffectSuite[IO] =>
}

trait Checkers[F[_]] {
  self: EffectSuite[F] =>
  import Checkers._

  type PropF[A] = Prop[F, A]

  private def liftProp[A, B: PropF](f: A => B): A => F[Expectations] = {
    f andThen (b => Prop[F, B].lift(b))
  }

  // Configuration for property-based tests
  def checkConfig: CheckConfig = CheckConfig.default

  def forall[A1: Arbitrary: Show, B: PropF](f: A1 => B)(
      implicit loc: SourceLocation): F[Expectations] =
    forall(implicitly[Arbitrary[A1]].arbitrary)(liftProp(f))

  def forall[A1: Arbitrary: Show, A2: Arbitrary: Show, B: PropF](f: (
      A1,
      A2) => B)(
      implicit loc: SourceLocation): F[Expectations] =
    forall(implicitly[Arbitrary[(A1, A2)]].arbitrary)(liftProp(
      f.tupled))

  def forall[
      A1: Arbitrary: Show,
      A2: Arbitrary: Show,
      A3: Arbitrary: Show,
      B: PropF](
      f: (A1, A2, A3) => B)(
      implicit loc: SourceLocation): F[Expectations] = {
    implicit val tuple3Show: Show[(A1, A2, A3)] = {
      case (a1, a2, a3) => s"(${a1.show},${a2.show},${a3.show})"
    }
    forall(implicitly[Arbitrary[(A1, A2, A3)]].arbitrary)(liftProp(
      f.tupled))
  }

  def forall[
      A1: Arbitrary: Show,
      A2: Arbitrary: Show,
      A3: Arbitrary: Show,
      A4: Arbitrary: Show,
      B: PropF
  ](f: (A1, A2, A3, A4) => B)(
      implicit loc: SourceLocation): F[Expectations] = {
    implicit val tuple3Show: Show[(A1, A2, A3, A4)] = {
      case (a1, a2, a3, a4) => s"(${a1.show},${a2.show},${a3.show},${a4.show})"
    }
    forall(implicitly[Arbitrary[(A1, A2, A3, A4)]].arbitrary)(
      liftProp(f.tupled))
  }

  def forall[
      A1: Arbitrary: Show,
      A2: Arbitrary: Show,
      A3: Arbitrary: Show,
      A4: Arbitrary: Show,
      A5: Arbitrary: Show,
      B: PropF
  ](f: (A1, A2, A3, A4, A5) => B)(
      implicit loc: SourceLocation): F[Expectations] = {
    implicit val tuple3Show: Show[(A1, A2, A3, A4, A5)] = {
      case (a1, a2, a3, a4, a5) =>
        s"(${a1.show},${a2.show},${a3.show},${a4.show},${a5.show})"
    }
    forall(implicitly[Arbitrary[(A1, A2, A3, A4, A5)]].arbitrary)(
      liftProp(f.tupled))
  }

  def forall[
      A1: Arbitrary: Show,
      A2: Arbitrary: Show,
      A3: Arbitrary: Show,
      A4: Arbitrary: Show,
      A5: Arbitrary: Show,
      A6: Arbitrary: Show,
      B: PropF
  ](f: (A1, A2, A3, A4, A5, A6) => B)(
      implicit loc: SourceLocation): F[Expectations] = {
    implicit val tuple3Show: Show[(A1, A2, A3, A4, A5, A6)] = {
      case (a1, a2, a3, a4, a5, a6) =>
        s"(${a1.show},${a2.show},${a3.show},${a4.show},${a5.show},${a6.show})"
    }
    forall(implicitly[Arbitrary[(A1, A2, A3, A4, A5, A6)]].arbitrary)(
      liftProp(f.tupled))
  }

  /** ScalaCheck test parameters instance. */
  val numbers = fs2.Stream.iterate(1)(_ + 1)

  def forall[A: Show](gen: Gen[A])(f: A => F[Expectations])(
      implicit loc: SourceLocation): F[Expectations] =
    Ref[F].of(Status.start[A]).flatMap(forall_(gen, f))

  private def forall_[A: Show](gen: Gen[A], f: A => F[Expectations])(
      state: Ref[F, Status[A]])(
      implicit loc: SourceLocation): F[Expectations] = {
    paramStream
      .parEvalMapUnordered(checkConfig.perPropertyParallelism) {
        testOneTupled(gen, state, f)
      }
      .takeWhile(_.shouldContinue, takeFailure = true)
      .takeRight(1) // getting the first error (which finishes the stream)
      .compile
      .last
      .map {
        case Some(status) => status.endResult
        case None         => Expectations.Helpers.success
      }
  }

  private def paramStream: fs2.Stream[F, (Gen.Parameters, Seed)] = {
    val initial = startSeed(
      Gen.Parameters.default
        .withSize(checkConfig.maximumGeneratorSize)
        .withInitialSeed(checkConfig.initialSeed.map(Seed(_))))

    fs2.Stream.iterate(initial) {
      case (p, s) => (p, s.slide)
    }
  }

  private def testOneTupled[T: Show](
      gen: Gen[T],
      state: Ref[F, Status[T]],
      f: T => F[Expectations])(ps: (Gen.Parameters, Seed)) =
    testOne(gen, state, f)(ps._1, ps._2)

  private def testOne[T: Show](
      gen: Gen[T],
      state: Ref[F, Status[T]],
      f: T => F[Expectations])(
      params: Gen.Parameters,
      seed: Seed): F[Status[T]] = {
    Defer[F](self.effect).defer {
      gen(params, seed)
        .traverse(x => f(x).map(x -> _))
        .flatTap {
          case Some((_, ex)) if ex.run.isValid => state.update(_.addSuccess)
          case Some((t, ex))                   => state.update(_.addFailure(t.show, ex))
          case None                            => state.update(_.addDiscard)
        }
        .productR(state.get)
    }
  }

  def startSeed(params: Gen.Parameters): (Gen.Parameters, Seed) =
    params.initialSeed match {
      case Some(seed) => (params.withNoInitialSeed, seed)
      case None       => (params, Seed.random())
    }

  private[scalacheck] case class Status[T](
      succeeded: Int,
      discarded: Int,
      failure: Option[Expectations]
  ) {
    def addSuccess: Status[T] =
      if (failure.isEmpty) copy(succeeded = succeeded + 1) else this
    def addDiscard: Status[T] =
      if (failure.isEmpty) copy(discarded = discarded + 1) else this
    def addFailure(input: String, exp: Expectations): Status[T] =
      if (failure.isEmpty) {
        val ith = succeeded + discarded + 1
        val failure = Expectations.Helpers
          .failure(s"Property test failed on try $ith with input $input")
          .and(exp)
        copy(failure = Some(failure))
      } else this

    def shouldStop =
      failure.isDefined ||
        succeeded >= checkConfig.minimumSuccessful ||
        discarded >= checkConfig.maximumDiscardRatio

    def shouldContinue = !shouldStop

    def endResult(implicit loc: SourceLocation) = failure.getOrElse {
      if (succeeded < checkConfig.minimumSuccessful)
        Expectations.Helpers.failure(
          s"Discarded more inputs ($discarded) than allowed")
      else Expectations.Helpers.success
    }
  }
  private object Status {
    def start[T] = Status[T](0, 0, None)
  }

}

private[weaver] object Checkers {
  trait Prop[F[_], A] {
    def lift(a: A): F[Expectations]
  }

  object Prop {
    def apply[F[_], B](implicit ev: Prop[F, B]): Prop[F, B] = ev

    implicit def wrap[F[_]: Applicative]: Prop[F, Expectations] =
      new Prop[F, Expectations] {
        def lift(a: Expectations): F[Expectations] = Applicative[F].pure(a)
      }

    implicit def unwrapped[F[_]]: Prop[F, F[Expectations]] =
      new Prop[F, F[Expectations]] {
        def lift(a: F[Expectations]): F[Expectations] = a
      }

  }

}
