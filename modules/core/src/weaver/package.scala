package object weaver {
  import java.util.regex.Pattern

  type IOSuite       = MutableIOSuite
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

  type TestConfig = (String, TestIndicator)

  private type Predicate = TestConfig => Boolean

  private[weaver] def filterTests(suiteName: String)(
      args: List[String]): TestConfig => Boolean = {

    def toPredicate(filter: String): Predicate = {
      filter match {

        case s"${filterSuiteName}.line://${lineNumber}"
            if filterSuiteName == suiteName => {
          case (_, indicator) => indicator.line.toString == lineNumber
        }
        case regexStr => {
          case (name, _) =>
            val fullName = suiteName + "." + name
            toPattern(regexStr).matcher(fullName).matches()
        }
      }
    }

    import scala.util.Try
    val maybePattern = for {
      index <- Option(args.indexOf("-o"))
        .orElse(Option(args.indexOf("--only")))
        .filter(_ >= 0)
      filter <- Try(args(index + 1)).toOption
    } yield toPredicate(filter)
    testName => maybePattern.forall(_.apply((testName._1, testName._2)))
  }

}
