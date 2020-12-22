---
id: version-0.6.0-M3-installation
title: Installation
original_id: installation
---

All of the artifacts from the table below are:

1. Available for **Scala 2.12 and 2.13**
2. Available for **JVM and Scala.js**

|Effect types|Cats Effect 2|Cats Effect 3.0.0-M5|
|---|---|---|
|Cats-Effect|✅ `0.6.0-M3`|✅ `0.7.0-M3`|
|Monix|✅ `0.6.0-M3`|❌|
|Monix BIO|✅ `0.6.0-M3`|❌|
|ZIO|✅ `0.6.0-M3`|❌|

|Integrations|Cats Effect 2|Cats Effect 3.0.0-M5|
|---|---|---|
|ScalaCheck|✅ `0.6.0-M3`|✅ `0.7.0-M3`|
|Specs2 matchers|✅ `0.6.0-M3`|✅ `0.7.0-M3`|


Weaver offers effect-type specific test frameworks. The Build setup depends on
the effect-type library you've elected to use (or test against).

Refer yourself to the library specific pages to get the correct configuration.

* [cats](cats_effect_usage.md)
* [monix](monix_usage.md)
* [monix-bio](monix_bio_usage.md)
* [zio](zio_usage.md)
