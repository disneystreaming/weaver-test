package weaver

import cats.effect.{ IO, Resource }

abstract class PureIOSuite
    extends RunnableSuite[IO]
    with EffectSuite[IO]
    with BaseIOSuite
    with Expectations.Helpers {

  def pureTest(name: String)(run: => Expectations): IO[TestOutcome] =
    Test[IO](name, IO(run))
  def simpleTest(name: String)(run: IO[Expectations]): IO[TestOutcome] =
    Test[IO](name, run)
  def loggedTest(name: String)(
      run: Log[IO] => IO[Expectations]): IO[TestOutcome] = Test[IO](name, run)

}

abstract class MutableIOSuite
    extends MutableFSuite[IO]
    with BaseIOSuite
    with Expectations.Helpers

abstract class SimpleMutableIOSuite extends MutableIOSuite {
  type Res = Unit
  def sharedResource: Resource[IO, Unit] = Resource.pure[IO, Unit](())
}
