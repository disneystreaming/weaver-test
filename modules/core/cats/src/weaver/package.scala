import cats.effect.IO

package object weaver {

  type IOSuite        = MutableIOSuite
  type IOForEachSuite = MutableForEachIOSuite
  type SimpleIOSuite  = SimpleMutableIOSuite
  type GlobalResource = IOGlobalResource
  type GlobalRead     = GlobalResourceF.Read[IO]
  type GlobalWrite    = GlobalResourceF.Write[IO]
  type FunSuite       = FunSuiteIO
}
