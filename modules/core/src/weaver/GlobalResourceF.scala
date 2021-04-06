package weaver

import scala.reflect.ClassTag
import scala.util.Try

import cats.MonadError
import cats.effect._
import cats.syntax.all._

import org.portablescala.reflect.annotation.EnableReflectiveInstantiation

import CECompat.Ref

/**
 * Top-level instances of this trait are detected by the framework and used to manage
 * the lifecycle of shared resources.
 *
 * The [[weaver.GlobalResourceF.Write]] store is a channel that lets you store
 * resources (http/database clients) using some type-specific tags. We provide [[scala.reflect.ClassTag]]
 * based implementation that works for that aren't subject to type-erasure (ie when a Scala type is
 * equivalent to a JVM class)
 *
 * Stored resources can be retrieved in test suites, by having your suite sport a constructor
 * that takes a [[GlobalResourceF]] instance.
 */
@EnableReflectiveInstantiation
trait GlobalResourceBase

trait GlobalResourceF[F[_]] extends GlobalResourceBase {
  def sharedResources(global: GlobalResourceF.Write[F]): Resource[F, Unit]
}

object GlobalResourceF {

  trait Write[F[_]] {
    protected implicit def F: CECompat.Effect[F]
    protected def rawPut[A](
        pureOrLazy: Either[A, Resource[F, A]],
        label: Option[String])(implicit rt: ResourceTag[A]): F[Unit]

    def put[A](value: A, label: Option[String] = None)(
        implicit rt: ResourceTag[A]): F[Unit] = rawPut(Left(value), label)
    def putR[A](value: A, label: Option[String] = None)(
        implicit rt: ResourceTag[A]): Resource[F, Unit] =
      CECompat.resourceLift(put(value, label))

    /**
     * Memoises a resource so to optimise its sharing. The memoised resource gets allocated
     * lazily, when the first suite that needs it starts running, and gets finalised as soon
     * as all suites that need it concurrently are done.
     *
     * In case the resource was already finalised when a suite needs, it gets re-allocated
     * on demand.
     *
     * This can be useful for constructs that consume large amount of machine resources
     * (CPU, memory, connections), to ensure they are cleaned-up when they should.
     */
    def putLazy[A](
        resource: Resource[F, A],
        label: Option[String] = None)(implicit rt: ResourceTag[A]): F[Unit] =
      MemoisedResource(resource).flatMap(r => rawPut(Right(r), label))

    def putLazyR[A](
        resource: Resource[F, A],
        label: Option[String] = None)(implicit
    rt: ResourceTag[A]): Resource[F, Unit] =
      CECompat.resourceLift(putLazy(resource, label))
  }

  trait Read[F[_]] {
    protected implicit def F: MonadError[F, Throwable]

    protected def rawGet[A](label: Option[String] = None)(
        implicit rt: ResourceTag[A]): F[Option[Either[A, Resource[F, A]]]]

    def get[A](label: Option[String] = None)(
        implicit rt: ResourceTag[A]): F[Option[A]] = rawGet[A](label).map {
      case Some(Left(value)) => Some(value)
      case _                 => None
    }

    def getR[A](label: Option[String] = None)(
        implicit rt: ResourceTag[A]): Resource[F, Option[A]] =
      CECompat.resourceLift {
        rawGet[A](label)
      }.flatMap {
        case Some(Left(value))     => Resource.pure(Some(value))
        case Some(Right(resource)) => resource.map(Some(_))
        case None                  => Resource.pure(None)
      }

    def getOrFail[A](label: Option[String] = None)(
        implicit rt: ResourceTag[A]
    ): F[A] =
      get[A](label).flatMap[A] {
        case Some(value) => F.pure(value)
        case None =>
          F.raiseError(GlobalResourceF.ResourceNotFound(label, rt.description))
      }

    def getOrFailR[A](label: Option[String] = None)(
        implicit rt: ResourceTag[A]): Resource[F, A] =
      getR[A](label).flatMap {
        case Some(value) => Resource.pure[F, A](value)
        case None =>
          CECompat.resourceLift(F.raiseError(GlobalResourceF.ResourceNotFound(
            label,
            rt.description)))
      }
  }

