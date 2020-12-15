package weaver
package ziocompat

import zio._

object log {

  def info(
      msg: => String,
      ctx: Map[String, String] = Map.empty,
      cause: Throwable = null)(implicit loc: SourceLocation) =
    ZIO.accessM[LogModule](_.get[Log[UIO]].info(msg, ctx, cause))

  def debug(
      msg: => String,
      ctx: Map[String, String] = Map.empty,
      cause: Throwable = null)(implicit loc: SourceLocation) =
    ZIO.accessM[LogModule](_.get[Log[UIO]].debug(msg, ctx, cause))

  def warn(
      msg: => String,
      ctx: Map[String, String] = Map.empty,
      cause: Throwable = null)(implicit loc: SourceLocation) =
    ZIO.accessM[LogModule](_.get[Log[UIO]].warn(msg, ctx, cause))

  def error(
      msg: => String,
      ctx: Map[String, String] = Map.empty,
      cause: Throwable = null)(implicit loc: SourceLocation) =
    ZIO.accessM[LogModule](_.get[Log[UIO]].error(msg, ctx, cause))
}
