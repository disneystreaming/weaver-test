import cats.effect.IO

package object weaver {

  /**
   * Extend this when each test in the suite returns an `Resource[IO, Res]` for
   * some shared resource `Res`
   */
  type IOSuite = MutableIOSuite

  /**
   * Extend this when each test in the suite returns an `IO[_]`
   */
  type SimpleIOSuite = SimpleMutableIOSuite

  type GlobalResource = IOGlobalResource
  type GlobalRead     = GlobalResourceF.Read[IO]
  type GlobalWrite    = GlobalResourceF.Write[IO]

  /**
   * Extend this when each test in the suite is pure and does not return `IO[_]`
   */
  type FunSuite = FunSuiteIO
}
