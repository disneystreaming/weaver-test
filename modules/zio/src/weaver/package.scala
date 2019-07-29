package weaver

package object zio {

  type ZIOSuite[A]    = MutableZIOSuite[A]
  type SimpleZIOSuite = SimpleMutableZIOSuite

}
