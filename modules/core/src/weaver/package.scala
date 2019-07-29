package object weaver {

  type IOSuite[A]    = MutableIOSuite[A]
  type SimpleIOSuite = SimpleMutableIOSuite
  type Event         = TestOutcome

  object discard {
    def apply[T]: T => Unit = { value =>
      val _ = value
    }
  }

  def colored(color: String)(s: String): String =
    new StringBuilder()
      .append(color)
      .append(s)
      .append(Console.RESET)
      .toString()

  val red = colored(Console.RED) _

  val yellow = colored(Console.YELLOW) _

  val cyan = colored(Console.CYAN) _

  val green = colored(Console.GREEN) _

}
