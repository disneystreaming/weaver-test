package weaver

import scala.util.control.NoStackTrace

import org.portablescala.reflect.Reflect

package object framework {

  def suiteFromModule(
      qualifiedName: String,
      loader: ClassLoader): BaseIOSuite =
    castToSuite(loadModule(qualifiedName, loader))

  // def suiteFromGlobalResourcesSharingClass(
  //   qualifiedName: String,
  //   globalResources: GlobalResources,
  //   loader: ClassLoader): IO[EffectSuite[Any]] =
  // castToSuite(makeInstance(qualifiedName, globalResources, loader))

  private def castToSuite(any: Any): BaseIOSuite = any match {
    case suite: BaseIOSuite =>
      suite
    case other =>
      throw new Exception(s"$other is not an effect suite") with NoStackTrace
  }

  def loadModule(name: String, loader: ClassLoader): Any = {
    val moduleName = name + "$"
    Reflect.lookupLoadableModuleClass(moduleName) match {
      case None =>
        throw new Exception(s"Could not load object $moduleName")
          with NoStackTrace
      case Some(cls) => cls.loadModule()
    }

  }

  // private def makeInstance(
  //     name: String,
  //     globalResources: GlobalResources,
  //     loader: ClassLoader): IO[Any] = {
  //   IO(Reflect.lookupInstantiatableClass(name, loader))
  //     .flatMap {
  //       case None =>
  //         IO.raiseError(
  //           new Exception(s"Could not load class $name")
  //             with scala.util.control.NoStackTrace
  //         )
  //       case Some(cls) =>
  //         IO(cls.getConstructor(classOf[GlobalResources])).flatMap {
  //           case None =>
  //             val message =
  //               s"${cls.runtimeClass} is a class. It should either be an object, or have a constructor that takes a single parameter of type weaver.GlobalResources"
  //             IO.raiseError(new Exception(message)
  //               with scala.util.control.NoStackTrace)
  //           case Some(cst) =>
  //             IO(cst.newInstance(globalResources))
  //         }
  //     }
  // }

}
