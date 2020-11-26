package weaver
package framework

import sbt.testing.TaskDef

/**
 * An interface for loading weaver suites from a task def.
 */
trait SuiteLoader[F[_]] {
  def apply(TaskDef: TaskDef): Option[SuiteRef]

  sealed trait SuiteRef
  case class ModuleSuite(suite: EffectSuite[F])               extends SuiteRef
  case class GlobalResourcesRef(init: GlobalResourcesInit[F]) extends SuiteRef
  case class ResourcesSharingSuite(
      build: GlobalResources.Read[F] => EffectSuite[F])
      extends SuiteRef
}
