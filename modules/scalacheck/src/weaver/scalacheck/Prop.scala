package weaver.scalacheck

import weaver.Expectations
import cats.Applicative
import weaver.SingleExpectation
import weaver.SourceLocation
import cats.Functor

case class Prop[F[_]](value: F[Expectations])

object Prop {

  implicit def feConversion[F[_]: Applicative](fe: Expectations): Prop[F] =
    Prop[F](Applicative[F].pure(fe))

  implicit def eConversion[F[_]](fe: F[Expectations]): Prop[F] = Prop[F](fe)

  implicit def sConversion[F[_]: Applicative](s: SingleExpectation)(
      implicit loc: SourceLocation): Prop[F] =
    Prop[F](Applicative[F].pure(Expectations.fromSingle(s)))

  implicit def fsInstance[F[_]: Functor](fs: F[SingleExpectation])(
      implicit loc: SourceLocation): Prop[F] =
    Prop[F](Functor[F].map(fs)(Expectations.fromSingle(_)))

}
