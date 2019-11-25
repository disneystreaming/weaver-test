package object weaver {
  import java.util.regex.Pattern

  type IOSuite[A]    = MutableIOSuite[A]
  type SimpleIOSuite = SimpleMutableIOSuite
  type Event         = TestOutcome

  object discard {
    def apply[T]: T => Unit = { value =>
      val _ = value
    }
  }

  def colored(color: String)(s: String): String =
    new StringBuilder()
      .append(color)
      .append(s)
      .append(Console.RESET)
      .toString()

  val red = colored(Console.RED) _

  val yellow = colored(Console.YELLOW) _

  val cyan = colored(Console.CYAN) _

  val green = colored(Console.GREEN) _

  private[weaver] def toPattern(filter: String): Pattern = {
    val parts = filter
      .split("\\*", -1)
      .map { // Don't discard trailing empty string, if any.
        case ""  => ""
        case str => Pattern.quote(str)
      }
    Pattern.compile(parts.mkString(".*"))
  }

  type TestName = String
  private[weaver] def filterTests(suiteName: String)(
      args: List[String]): TestName => Boolean = {
    import scala.util.Try
    val maybePattern = for {
      index <- Option(args.indexOf("-o"))
        .orElse(Option(args.indexOf("--only")))
        .filter(_ >= 0)
      regexStr <- Try(args(index + 1)).toOption
    } yield toPattern(regexStr)
    testName => {
      val fullName = suiteName + "." + testName
      maybePattern.forall(_.matcher(fullName).matches())
    }
  }

}
