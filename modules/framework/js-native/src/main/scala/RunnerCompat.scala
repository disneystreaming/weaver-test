package weaver
package framework

import java.nio.ByteBuffer
import org.typelevel.scalaccompat.annotation.unused

import scala.collection.mutable.ListBuffer
import scala.concurrent.Future
import scala.concurrent.duration._

import cats.data.Chain
import cats.effect.kernel.Async
import cats.effect.{ Ref, Sync }
import cats.syntax.all._

import sbt.testing.{ EventHandler, Logger, Task, TaskDef }

trait RunnerCompat[F[_]] { self: sbt.testing.Runner =>
  protected val args: Array[String]
  protected val suiteLoader: SuiteLoader[F]
  protected val unsafeRun: UnsafeRun[F]
  protected val channel: Option[String => Unit]

  import unsafeRun._

  private[weaver] val failedTests = ListBuffer.empty[(SuiteName, TestOutcome)]

  def reportDone(out: TestOutcomeNative): Unit = {
    val serialised = ReadWriter.writer { p =>
      p.writeString(out.suiteName)
      p.writeString(out.testName)
      p.writeDouble(out.durationMs)
      p.writeString(out.verboseFormatting)
      ()
    }
    channel match {
      case Some(send) => send(serialised)
      case None       => failedTests.append(TestOutcomeNative.rehydrate(out))
    }
  }

  def reportDoneF(out: TestOutcomeNative): F[Unit] =
    Sync[F].delay(reportDone(out))

  override def deserializeTask(
      task: String,
      deserialize: String => sbt.testing.TaskDef): sbt.testing.Task = {
    val taskDef = deserialize(task)

    val suiteRefs = suiteLoader(taskDef).collect {
      case suite: suiteLoader.SuiteRef => suite
    }

    SbtTask(taskDef, suiteRefs)
  }

  override def serializeTask(
      task: sbt.testing.Task,
      serializer: sbt.testing.TaskDef => String): String = {
    serializer(task.taskDef())
  }

  override def done(): String = {
    val sb = new StringBuilder

    val s = { (str: String) =>
      val _ = sb.append(str + TaskCompat.lineSeparator)
    }

    Reporter.runFinished(s, s)(Chain(failedTests.toSeq: _*))

    sb.result()
  }

  override def receiveMessage(msg: String): Option[String] = {
    val outcome = ReadWriter.reader(msg) { p =>
      val suite = p.readString()
      val test  = p.readString()
      val dur   = p.readDouble()
      val verb  = p.readString()

      new TestOutcomeNative(suiteName = suite,
                            testName = test,
                            durationMs = dur,
                            verboseFormatting = verb)
    }
    reportDone(outcome)
    None
  }

  override def tasks(taskDefs: Array[TaskDef]): Array[Task] = {
    val tasksAndSuites = taskDefs.toList.map { taskDef =>
      taskDef -> suiteLoader(taskDef)
    }.collect {
      case (taskDef, Some(suite: suiteLoader.SuiteRef)) => (taskDef, suite)
    }

    tasksAndSuites.map { case (td, ld) => SbtTask(td, Some(ld)) }.toArray
  }

