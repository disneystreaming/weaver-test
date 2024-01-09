package weaver

private[weaver] object PlatformCompat {
  val platform: Platform = Platform.Native

  def getClassLoader(clazz: java.lang.Class[_]): ClassLoader =
    new ClassLoader() {}
}
