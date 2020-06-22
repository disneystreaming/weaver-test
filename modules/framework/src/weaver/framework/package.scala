package weaver

import cats.effect.IO
import org.portablescala.reflect.Reflect

package object framework {

  type DeferredLogger = (String, Event) => IO[Unit]

  def loadModule(name: String, loader: ClassLoader): IO[Any] = {
    val moduleName = name + "$"
    IO(Reflect.lookupLoadableModuleClass(moduleName))
      .flatMap {
        case None =>
          IO.raiseError(
            new Exception(s"Could not load class $moduleName")
              with scala.util.control.NoStackTrace
          )
        case Some(cls) => IO(cls.loadModule())
      }
  }

  def makeInstance(
      name: String,
      globalResources: GlobalResources,
      loader: ClassLoader): IO[Any] = {
    IO(Reflect.lookupInstantiatableClass(name))
      .flatMap {
        case None =>
          IO.raiseError(
            new Exception(s"Could not load class $name")
              with scala.util.control.NoStackTrace
          )
        case Some(cls) =>
          IO(cls.getConstructor(classOf[GlobalResources])).flatMap {
            case None =>
              IO.raiseError(
                new Exception(s"Could not find a constructor that takes a single weaver.GlobalResources parameter")
                  with scala.util.control.NoStackTrace
              )
            case Some(cst) =>
              IO(cst.newInstance(globalResources))
          }
      }
  }

}
