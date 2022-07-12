package weaver
package discipline

import scala.util.control.NoStackTrace

import cats.data.Kleisli
import cats.effect.Resource
import cats.implicits._
import fs2.Stream
import org.scalacheck.Prop.Arg
import org.scalacheck.{ Test => ScalaCheckTest }
import org.scalacheck.Test._
import org.scalacheck.util.Pretty
import org.typelevel.discipline.Laws
import org.scalacheck.Prop
import Discipline._

trait Discipline { self: FunSuiteAux =>

  def checkAll(
      name: TestName,
      ruleSet: Laws#RuleSet,
      parameters: Parameters => Parameters = identity): Unit =
    ruleSet.all.properties.toList.foreach {
      case (id, prop) =>
        test(name.copy(s"${name.name}: $id")) {
          executeProp(prop, name.location, parameters)
        }
    }

}

trait DisciplineFSuite[F[_]] extends EffectSuite[F] {

  type Res
  def sharedResource: Resource[F, Res]

  def maxParallelism: Int = 10000

  protected def registerTest(tests: Res => F[List[F[TestOutcome]]]): Unit =
    synchronized {
      if (isInitialized) throw initError()
      testsSeq = testsSeq :+ tests
    }

  def checkAll(
      name: TestName,
      parameters: Parameters => Parameters = identity
  ): PartiallyAppliedCheckAll = new PartiallyAppliedCheckAll(name, parameters)

  class PartiallyAppliedCheckAll(
      name: TestName,
      parameters: Parameters => Parameters) {
    def apply(run: => F[Laws#RuleSet]): Unit = apply(_ => run)
    def apply(run: Res => F[Laws#RuleSet]): Unit =
      registerTest(
        Kleisli(run).map(_.all.properties.toList.map {
          case (id, prop) => Test(
              s"${name.name}: $id",
              effectCompat.effect.delay(
                executeProp(prop, name.location, parameters)
              )
            )
        }).run
      )

    // this alias helps using pattern matching on `Res`
    def usingRes(run: Res => F[Laws#RuleSet]): Unit = apply(run)

    def pure(run: Res => Laws#RuleSet): Unit = apply(run.andThen(_.pure[F]))
  }

  override def spec(args: List[String]): Stream[F, TestOutcome] =
    synchronized {
      if (!isInitialized) isInitialized = true
      val parallism = math.max(1, maxParallelism)
      Stream.resource(sharedResource).flatMap { res =>
        Stream.emits(testsSeq).covary[F]
          .parEvalMap(parallism)(_.apply(res))
          .flatMap(Stream.emits)
          .parEvalMap(parallism)(identity)
      }
    }

  private[this] var testsSeq =
    Seq.empty[Res => F[List[F[TestOutcome]]]]

  private[this] var isInitialized = false

  private[this] def initError() = new AssertionError(
    "Cannot define new tests after TestSuite was initialized")
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

  private[discipline] def executeProp(
      prop: Prop,
      location: SourceLocation,
      parameters: Parameters => Parameters
  ): Expectations = {
    import Expectations.Helpers._

    ScalaCheckTest.check(prop)(parameters).status match {
      case Passed | Proved(_) => success
      case Exhausted          => failure("Property exhausted")(location)
      case Failed(input, _) =>
        failure(s"Property violated \n" + printArgs(input))(location)
      case PropException(input, cause, _) =>
        throw PropertyException(input, cause)
    }
  }

  private def printArgs(args: Seq[Arg[Any]]) =
    args.zipWithIndex.map { case (arg, idx) =>
      s"ARG $idx: " + arg.prettyArg(Pretty.defaultParams)
    }.mkString("\n")
}
