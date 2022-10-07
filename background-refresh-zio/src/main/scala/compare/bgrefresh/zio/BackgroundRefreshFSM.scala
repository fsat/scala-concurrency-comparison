package compare.bgrefresh.zio

import compare.bgrefresh.zio.BackgroundRefreshFSM.toLogAnnotation
import compare.bgrefresh.zio.interpreter.BackgroundRefreshAlgebra
import org.slf4j.{ LoggerFactory, MDC }
import zio.logging.LogAnnotation
import zio.logging.backend.SLF4J
import zio.{ Cause, Ref, Runtime, Schedule, Scope, Task, UIO, ZIO, ZIOAspect, durationInt }

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration
import scala.jdk.CollectionConverters._

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
  private val runtimeLogger = Runtime.removeDefaultLoggers >>> SLF4J.slf4j
  private val log = LoggerFactory.getLogger(this.getClass.getName)

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

    attempt.catchAllTrace { v =>
      implicit val (e, stackTrace) = v
      for {
        // TODO: is there a better way?
        _ <- ZIO.attempt {
          val existingMdc = Option(MDC.getCopyOfContextMap)
          MDC.setContextMap(mdc.asJava)
          log.error(s"Refreshing fail: ${e.getMessage}", e)
          existingMdc match {
            case Some(v) => MDC.setContextMap(v)
            case None => MDC.clear()
          }
        }
        _ <- ZIO.logErrorCause(s"Refreshing fail: ${e.getMessage}", Cause.fail(e)) @@ mdcLogAnnotation
        result <- ZIO.fail(e)
      } yield result
    }
  }

  def refreshContinually(interval: zio.Duration, mdc: Map[String, String]): Task[Done] = {
    for {
      parallelScope <- Scope.parallel
      _ <- refresh(mdc)
        .retry(Schedule.exponential(interval) && Schedule.upTo(interval.multipliedBy(10)))
        .repeat(Schedule.spaced(interval))
        .forkIn(parallelScope)
    } yield Done
  }

  def refreshContinually(interval: FiniteDuration, mdc: Map[String, String]): Task[Done] = {
    val zioInterval = zio.Duration(interval.toMillis, TimeUnit.MILLISECONDS)
    refreshContinually(zioInterval, mdc)
  }
}
