package weaver

import java.util.regex.Pattern

private[weaver] object Filters {

  private[weaver] def toPattern(filter: String): Pattern = {
    val parts = filter
      .split("\\*", -1)
      .map { // Don't discard trailing empty string, if any.
        case ""  => ""
        case str => Pattern.quote(str)
      }
    Pattern.compile(parts.mkString(".*"))
  }

  private type Predicate = TestName => Boolean

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
      args: List[String]): TestName => Boolean = {

    def toPredicate(filter: String): Predicate = {
      filter match {

        case atLine(`suiteName`, line) => {
          case TestName(_, indicator) => indicator.line == line
        }
        case regexStr => {
          case TestName(name, _) =>
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
    testId => maybePattern.forall(_.apply(testId))
  }

}
