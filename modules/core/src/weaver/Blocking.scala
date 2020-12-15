package weaver

private[weaver] trait BlockerCompat[F[_]] {
  def block[A](thunk: => A): F[A]
}
