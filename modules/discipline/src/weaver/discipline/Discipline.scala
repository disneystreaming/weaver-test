package weaver
package discipline

import org.scalacheck.Test
import org.typelevel.discipline.Laws
import org.scalacheck.Prop.Arg
import org.scalacheck.util.Pretty

import scala.util.control.NoStackTrace

import org.scalacheck.Test.{ Passed, Proved, Exhausted, Failed, PropException }

trait Discipline { self: EffectSuiteAux =>

  import Expectations.Helpers._
  import Discipline._

  def checkAll(name: String, ruleSet: Laws#RuleSet): Unit =
    ruleSet.all.properties.toList.foreach {
      case (id, prop) =>
        pureTest(s"$name: $id") {
          Test.check(prop)(identity).status match {
            case Passed | Proved(_) => success
            case Exhausted          => failure("Property exhausted")
            case Failed(input, _) =>
              failure(s"Property violated \n" + printArgs(input))
            case PropException(input, cause, _) =>
              throw DisciplineException(input, cause)
          }
        }
    }
}

object Discipline {

  private[discipline] case class DisciplineException(
      input: List[Arg[Any]],
      cause: Throwable)
      extends Exception(cause)
      with NoStackTrace {
    override def getMessage() =
      "Property failed with an exception\n" + printArgs(input)
  }

  private def printArgs(args: Seq[Arg[Any]]) =
    args.zipWithIndex.map { case (arg, idx) =>
      s"ARG $idx: " + arg.prettyArg(Pretty.defaultParams)
    }.mkString("\n")
}