  private case class SbtTask(td: TaskDef, loader: Option[suiteLoader.SuiteRef])
      extends PlatformTask {
    override def tags(): Array[String] = Array()

    def executeFuture(
        eventHandler: EventHandler,
        loggers: Array[Logger]): Future[Unit] = {
      val fqn = taskDef().fullyQualifiedName()

      def reportTest(outcome: TestOutcome) =
        effect.delay(eventHandler.handle(SbtEvent(td, outcome)))

      def runSuite(
          fqn: String,
          suite: EffectSuite[F],
          outcomes: Ref[F, Chain[TestOutcome]]): F[Unit] = for {
        _ <- effect.delay(Reporter.logSuiteStarted(loggers)(SuiteName(fqn)))
        _ <- suite.run(args.toList) { outcome =>
          effect.delay(Reporter.logTestFinished(loggers)(outcome))
            .productR(reportTest(outcome))
            .productR(outcomes.update(_.append(outcome)))
        }
      } yield ()

      def finaliseCompleted(outcomes: Ref[F, Chain[TestOutcome]]): F[Unit] = {
        val failedF = outcomes.get.map(
          _.filter(_.status.isFailed).map(SuiteName(fqn) -> _))

        failedF.flatMap {
          case c if c.isEmpty => effect.unit
          case failed => {
            val ots: Chain[TestOutcomeNative] =
              failed.map { case (SuiteName(name), to) =>
                TestOutcomeNative.from(name)(to)
              }

            ots.traverse(reportDoneF).void
          }
        }
      }

      def finaliseError(@unused outcomes: Ref[
        F,
        Chain[TestOutcome]]): Throwable => F[Unit] = { error =>
        val outcome =
          TestOutcome("Unexpected failure",
                      0.seconds,
                      Result.from(error),
                      Chain.empty)
        reportTest(outcome).productR(
          reportDoneF(TestOutcomeNative.from(fqn)(outcome)))
      }

      val action = loader match {
        case None => effect.unit
        case Some(loader) => for {
            outcomes <- Ref.of(Chain.empty[TestOutcome])
            loadAndRun = loader.suite.flatMap(runSuite(fqn, _, outcomes))
            _ <- Async[F].background(loadAndRun).use {
              _.flatMap {
                _.fold(
                  canceled = effect.unit,
                  completed = _ *> finaliseCompleted(outcomes),
                  errored = finaliseError(outcomes)
                )
              }
            }
          } yield ()
      }

      unsafeRun.unsafeRunToFuture(action)
    }

    override def taskDef(): TaskDef = td

  }

}

private[weaver] object ReadWriter {
  class Reader(bytes: ByteBuffer, @unused private var pt: Int) {
    def readString() = {
      val stringSize = bytes.getInt()
      val ar         = new Array[Byte](stringSize)
      bytes.get(ar)

      new String(ar)
    }

    def readDouble() = bytes.getDouble
  }

  class Writer(bb: ByteBuffer) {

    def writeString(s: String) = {
      bb.putInt(s.getBytes.size)
      bb.put(s.getBytes())
    }

    def writeDouble(d: Double) = bb.putDouble(d)
  }

  def reader[A](s: String)(f: Reader => A) = {
    val buf = ByteBuffer.wrap(s.getBytes)
    f(new Reader(buf, 0))
  }

  def writer(f: Writer => Unit): String = {
    val buf = ByteBuffer.allocate(2048)

    f(new Writer(buf))

    new String(buf.array())

  }
}

case class TestOutcomeNative(
    suiteName: String,
    testName: String,
    durationMs: Double,
    verboseFormatting: String
)

object TestOutcomeNative {
  def from(suiteName: String)(outcome: TestOutcome): TestOutcomeNative = {
    new TestOutcomeNative(
      suiteName,
      outcome.name,
      outcome.duration.toMillis.toDouble,
      outcome.formatted(TestOutcome.Verbose))
  }

  def rehydrate(t: TestOutcomeNative): (SuiteName, TestOutcome) = {
    SuiteName(t.suiteName) -> DecodedOutcome(
      t.testName,
      t.durationMs.millis,
      t.verboseFormatting
    )
  }

  private case class DecodedOutcome(
      testName: String,
      dur: FiniteDuration,
      verboseFormatting: String)
      extends TestOutcome {
    def name: String                              = testName
    def duration: FiniteDuration                  = dur
    def status: TestStatus                        = TestStatus.Failure
    def log: Chain[Log.Entry]                     = Chain.empty
    def formatted(mode: TestOutcome.Mode): String = verboseFormatting
    def cause: Option[Throwable]                  = None
  }
}
