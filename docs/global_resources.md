---
id: global_resources
title: Sharing resources across suites
---

## A word of warning

This feature works **only on JVM**, and has been tested in SBT, Mill and Bloop.

When using weaver manually, outside of the build tool, with a standalone runner (we do provide one), please disregard this mechanism and use classic dependency injection and/or your own wits to share resources across suites.

## Declaring global resources

In order to declare global resources, which suites will be able to share, weaver provides a `GlobalResource` interface that users can implement. This interface sports a method that takes a `GlobalWrite` instance, which contains semantics to store items, indexing them by types.

NB : the implementations have to be static objects.

```scala mdoc
import weaver._

// The same API / developer experience is offered with any of the following imports :
// import weaver.monixcompat._
// import weaver.monixbiocompat._
// import weaver.ziocompat._

import cats.effect.IO
import cats.effect.Resource

// note how this is a static object
object SharedResources extends GlobalResource {
  def sharedResources(global: GlobalWrite): Resource[IO, Unit] =
    for {
      foo <- Resource.pure[IO, String]("hello world!")
      _   <- global.putR(foo)
    } yield ()
}
```

## Accessing global resources

On the suite side, accessing the global resources happen via declaring a constructor on your suite that takes a single parameter of type `GlobalRead`.

This item can be used to access the resources that were previously initialised and stored in the `GlobalResource`.

```scala mdoc
class SharingSuite(global: GlobalRead) extends IOSuite {
  type Res = String
  def sharedResource: Resource[IO, String] =
    global.getOrFailR[String]()

  test("a stranger, from the outside ! ooooh") { sharedString =>
    IO(expect(sharedString == "hello world!"))
  }
}

class OtherSharingSuite(global: GlobalRead)
    extends IOSuite {
  type Res = Option[Int]

  // We didn't store an `Int` value in our `GlobalResourcesInit` impl
  def sharedResource: Resource[IO, Option[Int]] =
    global.getR[Int]()

  test("oops, forgot something here") { sharedInt =>
    IO(expect(sharedInt.isEmpty))
  }
}
```

## Lifecycle

Weaver guarantees the following order :

* All `GlobalResource` instances in current compile-unit are run. Because the interface they have access to is "write-only", the order in which these instances are used should not matter.
* All suites are run in arbitrary order. Whether they run in parallel or not depends on the build-tool settings, not weaver.
* Weaver then calls the finalisers of the resources created from the various `GlobalResource` instances.

This implies that **all resources declared in GlobalResource will remain alive/active until all tests have run**.

## Regarding "testOnly" build tool commands

Some build tools provide a "testOnly" (or equivalent) command that lets you test a single suite. Because of how weaver taps into the same detection mechanism build tools use to communicate suites to the framework, you should either :

* pass your `GlobalResource` implementations to the `testOnly` command, alongside the suite you really want to run
* ensure that suites can recover when the resource they need is not found in `GlobalResource`

An example of how to do this:

```scala mdoc
import cats.effect.{ IO, Resource }
import weaver._

object MyResources extends GlobalResource {
  override def sharedResources(global: GlobalWrite): Resource[IO, Unit] =
    baseResources.flatMap(global.putR(_))

  def baseResources: Resource[IO, String] = Resource.pure[IO, String]("hello world!")

  // Provides a fallback to support running individual tests via testOnly
  def sharedResourceOrFallback(read: GlobalRead): Resource[IO, String] =
    read.getR[String]().flatMap {
      case Some(value) => Resource.liftF(IO(value))
      case None        => baseResources
    }
}

class MySuite(global: GlobalRead) extends IOSuite {
  import MyResources._

  override type Res = String

  def sharedResource: Resource[IO, String] = sharedResourceOrFallback(global)

  test("a stranger, from the outside ! ooooh") { sharedString =>
    IO(expect(sharedString == "hello world!"))
  }
}
```

## Regarding global resource indexing

### Runtime constraints

The shared items are indexed via `weaver.ResourceTag`, a custom typeclass that has a default instance for based on `scala.reflect.ClassTag`. This implies that the default instance only works for types that are not subject to type-erasure.

If the user wants to share resources which are subject to type-erasure (ie that have type parameters, such as `org.http4s.client.Client`), they have to provide an instance themselves, or alternatively use a monomorphic wrapper (which is not subject to type-erasure).

```scala mdoc:fail
import weaver._

import cats.effect.IO
import cats.effect.Resource

// Class subject to type-erasure
case class Foo[A](value : A)

object FailingSharedResources extends GlobalResource {
  def sharedResources(global: GlobalWrite): Resource[IO, Unit] =
    global.putR(Foo("hello"))
}
```

### Labelling

On the two sides of (production and consumption) of the global resources, it is possible to label the resources with string values, to discriminate between several resources of the same.

```scala mdoc
import cats.syntax.all._

object LabelSharedResources extends GlobalResource {
  def sharedResources(global: GlobalWrite): Resource[IO, Unit] =
    for {
      _ <- global.putR(1, "one".some)
      _ <- global.putR(2, "two".some)
    } yield ()
}

class LabelSharingSuite(global: GlobalRead)
    extends IOSuite {

  type Res = Int

  // We didn't store an `Int` value in our `GlobalResourcesInit` impl
  def sharedResource: Resource[IO, Int] = for {
    one <- global.getOrFailR[Int]("one".some)
    two <- global.getOrFailR[Int]("two".some)
  } yield one + two

  test("labels work") { sharedInt =>
    IO(expect(sharedInt == 3))
  }
}
```
