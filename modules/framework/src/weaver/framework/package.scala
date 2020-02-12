package weaver

import cats.effect.IO

package object framework {

  type DeferredLogger = (String, Event) => IO[Unit]

}
