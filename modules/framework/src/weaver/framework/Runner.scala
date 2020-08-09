package weaver
package framework

import cats.data.Chain
import cats.effect.concurrent.{ Ref, Semaphore }
import cats.effect.{ ContextShift, IO, Resource, Timer }
import cats.implicits._

import sbt.testing.{
  Logger => BaseLogger,
  Runner => BaseRunner,
  Task => BaseTask,
  _
}

import TestFramework._

final class Runner(
    val args: Array[String],
    val remoteArgs: Array[String],
    classLoader: ClassLoader)
    extends BaseRunner {

  def done(): String = ""

  val ec = scala.concurrent.ExecutionContext.global

  implicit val timerIO: Timer[IO]             = IO.timer(ec)
  implicit val contextShift: ContextShift[IO] = IO.contextShift(ec)

  def tasks(list: Array[TaskDef]): Array[BaseTask] = {
    val globalResourceModules: IO[List[GlobalResourcesInit]] = list
      .collect {
        case GlobalResourcesFingerprint(taskDef) =>
          loadModule(taskDef.fullyQualifiedName(), classLoader).flatMap {
            case g: GlobalResourcesInit => IO.pure(g)
            case other =>
              IO.raiseError {
                new Exception(s"$other is not an global resource initialiser")
                  with scala.util.control.NoStackTrace
              }
          }
      }
      .toList
      .traverse(identity)

    def globalAllocation(rw: GlobalResources.Write[IO]): IO[IO[Unit]] =
      globalResourceModules
        .flatMap(_.traverse(gr => gr.sharedResources(rw)).void.allocated)
        .map(_._2)

    val N = list.count {
      case ModuleFingerprint(_) | GlobalResourcesSharingFingerprint(_) => true
      case _                                                           => false
    }.toLong

    val prep = for {
      resourceMap <- GlobalResources.createMap
      semaphore   <- Semaphore[IO](N)
      _           <- semaphore.acquireN(N)
      cleanup     <- globalAllocation(resourceMap)
      ref         <- Ref[IO].of(Chain.empty[(String, TestOutcome)])
    } yield {

      val next = (loggers: Array[BaseLogger]) =>
        for {
          acquired <- semaphore.tryAcquireN(N)
          _ <- if (acquired)
            cleanup >> ref.get.flatMap(ReportTask.report(loggers))
          else IO.unit
        } yield ()

      val loggerResource: Resource[IO, DeferredLogger] =
        Resource.make(IO.pure[DeferredLogger]((str, event) =>
          ref.update(cat => cat.append(str -> event)))) { _ =>
          semaphore.release
        }

      list.collect {
        case ModuleFingerprint(taskDef) =>
          new Task(
            taskDef,
            args.toList,
            suiteFromModule(taskDef.fullyQualifiedName(), classLoader),
            loggerResource.some,
            next.some
          ): BaseTask
        case GlobalResourcesSharingFingerprint(taskDef) =>
          new Task(
            taskDef,
            args.toList,
            suiteFromGlobalResourcesSharingClass(
              taskDef.fullyQualifiedName(),
              resourceMap,
              classLoader),
            loggerResource.some,
            next.some
          ): BaseTask
      }

    }
    prep.unsafeRunSync()
  }

  def receiveMessage(msg: String): Option[String] = {
    discard[String](msg)
    None
  }

  def serializeTask(task: BaseTask, serializer: TaskDef => String): String =
    serializer(task.taskDef())

  def deserializeTask(
      task: String,
      deserializer: String => TaskDef): BaseTask = {
    val taskDef = deserializer(task)
    new Task(deserializer(task),
             args.toList,
             suiteFromModule(taskDef.fullyQualifiedName(), classLoader),
             None,
             None)
  }
}
