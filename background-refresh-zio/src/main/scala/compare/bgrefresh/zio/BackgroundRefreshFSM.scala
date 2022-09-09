package compare.bgrefresh.zio

import compare.bgrefresh.zio.interpreter.BackgroundRefreshAlgebra
import zio.{ Cause, Fiber, IO, Runtime, Ref, Schedule, Task, UIO, ZIO }
import zio.logging.backend.SLF4J

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration

class BackgroundRefreshFSM(state: Ref[List[Int]])(implicit refreshInterpreter: BackgroundRefreshAlgebra[Task]) {
  private val logger = Runtime.removeDefaultLoggers >>> SLF4J.slf4j

  def getState(): UIO[List[Int]] = state.get

  def refresh(): Task[Done] = {
    val attempt = for {
      _ <- ZIO.logInfo("Refreshing")
      stateContent <- state.get
      refreshResult <- refreshInterpreter.refresh(stateContent)
      _ <- state.set(refreshResult)
      _ <- ZIO.logInfo("Refreshing complete")
    } yield Done

    attempt.catchNonFatalOrDie { e =>
      for {
        _ <- ZIO.logErrorCause(s"Refreshing fail: ${e.getMessage}", Cause.fail(e))
        result <- ZIO.fail(e)
      } yield result
    }
  }

  def refreshContinually(interval: FiniteDuration): Task[Done] = {
    val zioInterval = zio.Duration(interval.toMillis, TimeUnit.MILLISECONDS)
    refresh()
      .retry(Schedule.exponential(zioInterval))
      .repeat(Schedule.spaced(zioInterval))
      .map(_ => Done)
  }
}
