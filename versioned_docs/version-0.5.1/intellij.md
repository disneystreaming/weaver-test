---
id: version-0.5.1-intellij
title: intellij plugin
original_id: intellij
---

Starting with version 0.5.0, weaver provides bespoke intellij support via  a plugin that allows to run suites and tests directly from the IDE

## Installation

In Intellij, go to `preferences > plugins`, click on the cog icon and select `manage plugin repositories`

![](../img/intellij_repo.png)

Add `https://disneystreaming.github.io/weaver-test/intellij.xml` to the list.

You can now search for the `weaver-intellij` plugin in the marketplace view

![](../img/intellij_install.png)


## Usage

The plugin requires your project to be using version 0.5.0-RC1 of weaver (or newer). However, despite this requirement, the version of the plugin and the version of the test framework **DO NOT NEED TO MATCH**.

### Running suites

When test suites are open in Intellij, the plugin adds buttons to the left of the editor (next to the line number of the suite declaration) , letting you run individual suites from the IDE.

![](../img/intellij_usage.png)

### Running tests

The plugin also **attempts** to add buttons next on the same lines of tests, in a best-effort fashion, by detecting implicit conversions from `String` to `weaver.TestName`. When running tests this way, only tests matching the selected line will run.

![](../img/intellij_output.png)
