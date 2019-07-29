package weaver.zio

object MutableZIOSuiteTest extends SimpleZIOSuite {

  test("hello test") {
    for {
      _ <- log.info("hello")
    } yield expect(List(1, 2, 3).size == 3)
  }


  pureTest("hello pure") {
    expect(true)
  }


}
