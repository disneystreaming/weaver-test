---
id: installation
title: Installation
---

The artifacts below are available for **Scala JVM, Scala.js, Scala-native**.

```scala mdoc:passthrough
import weaver.docs._

import BuildMatrix._

val effectsTable = Table
    .create("Effect types", effects)
    .render(artifactsCE3Version)

val integrationsTable = Table
    .create("Integrations", integrations)
    .render(artifactsCE3Version)

println(effectsTable)
println(integrationsTable)
```

Weaver offers effect-type specific test frameworks. The Build setup depends on
the effect-type library you've elected to use (or test against).

Refer yourself to the library specific pages to get the correct configuration.

- [cats](cats_effect_usage.md)
