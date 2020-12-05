package weaver
package framework

import sbt.testing.TaskDef

/**
 * An interface for loading weaver suites from a task def.
 */
trait SuiteLoader[F[_]] {
  def apply(TaskDef: TaskDef): Option[Loader]

  sealed trait Loader
  case class SuiteRef(suite: F[EffectSuite[F]])          extends Loader
  case class GlobalResourcesRef(init: GlobalResource[F]) extends Loader
  case class ResourcesSharingSuiteRef(
      build: GlobalResource.Read[F] => F[EffectSuite[F]])
      extends Loader
}
