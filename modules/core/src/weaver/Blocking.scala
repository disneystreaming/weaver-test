package weaver

protected[weaver] trait BlockerCompat[F[_]] {
  def block[A](thunk: => A): F[A]
}
