package weaver
package docs

import mdoc._

class FansiModifier extends mdoc.PostModifier {
  override val name: String = "fansi"

  override def process(ctx: PostModifierContext): String = {
    val header = "<div class='terminal'><pre><code class = 'nohighlight'>"
    val footer = "</code></pre></div>"

    val add = ctx.lastValue match {
      case assertion: Expectations =>
        val result = Result.fromAssertion(assertion).formatted.mkString("\n")
        header + Ansi2Html(result) + footer
      case _ => ""

    }

    "\n```scala\n" + ctx.originalCode.text + "\n```\n\n" + add + "\n"

  }
}
