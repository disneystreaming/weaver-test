package weaver.ziocompat

import cats.data.Chain

import weaver.Log

import zio._
import zio.clock.Clock

class FiberRefLog(ref: FiberRef[Chain[Log.Entry]], clock: Clock.Service)
    extends LogModule.Service(clock) {
  self =>
  def log(l: => Log.Entry): UIO[Unit] =
    ref.modify(current => ((), current.append(l)))

  def logs: UIO[Chain[Log.Entry]] =
    ref.get
}

object FiberRefLog {
  def apply(
      ref: FiberRef[Chain[Log.Entry]],
      clock: Clock.Service): LogModule.Service =
    new FiberRefLog(ref, clock)
}
