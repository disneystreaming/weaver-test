package weaver

import cats.effect.IO
import org.portablescala.reflect.Reflect
import scala.util.control.NoStackTrace

package object framework {

  type DeferredLogger = (String, Event) => IO[Unit]

  def suiteFromModule(
      qualifiedName: String,
      loader: ClassLoader): IO[EffectSuite[Any]] =
    castToSuite(loadModule(qualifiedName, loader))

  def suiteFromGlobalResourcesSharingClass(
      qualifiedName: String,
      globalResources: GlobalResources,
      loader: ClassLoader): IO[EffectSuite[Any]] =
    castToSuite(makeInstance(qualifiedName, globalResources, loader))

  private def castToSuite(io: IO[Any]): IO[EffectSuite[Any]] = io.flatMap {
    case ref: EffectSuite[_] => IO.pure(ref)
    case other =>
      IO.raiseError {
        new Exception(s"$other is not an effect suite") with NoStackTrace
      }
  }

  def loadModule(name: String, loader: ClassLoader): IO[Any] = {
    val moduleName = name + "$"
    IO(Reflect.lookupLoadableModuleClass(moduleName))
      .flatMap {
        case None =>
          IO.raiseError(
            new Exception(s"Could not load object $moduleName")
              with NoStackTrace
          )
        case Some(cls) => IO(cls.loadModule())
      }
  }

  private def makeInstance(
      name: String,
      globalResources: GlobalResources,
      loader: ClassLoader): IO[Any] = {
    IO(Reflect.lookupInstantiatableClass(name, loader))
      .flatMap {
        case None =>
          IO.raiseError(
            new Exception(s"Could not load class $name")
              with scala.util.control.NoStackTrace
          )
        case Some(cls) =>
          IO(cls.getConstructor(classOf[GlobalResources])).flatMap {
            case None =>
              val message =
                s"${cls.runtimeClass} is a class. It should either be an object, or have a constructor that takes a single parameter of type weaver.GlobalResources"
              IO.raiseError(new Exception(message)
                with scala.util.control.NoStackTrace)
            case Some(cst) =>
              IO(cst.newInstance(globalResources))
          }
      }
  }

}
