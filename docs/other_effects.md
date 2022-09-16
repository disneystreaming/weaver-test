---
id: other_effects

title: Other effects
---

Starting with version 0.8.0, Weaver no longer offers out-of-the-box support for other effect types
than cats-effect.

We (maintainers) are happy to keep the core of weaver effect-agnostic, in an effort to allow for third party
to resurrect support for the effect types they use in repository they control.

You can read about the rationale for decision [here](https://github.com/disneystreaming/weaver-test/discussions/570). Feel free to ping us via a github discussion, if you seek to resurrect support for a given effect-type.

If you are looking for documentation of them maintenance branch of weaver that did support other effect types, you can find it [over there](https://disneystreaming.github.io/weaver-test/docs/0.6.15/installation).

We will try to fix critical bugs on the 0.6/0.7 series, as they get found.

To summarise :

* `0.8.x` and further -> CE3 only, active development,
* `0.7.x` -> CE3 + ZIO 1 support (maintenance mode, fixing critical bugs that not have workarounds)
* `0.6.x` ->  CE2 + Monix/MonixBIO/ZIO 1 support (maintenance mode, fixing critical bugs that do not have workarounds)
