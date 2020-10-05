package weaver
package framework
package test

import cats.data.Validated.Invalid
import cats.data.Validated.Valid

object TracingTests extends SimpleIOSuite {

  // Accommodate bloop
  def standardise(path: String): String = {
    val start = ("", 0)
    val (result, _) = path.split("/").foldLeft(start) {
      case ((path, count), "..")             => (path, count + 1)
      case ((path, count), _) if (count > 0) => (path, count - 1)
      case ((path, _), segment)              => (path + "/" + segment, 0)
    }
    result
  }

  val thisFile = "/modules/framework/test/src/TracingTests.scala"

  test("Traces work as expected") {
    val result = isOdd(2)
      .traced(here)
      .traced(here)

    result.run match {
      case Invalid(e) =>
        val locations = e.head.location.toList
        val paths     = locations.map(_.fileRelativePath).map(standardise)
        forall(paths)(p => expect(p == thisFile)) &&
        expect(locations.map(_.line) == List(38, 23, 24, 25))
      case Valid(_) => failure("Should have been invalid")
    }
  }

  def isOdd(i: Int)(implicit loc: SourceLocation): Expectations =
    expect(i % 2 == 1).traced(loc)

}
