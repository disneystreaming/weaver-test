package weaver
package framework
package test

object TestConsole {
  private val ansiColorRgx = """\u001b\[([;\d]*)m""".r

  val removeASCIIColors: String => String = { s =>
    ansiColorRgx.replaceAllIn(s, "")
  }
}
