package compare.bgrefresh.zio

import compare.bgrefresh.zio.interpreter.{ BackgroundRefreshAlgebra, BackgroundRefreshInterpreter }
import zio._
import zio.test._
import zio.test.Assertion._

object BackgroundRefreshFSMSpec extends ZIOSpecDefault {

  class BackgroundRefreshInterpreterWithFailure(
    successInterpreter: BackgroundRefreshInterpreter,
    counter: Ref[Int],
    dummyError: Exception,
    successAfterN: Int) extends BackgroundRefreshAlgebra[Task] {
    override def refresh(state: List[Int]): Task[List[Int]] = {
      for {
        counterValue <- counter.getAndUpdate(_ + 1)
        result <- if (counterValue < successAfterN)
          for {
            _ <- Console.printLine(s"counter [${counterValue}] <=> successAfterN [${successAfterN}] - failing")
            r <- ZIO.fail(dummyError)
          } yield r
        else
          for {
            _ <- Console.printLine(s"counter [${counterValue}] <=> successAfterN [${successAfterN}] - continuing w/ success")
            r <- successInterpreter.refresh(state)
          } yield r
      } yield result
    }

  }

  val dummyError = new Exception("dummy")
  val mdc = Map("foo" -> "bla")

  def spec = suite("background refresh")(
    test("refreshes the list when the tick is sent") {
      for {
        bgRefreshWithSuccess <- ZIO.attempt(new BackgroundRefreshInterpreter)

        ref <- Ref.make(List.empty[Int])
        fsm <- ZIO.attempt {
          implicit val b = bgRefreshWithSuccess
          new BackgroundRefreshFSM(ref)
        }

        initialState <- fsm.getState()
        _ <- assertTrue(initialState.isEmpty)

        _ <- fsm.refresh(mdc)

        nextState <- fsm.getState()
        r <- assertTrue(nextState == List(0))
      } yield r
    },

    test("refreshes continually") {
      for {
        failureCounter <- Ref.make(0)
        bgRefreshWithFailure <- ZIO.attempt {
          val success = new BackgroundRefreshInterpreter
          new BackgroundRefreshInterpreterWithFailure(success, failureCounter, dummyError, successAfterN = 2)
        }

        ref <- Ref.make(List.empty[Int])
        fsm <- ZIO.attempt {
          implicit val b = bgRefreshWithFailure
          new BackgroundRefreshFSM(ref)
        }

        _ <- fsm.refreshContinually(200.millis, mdc ++ Map("auto" -> "true"))

        initialState <- fsm.getState()
        _ <- assertTrue(initialState.isEmpty)

        // First two will fail since successAfterN = 2
        _ <- TestClock.adjust(200.millis)
        _ <- TestClock.adjust(200.millis)
        _ <- TestClock.adjust(200.millis)
        nextState <- fsm.getState()
        _ <- assertTrue(nextState == List(0))

        _ <- TestClock.adjust(200.millis)
        nextState <- fsm.getState()
        r <- assertTrue(nextState == List(0, 1))
      } yield r
    })
}
