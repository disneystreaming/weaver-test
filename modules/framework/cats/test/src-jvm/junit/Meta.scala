package weaver
package junit

import cats.effect.{ IO, Resource }

object Meta {

  object MySuite extends SimpleIOSuite {

    override def maxParallelism: Int = 1

    pureTest("success") {
      success
    }

    pureTest("failure") {
      failure("oops")
    }

    test("ignore") {
      ignore("just because")
    }

  }

  object Only extends SimpleIOSuite {

    override def maxParallelism: Int = 1
    override def isCI: Boolean       = false

    pureTest("only".only) {
      success
    }

    pureTest("not only") {
      failure("foo")
    }

  }

  object OnlyFailsOnCi extends SimpleIOSuite {

    override def maxParallelism: Int = 1
    override def isCI: Boolean       = true

    pureTest("first only test".only) {
      success
    }

    pureTest("second only test".only) {
      success
    }

    pureTest("normal test") {
      success
    }

    pureTest("not only") {
      failure("foo")
    }

  }

  object Ignore extends SimpleIOSuite {

    override def maxParallelism: Int = 1

    pureTest("not ignored 1") {
      success
    }

    pureTest("not ignored 2") {
      success
    }

    pureTest("is ignored".ignore) {
      failure("foo")
    }

  }

  object IgnoreAndOnly extends SimpleIOSuite {

    override def maxParallelism: Int = 1
    override def isCI: Boolean       = false

    pureTest("only".only) {
      success
    }

    pureTest("not tagged") {
      failure("foo")
    }

    pureTest("only and ignored".only.ignore) {
      failure("foo")
    }

    pureTest("is ignored".ignore) {
      failure("foo")
    }

  }

  object OnlyFailsOnCiEvenIfIgnored extends SimpleIOSuite {

    override def maxParallelism: Int = 1
    override def isCI: Boolean       = true

    pureTest("only and ignored".only.ignore) {
      failure("foo")
    }

  }

  object IgnorePure extends FunSuite {

    test("not ignored 1") {
      success
    }

    test("not ignored 2") {
      success
    }

    test("is ignored".ignore) {
      failure("foo")
    }

  }

  class Sharing(global: GlobalRead) extends IOSuite {

    type Res = Unit
    // Just checking the suite does not crash
    def sharedResource: Resource[IO, Unit] = global.getR[Int]().map(_ => ())

    pureTest("foo") {
      success
    }

  }

}
