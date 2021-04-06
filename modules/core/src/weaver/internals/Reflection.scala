package weaver
package internals

import scala.reflect.ClassTag
import scala.util.control.NoStackTrace

import org.portablescala.reflect.Reflect

private[weaver] object Reflection {

  private[weaver] type AnyEffect[A] = Any

  private[weaver] def loadRunnableSuite[A](
      qualifiedName: String,
      loader: ClassLoader): RunnableSuite[AnyEffect] =
    Reflect.lookupLoadableModuleClass(qualifiedName, loader) match {
      case Some(cls) => cls.loadModule().asInstanceOf[RunnableSuite[AnyEffect]]
      case None =>
        Reflect.lookupInstantiatableClass(qualifiedName, loader) match {
          case None =>
            throw new Exception(s"Could not find class $qualifiedName")
              with NoStackTrace
          case Some(cls) =>
            cls.getConstructor(classOf[GlobalResourceF.Read[AnyEffect]]) match {
              case Some(value) =>
                // Instantiating with null a first time to retrieve the effect...
                val unused =
                  value.newInstance(null).asInstanceOf[RunnableSuite[AnyEffect]]
                val effectCompat = unused.getEffectCompat
                val read         = GlobalResourceF.Read.empty(effectCompat.effect)
                // Re-instantiating with empty global read.
                value.newInstance(read).asInstanceOf[RunnableSuite[AnyEffect]]
              case None =>
                throw new Exception("Could not find a suitable constructor that takes GlobalResourceF.Read")
            }
        }

    }

  private[weaver] def loadConstructor[A, C](
      qualifiedName: String,
      loader: ClassLoader)(
      implicit A: ClassTag[A],
      C: ClassTag[C]): A => C = {
    Reflect.lookupInstantiatableClass(qualifiedName, loader) match {
      case None =>
        throw new Exception(s"Could not find class $qualifiedName")
          with NoStackTrace
      case Some(cls) => cls.getConstructor(A.runtimeClass) match {
          case None =>
            val message =
              s"${cls.runtimeClass} is a class. It should either be an object, or have a constructor that takes a single parameter of type ${A.runtimeClass.getName()}"
            throw new Exception(message) with scala.util.control.NoStackTrace
          case Some(cst) => (a: A) => cast[C](cst.newInstance(a))
        }
    }
  }

  private[weaver] def cast[T](any: Any)(
      implicit T: ClassTag[T]): T = any match {
    case suite if T.runtimeClass.isInstance(suite) =>
      suite.asInstanceOf[T]
    case other =>
      throw new Exception(
        s"$other is not an instance of ${T.runtimeClass.getName()}")
        with NoStackTrace
  }

  private[weaver] def loadModule(
      qualifiedName: String,
      loader: ClassLoader): Any = {
    val moduleName = qualifiedName + "$"
    Reflect.lookupLoadableModuleClass(moduleName, loader) match {
      case None =>
        throw new Exception(s"Could not load object $moduleName")
          with NoStackTrace
      case Some(cls) => cls.loadModule()
    }
  }

}
