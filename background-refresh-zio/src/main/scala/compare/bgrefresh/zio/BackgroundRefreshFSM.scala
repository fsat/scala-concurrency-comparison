package compare.bgrefresh.zio

import compare.bgrefresh.zio.interpreter.BackgroundRefreshAlgebra
import zio.{ Cause, Fiber, IO, Ref, Schedule, Task, UIO, ZIO }

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration

class BackgroundRefreshFSM(state: Ref[List[Int]])(implicit refreshInterpreter: BackgroundRefreshAlgebra[Task]) {
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
}
