package weaver
package docs

import mdoc._

class FansiModifier extends mdoc.PostModifier {
  override val name: String = "fansi"

  override def process(ctx: PostModifierContext): String = {
    val header = "<div class='terminal'><pre><code class = 'nohighlight'>"
    val footer = "</code></pre></div>"

    val (add, code) = ctx.lastValue match {
      case assertion: Expectations =>
        val result =
          Result
            .fromAssertion(assertion)
            .formatted
            .mkString("\n")

        val add = header + Ansi2Html(result) + footer
        val raw =
          ctx.originalCode.text.trim().linesIterator.toVector.map(_.trim())

        if (raw.headOption.contains("{") && raw.lastOption.contains("}"))
          (add, raw.init.drop(1).mkString("\n"))
        else
          (add, raw.mkString("\n"))

      case _ => ("", ctx.originalCode.text)

    }

    "\n```scala\n" + code + "\n```\n\n" + add + "\n"

  }
}
