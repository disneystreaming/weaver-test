package weaver
package framework
package test

object SourceLocationTest extends SimpleIOSuite {

  // DO NOT MOVE THIS
  val sourceLocation = implicitly[SourceLocation]

  test("implicit capture of source location is relativised") {
    val name    = sourceLocation.fileName
    val relPath = sourceLocation.fileRelativePath
    val line    = sourceLocation.line

    expect(name.contains("SourceLocationTest.scala")) &&
    expect(relPath.contains(
      "modules/framework/test/src/SourceLocationTest.scala")) &&
    expect(line == 8)
  }

}
