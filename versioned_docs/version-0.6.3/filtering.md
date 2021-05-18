---
id: version-0.6.3-filtering
title: Filtering tests
original_id: filtering
---

When using the IOSuite variants, the user can call `sbt`'s test command as such:

``` 
> testOnly -- -o *foo*
```

This filter will prevent the execution of any test that doesn't contain the string "foo" in is qualified name. For a test labeled "foo" in a "FooSuite" object, in the package "fooPackage", the qualified name of a test is:

```
fooPackage.FooSuite.foo
```