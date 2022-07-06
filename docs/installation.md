---
id: installation
title: Installation
---

All of the artifacts below are available for both **JVM and Scala.js**.


```scala mdoc:passthrough
import weaver.docs._

import BuildMatrix._

val effectsTable = Table
    .create("Effect types", effects)
    .render(artifactsCE2Version)

val integrationsTable = Table
    .create("Integrations", integrations)
    .render(artifactsCE2Version)

println(effectsTable)
println(integrationsTable)
```

Weaver offers effect-type specific test frameworks. The Build setup depends on
the effect-type library you've elected to use (or test against).

Refer yourself to the library specific pages to get the correct configuration.

- [cats](cats_effect_usage.md)
- [monix](monix_usage.md)
- [monix-bio](monix_bio_usage.md)
- [zio](zio_usage.md)
