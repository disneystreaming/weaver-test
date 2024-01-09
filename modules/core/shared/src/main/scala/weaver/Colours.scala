package weaver

private[weaver] object Colours {

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

  val whitebold = colored(Console.WHITE + Console.BOLD) _

  val ansiColorRgx = "\u001b\\[([;\\d]*)m".r

  def removeASCIIColors(str: String) =
    ansiColorRgx.replaceAllIn(str, "")
}
