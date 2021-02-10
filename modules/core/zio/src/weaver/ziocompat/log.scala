package weaver
package ziocompat

import zio._

object log {

  def info(
      msg: => String,
      ctx: Map[String, String] = Map.empty,
      cause: Throwable = null)(implicit loc: SourceLocation) =
    ZIO.accessM[LogModule](_.get[LogModule.Service].info(msg, ctx, cause))

  def debug(
      msg: => String,
      ctx: Map[String, String] = Map.empty,
      cause: Throwable = null)(implicit loc: SourceLocation) =
    ZIO.accessM[LogModule](_.get[LogModule.Service].debug(msg, ctx, cause))

  def warn(
      msg: => String,
      ctx: Map[String, String] = Map.empty,
      cause: Throwable = null)(implicit loc: SourceLocation) =
    ZIO.accessM[LogModule](_.get[LogModule.Service].warn(msg, ctx, cause))

  def error(
      msg: => String,
      ctx: Map[String, String] = Map.empty,
      cause: Throwable = null)(implicit loc: SourceLocation) =
    ZIO.accessM[LogModule](_.get[LogModule.Service].error(msg, ctx, cause))
}
