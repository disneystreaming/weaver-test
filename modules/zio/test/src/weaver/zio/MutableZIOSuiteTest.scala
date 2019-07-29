package weaver.zio

object MutableZIOSuiteTest extends SimpleMutableZIOSuite {

  pureTest("hello pure") {
    expect(true)
  }

  test("hello test") {
    for {
      _ <- log.info("hello")
    } yield expect(List(1, 2, 3).size == 3)
  }

}
