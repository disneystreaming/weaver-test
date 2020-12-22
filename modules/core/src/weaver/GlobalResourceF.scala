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
 * The [[weaver.GlobalResources.Write]] store is a channel that lets you store
 * resources (http/database clients) using some type-specific tags. We provide [[scala.reflect.ClassTag]]
 * based implementation that works for that aren't subject to type-erasure (ie when a Scala type is
 * equivalent to a JVM class)
 *
 * Stored resources can be retrieved in test suites, by having your suite sport a constructor
 * that takes a [[GlobalResource]] instance.
 */
@EnableReflectiveInstantiation
trait GlobalResourceBase

trait GlobalResourceF[F[_]] extends GlobalResourceBase {
  def sharedResources(global: GlobalResourceF.Write[F]): Resource[F, Unit]
}

object GlobalResourceF {

  trait Write[F[_]] {
    protected implicit def F: Sync[F]
    def put[A](value: A, label: Option[String] = None)(
        implicit rt: ResourceTag[A]): F[Unit]
    def putR[A](value: A, label: Option[String] = None)(
        implicit rt: ResourceTag[A]): Resource[F, Unit] =
      CECompat.resourceLift(put(value, label))
  }

  trait Read[F[_]] {
    protected implicit def F: MonadError[F, Throwable]
    def get[A](label: Option[String] = None)(
        implicit rt: ResourceTag[A]): F[Option[A]]

    def getR[A](label: Option[String] = None)(
        implicit rt: ResourceTag[A]): Resource[F, Option[A]] =
      CECompat.resourceLift(get[A](label))

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
      CECompat.resourceLift(getOrFail[A](label))

  }

  private[weaver] def createMap[F[_]: Sync]: F[Read[F] with Write[F]] =
    Ref[F]
      .of(Map.empty[(Option[String], ResourceTag[_]), Any])
      .map(new ResourceMap(_))

  private class ResourceMap[F[_]](
      ref: Ref[F, Map[(Option[String], ResourceTag[_]), Any]])(
      implicit val F: Sync[F])
      extends Read[F]
      with Write[F] { self =>

    def put[A](value: A, label: Option[String])(
        implicit rt: ResourceTag[A]): F[Unit] = {
      ref.update(_ + ((label, rt) -> value))
    }

    def get[A](label: Option[String])(
        implicit rt: ResourceTag[A]): F[Option[A]] =
      ref.get.map(_.get(label -> rt).flatMap(rt.cast))

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
