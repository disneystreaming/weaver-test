package weaver.ziocompat

import cats.data.Chain

import weaver.Log

import zio._

class RefLog(ref: Ref[Chain[Log.Entry]]) extends Log[UIO] { self =>
  def log(l: => Log.Entry): UIO[Unit] =
    ref.modify(current => ((), current.append(l)))
}

object RefLog {
  def apply(ref: Ref[Chain[Log.Entry]]): Log[UIO] = new RefLog(ref)
}
