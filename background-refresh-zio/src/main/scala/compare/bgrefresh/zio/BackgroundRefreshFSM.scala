package compare.bgrefresh.zio

import compare.bgrefresh.zio.BackgroundRefreshFSM.toLogAnnotation
import compare.bgrefresh.zio.interpreter.BackgroundRefreshAlgebra
import zio.{ Cause, Fiber, IO, Ref, Runtime, Schedule, Task, UIO, ZIO, ZIOAspect }
import zio.json._
import zio.logging.LogAnnotation
import zio.logging.backend.SLF4J

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration

object BackgroundRefreshFSM {
  def toLogAnnotation(mdc: Map[String, String]): ZIOAspect[Nothing, Any, Nothing, Any, Nothing, Any] = {
    mdc.map { v =>
      val (key, value) = v
      // Not sure why .toString is required here, otherwise it won't compile
      LogAnnotation[String](key, _ + _, _.toString).apply(value)
    }
      // This is obviously unsafe if Map is empty, but we just do this quickly to show MDC as JSON fields
      .reduce(_ @@ _)
  }
}

class BackgroundRefreshFSM(state: Ref[List[Int]])(implicit refreshInterpreter: BackgroundRefreshAlgebra[Task]) {
  private val logger = Runtime.removeDefaultLoggers >>> SLF4J.slf4j

  def getState(): UIO[List[Int]] = state.get

  def refresh(mdc: Map[String, String]): Task[Done] = {
    val mdcLogAnnotation = toLogAnnotation(mdc)

    val attempt = for {
      _ <- ZIO.logInfo("Refreshing") @@ mdcLogAnnotation
      stateContent <- state.get
      refreshResult <- refreshInterpreter.refresh(stateContent)
      _ <- state.set(refreshResult)
      _ <- ZIO.logInfo("Refreshing complete") @@ mdcLogAnnotation
    } yield Done

    attempt.catchNonFatalOrDie { e =>
      for {
        _ <- ZIO.logErrorCause(s"Refreshing fail: ${e.getMessage}", Cause.fail(e)) @@ mdcLogAnnotation
        result <- ZIO.fail(e)
      } yield result
    }
  }

  def refreshContinually(interval: FiniteDuration, mdc: Map[String, String]): Task[Done] = {
    val zioInterval = zio.Duration(interval.toMillis, TimeUnit.MILLISECONDS)
    refresh(mdc)
      .retry(Schedule.exponential(zioInterval))
      .repeat(Schedule.spaced(zioInterval))
      .map(_ => Done)
  }
}
