package weaver.docs

import weaver._
import cats.effect._
import cats.effect.concurrent.Ref
import cats.data.NonEmptyChain
import scala.concurrent.ExecutionContext.Implicits.global

object Output {
  implicit val cs          = IO.contextShift(global)
  private val ansiColorRgx = """\u001b\[([;\d]*)m""".r

  def removeASCIIColors(str: String) =
    ansiColorRgx.replaceAllIn(str, "")

  def format(s: String) =
    removeTrailingNewLine(removeASCIIColors(s.replace("repl.SessionApp", "")))

  def removeTrailingNewLine(s: String) = {
    if (s.endsWith("\n")) s.substring(0, s.length - 2) else s
  }

  def runSuites(s: Suite[IO]*): IO[String] = {
    val header = "```text"
    val footer = "```"

    for {
      buf <- Ref.of[IO, NonEmptyChain[String]](NonEmptyChain(header))
      printLine = (s: String) => buf.update(_.append(format(s)))
      runner    = new Runner[IO](Nil, 10)(s => printLine(s))

      _     <- runner.run(fs2.Stream(s: _*))
      _     <- printLine(footer)
      value <- buf.get
    } yield value.reduceLeft(_ + "\n" + _)
  }
}