  object Read {
    def empty[F[_]](effect: CECompat.Effect[F]): Read[F] = new Read[F] {
      implicit protected def F: MonadError[F, Throwable] = effect

      protected def rawGet[A](label: Option[String])(implicit
      rt: ResourceTag[A]): F[Option[Either[A, Resource[F, A]]]] =
        effect.pure(None)
    }
  }

  def createMap[F[_]: CECompat.Effect]: F[Read[F] with Write[F]] =
    Ref[F]
      .of(Map.empty[(Option[String], ResourceTag[_]),
                    Either[Any, Resource[F, Any]]])
      .map(new ResourceMap(_))

  private class ResourceMap[F[_]](
      ref: Ref[
        F,
        Map[(Option[String], ResourceTag[_]), Either[Any, Resource[F, Any]]]])(
      implicit val F: CECompat.Effect[F])
      extends Read[F]
      with Write[F] { self =>

    def rawPut[A](pureOrLazy: Either[A, Resource[F, A]], label: Option[String])(
        implicit rt: ResourceTag[A]): F[Unit] = {
      ref.update(_ + ((label, rt) -> pureOrLazy))
    }

    def rawGet[A](label: Option[String])(
        implicit rt: ResourceTag[A]): F[Option[Either[A, Resource[F, A]]]] =
      ref.get.map(_.get(label -> rt)).map {
        case None              => None
        case Some(Left(value)) => rt.cast(value).map(Left(_))
        case Some(Right(resource)) =>
          Some(Right(resource.map(rt.cast).map {
            case None =>
              F.raiseError(ResourceNotFound(label, rt.description))
            case Some(value) => F.pure(value)
          }.flatMap(CECompat.resourceLift(_))))
      }

  }

  case class ResourceNotFound(label: Option[String], typeDesc: String)
      extends Throwable {
    override def getMessage(): String =
      s"Could not find a resource of type $typeDesc with label ${label.orNull}"
  }

}

/**
 * Rough type-tag, for which we provide a low effort instance based on classtags for classes that
 * are not subject to type-erasure.
 *
 * Because this type is used as an index in a map, you ought to make sure it implements
 * proper equals/hashCode methods
 */
trait ResourceTag[A] extends AnyRef {
  def description: String
  def cast(obj: Any): Option[A]
}

object ResourceTag extends LowPriorityImplicits

private[weaver] case class ClassBasedResourceTag[A](ct: ClassTag[A])
    extends ResourceTag[A] {

  def description: String       = ct.toString()
  def cast(obj: Any): Option[A] = Try(obj.asInstanceOf[A]).toOption
}

trait LowPriorityImplicits {
  implicit def classBasedInstance[A](implicit ct: ClassTag[A]): ResourceTag[A] =
    ClassBasedResourceTag(ct)
  @scala.annotation.implicitAmbiguous(
    "\n\nCould not find an implicit ResourceTag instance for type ${F}[${A}]\n" +
      "This is likely because ${F} is subject to type erasure. You can implement a ResourceTag manually " +
      "or wrap the item you are trying to store/access, in some monomorphic case class that is not subject " +
      "to type erasure\n\n")
  implicit def notProvided1[F[_], A](
      implicit ct: ClassTag[F[A]]): ResourceTag[F[A]] = ???
  implicit def notProvided2[F[_], A](
      implicit ct: ClassTag[F[A]]): ResourceTag[F[A]] = ???

  @scala.annotation.implicitAmbiguous(
    "\n\nCould not find an implicit ResourceTag instance for type ${HKF}[${F}]\n" +
      "This is likely because ${HKF} is subject to type erasure. You can implement a ResourceTag manually " +
      "or wrap the item you are trying to store/access, in some monomorphic case class that is not subject " +
      "to type erasure\n\n")
  implicit def notProvided3[HKF[_[_]], F[_]](
      implicit ct: ClassTag[HKF[F]]): ResourceTag[HKF[F]] = ???
  implicit def notProvided4[HKF[_[_]], F[_]](
      implicit ct: ClassTag[HKF[F]]): ResourceTag[HKF[F]] = ???
}
