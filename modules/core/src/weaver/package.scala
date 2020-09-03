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

  private object atLine {
    def unapply(testPath: String): Option[(String, Int)] = {
      // Can't use string interpolation in pattern (2.12)
      val members = testPath.split(".line://")
      if (members.size == 2) {
        val suiteName = members(0)
        // Can't use .toIntOption (2.12)
        val maybeLine = scala.util.Try(members(1).toInt).toOption
        maybeLine.map(suiteName -> _)
      } else None
    }
  }

  private[weaver] def filterTests(suiteName: String)(
      args: List[String]): TestConfig => Boolean = {

    def toPredicate(filter: String): Predicate = {
      filter match {

        case atLine(`suiteName`, line) => {
          case (_, indicator) => indicator.line == line
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
