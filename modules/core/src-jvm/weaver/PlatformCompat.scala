package weaver

private[weaver] object PlatformCompat {
  val platform: Platform = Platform.JVM

  def getClassLoader(clazz: java.lang.Class[_]): ClassLoader =
    clazz.getClassLoader()
}
