package weaver
package framework
package test

import cats.effect.{ IO, Resource }

object SharedResourceTests extends MutableIOSuite {
  override type Res = (String, Int)
  override def sharedResource: Resource[IO, (String, Int)] =
    Resource.pure[IO, (String, Int)](("foo", 5))

  test("should be able to use pattern matching with match") {
    _ match {
      case (someString, someInt) =>
        IO(expect.all(someString == "foo", someInt == 5))
    }
  }

  test("should be able to use pattern matching with `usingRes` method")
    .usingRes {
      case (someString, someInt) =>
        IO(expect.all(someString == "foo", someInt == 5))
    }
}
