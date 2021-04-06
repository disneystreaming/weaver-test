package weaver
package discipline

import scala.util.control.NoStackTrace

import org.scalacheck.Prop.Arg
import org.scalacheck.Test
import org.scalacheck.Test.{ Exhausted, Failed, Passed, PropException, Proved }
import org.scalacheck.util.Pretty
import org.typelevel.discipline.Laws

trait Discipline { self: FunSuiteAux =>

  import Expectations.Helpers._
  import Discipline._

  def checkAll(
      name: TestName,
      ruleSet: Laws#RuleSet,
      parameters: Test.Parameters => Test.Parameters = identity): Unit =
    ruleSet.all.properties.toList.foreach {
      case (id, prop) =>
        test(name.copy(s"${name.name}: $id")) {
          Test.check(prop)(parameters).status match {
            case Passed | Proved(_) => success
            case Exhausted          => failure("Property exhausted")(name.location)
            case Failed(input, _) =>
              failure(s"Property violated \n" + printArgs(input))(name.location)
            case PropException(input, cause, _) =>
              throw PropertyException(input, cause)
          }
        }
    }
}

object Discipline {

  private[discipline] case class PropertyException(
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
