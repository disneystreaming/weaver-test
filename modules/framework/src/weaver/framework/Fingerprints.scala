package weaver
package framework

import scala.reflect.ClassTag

import cats.effect.Sync

import weaver.internals.Reflection._
import weaver.{ EffectSuite, GlobalResourceF }

import sbt.testing.{ Fingerprint, SubclassFingerprint, TaskDef }

object WeaverFingerprints {
  // format: off
  abstract class Mixin[F[_], SC <: EffectSuite.Provider[F], GRIC <: GlobalResourceF[F]](
      implicit SC: ClassTag[SC], GRIC: ClassTag[GRIC], F : Sync[F]) extends WeaverFingerprints[F] {
    type SuiteClass = SC
    val SuiteClass = SC
    type GlobalResourcesInitClass = GRIC
    val GlobalResourcesInitClass = GRIC
  }
  // format: on
}

/**
 * Contains reference of the classes the build tool will be looking for when
 * searching for tests
 */
abstract class WeaverFingerprints[F[_]](implicit F: Sync[F]) {

  type SuiteClass <: EffectSuite.Provider[F]
  implicit protected def SuiteClass: ClassTag[SuiteClass]
  type GlobalResourcesInitClass <: GlobalResourceF[F]
  implicit protected def GlobalResourcesInitClass: ClassTag[GlobalResourcesInitClass]

  def suiteLoader(classLoader: ClassLoader): SuiteLoader[F] =
    new SuiteLoader[F] {

      def apply(taskDef: TaskDef): Option[Loader] =
        taskDef.fingerprint() match {
          case SuiteFingerprint.matches() =>
            val mkSuite = F.delay {
              val module = loadModule(taskDef.fullyQualifiedName(), classLoader)
              val provider = cast(module)(SuiteClass): EffectSuite.Provider[F]
              provider.getSuite
            }
            Some(SuiteRef(mkSuite))
          case ResourceSharingSuiteFingerprint.matches() =>
            val cst: F[GlobalResourceF.Read[F] => EffectSuite.Provider[F]] =
              // inherently unsafe, as it assumes the user doesn't
              F.delay(loadConstructor[GlobalResourceF.Read[F], SuiteClass](
                taskDef.fullyQualifiedName(),
                classLoader): GlobalResourceF.Read[F] => EffectSuite.Provider[
                F])
            Some(ResourcesSharingSuiteRef(read =>
              F.map(F.ap(cst)(F.pure(read)))(_.getSuite)))
          case GlobalResourcesFingerprint.matches() =>
            val module =
              loadModule(taskDef.fullyQualifiedName(), classLoader)
            val init = cast(module)(GlobalResourcesInitClass)
            Some(GlobalResourcesRef(init))
        }

    }

  /**
   * A fingerprint that searches only for singleton objects of type
   * [[weaver.EffectSuite]].
   */
  object SuiteFingerprint extends WeaverFingerprint {
    val isModule                           = true
    def requireNoArgConstructor(): Boolean = true
    def superclassName(): String           = SuiteClass.runtimeClass.getName
  }

  /**
   * A fingerprint that searches only for classes extending
   * [[weaver.EffectSuite]]. that have a constructor that takes a single
   * [[weaver.GlobalResources.Read]] parameter.
   */
  object ResourceSharingSuiteFingerprint extends WeaverFingerprint {
    val isModule                           = false
    def requireNoArgConstructor(): Boolean = false
    def superclassName(): String           = SuiteClass.runtimeClass.getName
  }

  object GlobalResourcesFingerprint extends WeaverFingerprint {
    val isModule                           = true
    def requireNoArgConstructor(): Boolean = true
    def superclassName(): String = GlobalResourcesInitClass.runtimeClass.getName
  }

  trait WeaverFingerprint extends SubclassFingerprint {
    object matches {
      def unapply(fingerPrint: Fingerprint): Boolean = fingerPrint match {
        case sf: SubclassFingerprint if fingerprintMatches(sf) => true
        case _                                                 => false
      }
    }

    private def fingerprintMatches(sf: SubclassFingerprint): Boolean = {
      sf.isModule() == this.isModule() &&
      sf.requireNoArgConstructor() == this.requireNoArgConstructor() &&
      sf.superclassName() == this.superclassName()
    }
  }

}
