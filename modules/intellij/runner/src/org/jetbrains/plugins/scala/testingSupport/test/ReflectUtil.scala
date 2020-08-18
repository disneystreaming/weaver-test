package org.jetbrains.plugins.scala.testingSupport.test

import java.lang.reflect._

import weaver.EffectSuite

object ReflectUtil {

  type F[_] = ({ type G[_] })#G[_]

  def loadModule(fqcn: String, loader: ClassLoader): Option[EffectSuite[F]] = {
    load(fqcn, loader).filter(isModuleClass).collect(load)
  }

  private def isModuleClass(clazz: Class[_]): Boolean = {
    try {
      val fld = clazz.getField("MODULE$")
      clazz.getName.endsWith("$") && (fld.getModifiers & Modifier.STATIC) != 0
    } catch {
      case _: NoSuchFieldException => false
    }
  }

  private def load(fqcn: String, loader: ClassLoader): Option[Class[_]] = {
    try {
      Some(Class.forName(fqcn, false, loader))
    } catch {
      case _: ClassNotFoundException => None
    }
  }

  private def load: PartialFunction[Class[_], EffectSuite[F]] = {
    case c if classOf[EffectSuite[F]].isAssignableFrom(c) =>
      c.getField("MODULE$").get(null).asInstanceOf[EffectSuite[F]]
  }
}
