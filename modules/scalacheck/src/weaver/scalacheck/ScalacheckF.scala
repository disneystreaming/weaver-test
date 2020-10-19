package weaver
package scalacheck

import scala.util.control.NoStackTrace

import cats.data.Validated.{Invalid, Valid}
import cats.effect.IO
import cats.syntax.all._

import Expectations.Helpers._
import org.scalacheck.Test.{Exhausted, Failed, Passed, PropException, Proved}
import org.scalacheck.effect.PropF
import org.scalacheck.util.Pretty

import org.scalacheck.Prop.Arg

trait ScalacheckIO extends ScalacheckF[IO] {
  self: MutableFSuite[IO] =>
}

case class Falsified(args: List[Arg[Any]], causedBy: Throwable)
    extends Exception("", causedBy)
    with NoStackTrace {
  override def toString(): String =
    """
        |Property falsified for the following arguments:
        |
        """.stripMargin + renderArguments

  private def renderArguments = {
    args.zipWithIndex.map { case (arg, i) =>
      val lab = if (arg.label == "") "ARG_" + i.toString() else arg.label
      lab + ": " + arg.prettyArg.apply(Pretty.Params(1))
    }.mkString("\n")
  }

  override def getMessage(): String = toString()
}

trait ScalacheckF[F[_]] { self: MutableFSuite[F] =>

  implicit def toPropF: IO[Expectations] => PropF[IO] = { expectations =>
    PropF.effectOfPropFToPropF(
      expectations.map(toProp)
    )
  }

  implicit def toProp: Expectations => PropF[IO] = { expectations =>
    expectations.run match {
      case Invalid(e) => PropF.exception(e.head)
      case Valid(_)   => PropF.passed
    }
  }

  def propTest(name: TestName)(f: PropF[F])(implicit sl: SourceLocation) = {
    simpleTest(name) {
      f.check().flatMap { result =>
        result.status match {
          case Exhausted => failure("Exhausted")
          case Proved(_) => success
          case Passed    => success
          case PropException(args, e, _) =>
            Falsified(args, e).raiseError[F, Expectations]
          case Failed(args, _) =>
            Falsified(args, null).raiseError[F, Expectations]
        }
      }
    }
  }
}
