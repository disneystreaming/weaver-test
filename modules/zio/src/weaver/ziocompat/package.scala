package weaver

import zio.clock.Clock
import zio.console.Console
import zio.system.System
import zio.random.Random

package object ziocompat {

  type BaseEnv = Clock with Console with System with Random

  type Env[R] = SharedResourceModule[R] with BaseEnv

  type ZIOSuite[A]    = MutableZIOSuite[A]
  type SimpleZIOSuite = SimpleMutableZIOSuite

}
