package weaver

import cats.data.NonEmptyVector

object TestErrorFormatter {

  def formatStackTrace(
      ex: Throwable,
      traceLimit: Option[Int]): Vector[String] = {
    val (traceElements, truncated) = {
      val tr = ex.getStackTrace.toVector

      traceLimit.fold(tr -> false) { limit =>
        if (tr.length <= limit) tr -> false
        else if (limit == 0) Vector[StackTraceElement]() -> false
        else tr.take(limit)                              -> true
      }
    }

    val newStackTrace = groupStackTraceElements(traceElements)

    newStackTrace match {
      case Nil => Vector()
      case h :: t =>
        renderGroupedStackTrace(NonEmptyVector.of(h, t: _*), truncated)
    }
  }

  private sealed trait TraceOutput
  private case class Element(st: StackTraceElement) extends TraceOutput {
    def location: String = s"${st.getFileName}:${st.getLineNumber}"
  }
  private case class Snip(pack: String) extends TraceOutput

  private def exclusion: StackTraceElement => Option[String] = { el =>
    val exclusions = Set(
      "cats.effect.internals",
      "java.util.concurrent",
      "zio.internal",
      "java.lang.Thread"
    )

    exclusions.find(el.toString.contains)
  }

  private def groupStackTraceElements(
      elements: Vector[StackTraceElement]): List[TraceOutput] = {
    val traces                      = new scala.collection.mutable.ListBuffer[TraceOutput]
    var latest: Option[TraceOutput] = None

    def append(el: TraceOutput) = {
      traces.append(el); latest = Some(el)
    }

    elements.map(el => el -> exclusion(el)).foreach {
      case (el, exclusion) =>
        (latest, exclusion) match {
          case (Some(Snip(pack)), Some(excl)) if pack == excl => ()
          case (Some(Snip(pack)), Some(excl)) if pack != excl =>
            append(Snip(excl))
          case (_, None) =>
            append(Element(el))
          case (None, Some(excl)) =>
            append(Snip(excl))
          case (Some(_: Element), Some(excl)) =>
            append(Snip(excl))

          case (Some(Snip(_)), Some(_)) => throw new Exception("wat")
        }
    }

    traces.toList
  }

  private def renderGroupedStackTrace(
      stackTrace: NonEmptyVector[TraceOutput],
      truncated: Boolean): Vector[String] = {

    import cats.syntax.all._

    val (snipPrefix, snipSuffix) = ("<snipped>", ".<...>")

    val sources = stackTrace.map {
      case el: Element => el.location
      case _: Snip     => snipPrefix
    }

    val maxLen = sources.map(_.length).toList.max

    val formatted = stackTrace.map {
      case el: Element =>
        s"${el.location.padTo(maxLen + 4, ' ')}${el.st.getClassName}#${el.st.getMethodName}"
      case Snip(pack) =>
        s"${snipPrefix.padTo(maxLen + 4, ' ')}$pack$snipSuffix"
    }

    val truncatedMaybe = if (truncated) Vector("...") else Vector()

    formatted.toVector ++ truncatedMaybe
  }

}
