package weaver

import cats.effect.IO
import weaver.testkit._

package object framework {

  type Logger = (String, Event) => IO[Unit]

  type WithLogger = Logger => IO[Unit]

  type LoggedBracket = WithLogger => IO[Unit]

}
