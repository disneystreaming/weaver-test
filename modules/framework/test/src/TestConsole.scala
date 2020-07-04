package weaver
package framework
package test

object TestConsole {
  private val ansiColorRgx = "\u001b\\[([;\\d]*)m".r

  def removeASCIIColors(str: String) =
    ansiColorRgx.replaceAllIn(str, "")
}
