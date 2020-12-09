---
id: version-0.5.0-global_resources
title: Sharing resources across suites
original_id: global_resources
---

## A word of warning

This feature works **only on JVM** and violates the semantics of the [test-framework protocol](https://github.com/sbt/test-interface) that most Scala build tools (mill, sbt, bloop, etc) use. That protocol implies that all suites are supposed to run in isolation.

Whilst the semantics cover most cases, it is sometimes really useful to share resources across suites, for efficiency reasons (especially if suites are run in parallel, which is decided by the build tool)

Weaver-test provides this feature using what is very much a hack. At the time of writing this, the feature works with sbt, mill, bloop, but we cannot guarantee for it to work with all build tools.

**Use at your own risk**

When using weaver manually, outside of the build tool, with a standalone runner (we do provide one), please disregard this mechanism and use classic dependency injection and/or your own wits to share resources across suites.

## Declaring global resources

In order to declare global resources, which suites will be able to share, weaver provides a `weaver.GlobalResourcesInit` interface that users can implement. This interface sports a method that takes a `weaver.GlobalResources.Write` instance, which contains semantics to store items indexed by types.

NB : the implementations have to be static objects.

```scala
import weaver._

import cats.effect.IO
import cats.effect.Resource

// note how this is a static object
object SharedResources extends GlobalResourcesInit {
  def sharedResources(store: GlobalResources.Write[IO]): Resource[IO, Unit] =
    for {
      foo <- Resource.pure[IO, String]("hello world!")
      _   <- store.putR(foo)
    } yield ()
}
```

## Accessing global resources

On the suite side, accessing the global resources happen via declaring a constructor on your suite that takes a single parameter of type `weaver.GlobalResources`.

This item can be used to access the resources that were previously initialised and stored in the `GlobalResourcesInit`.

```scala
class SharingSuite(globalResources: GlobalResources) extends IOSuite {
  type Res = String
  def sharedResource: Resource[IO, String] =
    globalResources.in[IO].getOrFailR[String]()

  test("a stranger, from the outside ! ooooh") { sharedString =>
    IO(expect(sharedString == "hello world!"))
  }
}

class OtherSharingSuite(globalResources: GlobalResources)
    extends IOSuite {
  type Res = Option[Int]

  // We didn't store an `Int` value in our `GlobalResourcesInit` impl
  def sharedResource: Resource[IO, Option[Int]] =
    globalResources.in[IO].getR[Int]()

  test("oops, forgot something here") { sharedInt =>
    IO(expect(sharedInt.isEmpty))
  }
}
```

## Lifecycle

Weaver guarantees the following order :

* All `weaver.GlobalResourcesInit` in current compile-unit are run. Because the interface they have access to is "write-only", the order in which these instances are used should not matter.
* All suites are run in arbitrary order. Whether they run in parallel or not depends on the build-tool settings, not weaver.
* Weaver then calls the finalisers of the resources created from the various `GlobalResourceInit` instances.

This implies that **all resources declared in GlobalResourcesInit will remain alive/active until all tests have run**.

## Regarding "testOnly" build tool commands

Some build tools provide a "testOnly" (or equivalent) command that lets you test a single suite. Because of how weaver taps into the same detection mechanism build tools use to communicate suites to the framework, you should either :

* pass your `weaver.GlobalResourcesInit` implementations to the `testOnly` command, alongside the suite you really want to run
* ensure that suites can recover when the resource they need is not found in `weaver.GlobalResource`

An example of how to do this:

```scala
import cats.effect.{ IO, Resource }
import weaver.{ GlobalResources, GlobalResourcesInit, IOSuite }

object MyResources extends GlobalResourcesInit {
  override def sharedResources(store: GlobalResources.Write[IO]): Resource[IO, Unit] =
    baseResources.flatMap(store.putR(_))

  def baseResources: Resource[IO, String] = Resource.pure[IO, String]("hello world!")

  // Provides a fallback to support running individual tests via testOnly
  def sharedResourceOrFallback(globalResources: GlobalResources): Resource[IO, String] =
    globalResources.in[IO].getR[String]().flatMap {
      case Some(value) => Resource.liftF(IO(value))
      case None        => baseResources
    }
}

class MySuite(globalResources: GlobalResources) extends IOSuite {
  import MyResources._

  override type Res = String

  def sharedResource: Resource[IO, String] = sharedResourceOrFallback(globalResources)

  test("a stranger, from the outside ! ooooh") { sharedString =>
    IO(expect(sharedString == "hello world!"))
  }
}
```

## Regarding global resource indexing

### Runtime constraints

The shared items are indexed via `weaver.ResourceTag`, a custom typeclass that has a default instance for based on `scala.reflect.ClassTag`. This implies that the default instance only works for types that are not subject to type-erasure.

If the user wants to share resources which are subject to type-erasure (ie that have type parameters, such as `org.http4s.client.Client`), they have to provide an instance themselves, or alternatively use a monomorphic wrapper (which is not subject to type-erasure).

```scala
import weaver._

import cats.effect.IO
import cats.effect.Resource

// Class subject to type-erasure
case class Foo[A](value : A)

object FailingSharedResources extends GlobalResourcesInit {
  def sharedResources(store: GlobalResources.Write[IO]): Resource[IO, Unit] =
    store.putR(Foo("hello"))
}
// error:
// 
// Could not find an implicit ResourceTag instance for type repl.MdocSession.App.Foo[String]
// This is likely because repl.MdocSession.App.Foo is subject to type erasure. You can implement a ResourceTag manually or wrap the item you are trying to store/access, in some monomorphic case class that is not subject to type erasure
// 
// 
// Error occurred in an application involving default arguments.
//     store.putR(Foo("hello"))
//     ^^^^^^^^^^^^^^^^^^^^^^^^
```

### Labelling

On the two sides of (production and consumption) of the global resources, it is possible to label the resources with string values, to discriminate between several resources of the same.

```scala
import cats.syntax.all._

object LabelSharedResources extends GlobalResourcesInit {
  def sharedResources(store: GlobalResources.Write[IO]): Resource[IO, Unit] =
    for {
      _ <- store.putR(1, "one".some)
      _ <- store.putR(2, "two".some)
    } yield ()
}

class LabelSharingSuite(globalResources: GlobalResources)
    extends IOSuite {

  type Res = Int

  // We didn't store an `Int` value in our `GlobalResourcesInit` impl
  def sharedResource: Resource[IO, Int] = for {
    one <- globalResources.in[IO].getOrFailR[Int]("one".some)
    two <- globalResources.in[IO].getOrFailR[Int]("two".some)
  } yield one + two

  test("labels work") { sharedInt =>
    IO(expect(sharedInt == 3))
  }
}
```
