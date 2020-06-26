package weaver
package framework
package test

import cats.syntax.option._

abstract class FSCompatTest extends SimpleIOSuite {

  val pwd               = FSCompat.wd
  val fileName          = "DogFood.scala"
  val relativePath      = s"foo/bar/${fileName}"
  val absolutePath      = s"${pwd}/${relativePath}"
  val wrongAbsolutePath = s"/invalid/subpath/${fileName}"

  test("best effort path of a relative path is that path") {
    val result = FSCompat.bestEffortPath(fileName.some, relativePath.some)
    expect(result == relativePath.some)
  }

  test(
    "best effort path of an absolute subpath of current path is the subpath") {
    val result = FSCompat.bestEffortPath(fileName.some, absolutePath.some)
    expect(result == relativePath.some)
  }

  test(
    "best effort path of an absolute non-subpath of current path is the file name") {
    val result = FSCompat.bestEffortPath(fileName.some, wrongAbsolutePath.some)
    expect(result == fileName.some)
  }
}

object FSCompatTest extends FSCompatTest
