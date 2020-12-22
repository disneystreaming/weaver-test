package weaver

private[weaver] object PlatformCompat  {
  val platform: Platform = Platform.JS

  def getClassLoader(clazz: java.lang.Class[_]): ClassLoader =
    new ClassLoader() {}
}
