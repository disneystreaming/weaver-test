package weaver.zio

import weaver.SourceLocation

import zio.ZIO

object log {

  def info(
      msg: => String,
      ctx: Map[String, String] = Map.empty,
      cause: Throwable = null)(implicit loc: SourceLocation) =
    ZIO.accessM[LogModule](_.log.info(msg, ctx, cause))

  def debug(
      msg: => String,
      ctx: Map[String, String] = Map.empty,
      cause: Throwable = null)(implicit loc: SourceLocation) =
    ZIO.accessM[LogModule](_.log.debug(msg, ctx, cause))

  def warn(
      msg: => String,
      ctx: Map[String, String] = Map.empty,
      cause: Throwable = null)(implicit loc: SourceLocation) =
    ZIO.accessM[LogModule](_.log.warn(msg, ctx, cause))

  def error(
      msg: => String,
      ctx: Map[String, String] = Map.empty,
      cause: Throwable = null)(implicit loc: SourceLocation) =
    ZIO.accessM[LogModule](_.log.error(msg, ctx, cause))
}
