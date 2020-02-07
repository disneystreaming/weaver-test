package weaver

import cats.effect.IO

package object framework {

  type Logger = (String, Event) => IO[Unit]

}
