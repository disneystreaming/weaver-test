package weaver

import cats.effect.{ IO, Resource }

trait BaseIOSuite extends RunnableSuite[IO] {
  implicit protected def effectCompat: UnsafeRun[IO] = CatsUnsafeRun
  final implicit protected def contextShift          = effectCompat.contextShift
  final implicit protected def timer                 = effectCompat.timer
}

trait PureIOSuite
    extends EffectSuite[IO]
    with BaseIOSuite
    with Expectations.Helpers {

  def pureTest(name: String)(run: => Expectations): IO[TestOutcome] =
    Test[IO](name, IO(run))
  def simpleTest(name: String)(run: IO[Expectations]): IO[TestOutcome] =
    Test[IO](name, run)
  def loggedTest(name: String)(
      run: Log[IO] => IO[Expectations]): IO[TestOutcome] = Test[IO](name, run)

}

trait MutableIOSuite
    extends MutableFSuite[IO]
    with BaseIOSuite
    with Expectations.Helpers

trait SimpleMutableIOSuite extends MutableIOSuite {
  type Res = Unit
  def sharedResource: Resource[IO, Unit] = Resource.pure[IO, Unit](())
}
