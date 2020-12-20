---
id: installation
title: Installation
---

All of the artifacts from the table below are:

1. Available for **Scala 2.12 and 2.13**
2. Available for **JVM and Scala.js**

```scala mdoc:passthrough
import weaver.docs._

val effects = Table
    .create("Effect types", BuildMatrix.effects)
    .render(BuildMatrix.catsEffect3Version)
    
val integrations = Table
    .create("Integrations", BuildMatrix.integrations)
    .render(BuildMatrix.catsEffect3Version)

println(effects)
println(integrations)
```

Weaver offers effect-type specific test frameworks. The Build setup depends on
the effect-type library you've elected to use (or test against).

Refer yourself to the library specific pages to get the correct configuration.

* [cats](cats_effect_usage.md)
* [monix](monix_usage.md)
* [monix-bio](monix_bio_usage.md)
* [zio](zio_usage.md)
