package weaver
package scalacheck

import scala.util.control.NoStackTrace

import cats.effect.IO

import org.scalacheck.effect.PropF

object ScalacheckFTest extends SimpleIOSuite with ScalacheckIO {
  propTest("successes") {
    PropF.forAllF { (x: Int) =>
      IO(x).start.flatMap(_.join).map(_ => PropF.passed[IO])
    }
  }

  propTest("falsified") {
    PropF.forAllF { (x: Int) =>
      IO(x).start.flatMap(_.join).map(_ => PropF.falsified[IO])
    }
  }

  propTest("exception") {
    PropF.forAllF { (x: Int) =>
      IO(x).start.flatMap(_.join).map(_ =>
        PropF.exception[IO](new RuntimeException("whaaat") with NoStackTrace))
    }
  }

  propTest("If it's above 250, it should be even") {
    PropF.forAllF { (x: Int) =>
      if (x > 250) expect(x % 2 == 0)
      else success
    }
  }

  propTest("Basic arithmetics") {
    PropF.forAllF { (x: Int) =>
      expect(x - x == 0)
    }
  }

  propTest("Basic arithmetics (but with Fibers)") {
    PropF.forAllF { (x: Int) =>
      IO(x).start.flatMap(_.join).as(expect(x - x == 0))
    }
  }

}
