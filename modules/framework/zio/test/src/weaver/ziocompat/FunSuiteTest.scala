package weaver.ziocompat

object FunSuiteTest extends FunSuite {

  test("and") {
    expect(1 == 1) and expect(2 == 2) && not(expect(1 == 2))
  }

  test("forall (success)") {
    forEach(List(true, true))(value => expect(value == true))
  }

  test("forall (failure)") {
    not(forEach(List(true, false))(value => expect(value == true)))
  }
}
