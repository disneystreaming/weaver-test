package weaver.zio

trait SharedResourceModule[A] { self =>
  def sharedResource: A

  trait lift extends SharedResourceModule[A] {
    def sharedResource: A = self.sharedResource
  }
}

case class SharedResource[A](get: A)  {
  trait asModule extends SharedResourceModule[A] {
    def sharedResource: A = get
  }
}
