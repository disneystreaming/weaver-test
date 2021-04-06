package weaver

import monix.bio.Task

package object monixbiocompat {
  type IOSuite        = MutableIOSuite
  type SimpleIOSuite  = SimpleMutableIOSuite
  type FunSuite       = FunIOSuite
  type GlobalResource = IOGlobalResource
  type GlobalRead     = GlobalResourceF.Read[Task]
  type GlobalWrite    = GlobalResourceF.Write[Task]
}
