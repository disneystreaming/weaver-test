package weaver.ziocompat

import java.util.concurrent.TimeUnit

import cats.data.Chain

import weaver.Log

import zio._
import zio.clock.Clock
import zio.interop.catz._

class RefLog(ref: Ref[Chain[Log.Entry]], clock: Clock.Service)
    extends Log[UIO](clock.currentTime(TimeUnit.MILLISECONDS)) { self =>
  def log(l: => Log.Entry): UIO[Unit] =
    ref.modify(current => ((), current.append(l)))
}

object RefLog {
  def apply(ref: Ref[Chain[Log.Entry]], clock: Clock.Service): Log[UIO] =
    new RefLog(ref, clock)
}
