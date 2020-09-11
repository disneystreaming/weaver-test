package weaver.docs

import weaver._
import cats.effect._
import cats.effect.concurrent.Ref
import cats.data.NonEmptyChain
import scala.concurrent.ExecutionContext.Implicits.global

object Output {
  implicit val cs          = IO.contextShift(global)

  def format(s: String) = { 
    Ansi2Html(removeTrailingNewLine(
      removeTrailingNewLine(
        s.replace("repl.MdocSessionApp", "").replace("(none:", "(MyTests.scala:")))
      )
  }

  def removeTrailingNewLine(s: String) = {
    if (s.endsWith("\n")) s.substring(0, s.length - 2) else s
  }

  def runSuites(s: Suite[IO]*): IO[String] = {
    val header = "<div class='terminal'><pre><code class = 'nohighlight'>" 
    val footer = "</code></pre></div>"

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


import fansi.Str

object Ansi2Html extends Function1[String, String] {
  def transition(from: fansi.Attr, to: fansi.Attr) = {
    import fansi._
    (from, to) match {
      case (Underlined.Off, Underlined.On) => "<u>"
      case (Underlined.On, Underlined.Off) => "</u>"
      case (Bold.Off, Bold.On)             => "<b>"
      case (Bold.On, Bold.Off)             => "</b>"
      case (col1, col2) if color.isDefinedAt(col2) =>
        val closing   = if (color.isDefinedAt(col1)) "</span>" else ""
        val nextColor = color(col2)
        s"$closing<span style='color: $nextColor'>"
      case (col1, fansi.Color.Reset) if color.isDefinedAt(col1) =>
        "</span>"
      case _ => ""
    }
  }

  def color: PartialFunction[fansi.Attr, String] = {
    case fansi.Color.Black        => "black"
    case fansi.Color.Red          => "red"
    case fansi.Color.Green        => "green"
    case fansi.Color.Yellow       => "yellow"
    case fansi.Color.Blue         => "blue"
    case fansi.Color.Magenta      => "magenta"
    case fansi.Color.Cyan         => "cyan"
    case fansi.Color.LightGray    => "lightgray"
    case fansi.Color.DarkGray     => "darkgray"
    case fansi.Color.LightRed     => "lightred"
    case fansi.Color.LightGreen   => "lightgreen"
    case fansi.Color.LightYellow  => "lightyellow"
    case fansi.Color.LightBlue    => "lightblue"
    case fansi.Color.LightMagenta => "lightmagenta"
    case fansi.Color.LightCyan    => "lightcyan"
    case fansi.Color.White        => "white"
  }

  def apply(s: String) = {
    val colored            = fansi.Str(s)
    var current: Str.State = 0L

    val categories = fansi.Attr.categories

    val sb = new StringBuilder

    colored.getChars.zip(colored.getColors).map {
      case (character, color) =>
        if (current != color) {
          categories.foreach { cat =>
            sb.append(transition(cat.lookupAttr(current & cat.mask),
                                 cat.lookupAttr(color & cat.mask)))
          }

          current = color
        }
        if(character == ' ')
          sb.append("&nbsp;")
        else if (character == '\n')
          sb.append("<br />")
        else if (character != '\r')
          sb.append(character)
    }

    if (current != 0L) {
      categories.foreach(cat =>
        sb.append(transition(cat.lookupAttr(current & cat.mask),
                             cat.lookupAttr(0L & cat.mask))))
    }

    sb.result()
  }
}
