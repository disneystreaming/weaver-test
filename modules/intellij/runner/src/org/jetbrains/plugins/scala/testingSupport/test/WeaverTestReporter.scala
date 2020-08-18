package org.jetbrains.plugins.scala.testingSupport.test

import java.io.{ PrintWriter, StringWriter }

import cats.effect.IO
import cats.effect.concurrent.Ref

import weaver.{ TestOutcome, TestStatus }

trait WeaverTestReporter {

  def reportScenario(parentId: Int, testClass: String, idCounter: Ref[IO, Int])(
      result: TestOutcome
  ): IO[Unit] = {
    import result._
    idCounter.modify(c => (c + 1, c)).map { id =>
      def reportError(message: String): Unit = {

        println(
          s"##teamcity[testStarted name='$name' parentNodeId='$parentId' nodeId='$id']"
        )
        val details =
          escapeString(cause.fold("")(ex => s"${stackTrace(ex)}"))
        val errorMessage = escapeString(cause.fold(message)(_.getMessage))
        println(
          s"##teamcity[testFailed nodeId='$id' message='$errorMessage' details='$details' duration='${duration.toMillis}']"
        )
        println(
          s"##teamcity[testStdOut nodeId='$id' out='${escapeString(formatted(TestOutcome.Verbose))}']"
        )

      }

      status match {
        case TestStatus.Success =>
          println(
            s"##teamcity[testStarted name='$name' parentNodeId='$parentId' nodeId='$id']"
          )
          println(
            s"##teamcity[testFinished nodeId='$id' duration='${duration.toMillis}']"
          )
        case TestStatus.Exception => reportError("Error")
        case TestStatus.Failure   => reportError("Failure")
        case TestStatus.Ignored =>
          println(
            s"##teamcity[testIgnored name='$name' parentNodeId='$parentId' nodeId='$id']"
          )
        case TestStatus.Cancelled =>
          println(
            s"##teamcity[testStarted name='$name' parentNodeId='$parentId' nodeId='$id']"
          )
          println(s"##teamcity[testFailed nodeId='$id' message='Canceled']")
      }
    }
  }

  def reportSuite(id: Int, testClass: String): IO[Unit] = {
    IO {
      println(
        s"##teamcity[testSuiteStarted parentNodeId='0' nodeId='$id' name='${escapeString(testClass)}']"
      )
    }
  }

  def suiteFinished(id: Int, testClass: String): IO[Unit] = {
    IO {
      println(
        s"##teamcity[testSuiteFinished parentNodeId='0' nodeId='$id' name='${escapeString(testClass)}']"
      )
    }
  }

  private def stackTrace(throwable: Throwable): String = {
    val sw = new StringWriter
    val pw = new PrintWriter(sw, true)
    throwable.printStackTrace(pw)
    sw.getBuffer.toString
  }

  private def escapeString(str: String): String = {
    if (str != null)
      str
        .replaceAll("[|]", "||")
        .replaceAll("[']", "|'")
        .replaceAll("[\n]", "|n")
        .replaceAll("[\r]", "|r")
        .replaceAll("]", "|]")
        .replaceAll("\\[", "|[")
    else ""
  }

}

object WeaverTestReporter extends WeaverTestReporter
