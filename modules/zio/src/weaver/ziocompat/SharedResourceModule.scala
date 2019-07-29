package weaver.ziocompat

trait SharedResourceModule[A] { self =>
  def sharedResource: A
}
