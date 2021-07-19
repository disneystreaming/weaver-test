package weaver
package scalacheck

import scala.concurrent.duration._

import cats.effect.IO
import cats.syntax.all._

import org.scalacheck.Gen

object CheckersTest extends SimpleIOSuite with Checkers {

  override def checkConfig: CheckConfig =
    super.checkConfig.copy(perPropertyParallelism = 100)

  test("universal") {
    forall(Gen.posNum[Int]) { a =>
      expect(a > 0)
    }
  }

  test("form 1") {
    forall { (a: Int) =>
      expect(a * 2 == 2 * a)
    }
  }

  test("form 2") {
    forall { (a1: Int, a2: Int) =>
      expect(a1 * a2 == a2 * a1)
    }
  }

  test("form 3") {
    forall { (a1: Int, a2: Int, a3: Int) =>
      expect(a1 * a2 * a3 == a3 * a2 * a1)
    }
  }

  test("form 4") {
    forall { (a1: Int, a2: Int, a3: Int, a4: Int) =>
      expect(a1 * a2 * a3 * a4 == a4 * a3 * a2 * a1)
    }
  }

  test("form 5") {
    forall { (a1: Int, a2: Int, a3: Int, a4: Int, a5: Int) =>
      expect(a1 * a2 * a3 * a4 * a5 == a5 * a4 * a3 * a2 * a1)
    }
  }

  test("form 6") {
    forall { (a1: Int, a2: Int, a3: Int, a4: Int, a5: Int, a6: Int) =>
      expect(a1 * a2 * a3 * a4 * a5 * a6 == a6 * a5 * a4 * a3 * a2 * a1)
    }
  }

  test("IO form (1)") {
    forall { (a1: Int) =>
      IO.sleep(100.millis).map(_ => expect(a1 * 2 == a1 + a1))
    }
  }

  test("IO form (2)") {
    forall { (a1: Int, a2: Int) =>
      IO.sleep(1.second).map(_ => expect(a1 + a2 == a2 + a1))
    }
  }


}
