package compare.bgrefresh.zio

import compare.bgrefresh.zio.interpreter.BackgroundRefreshInterpreter
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.Eventually
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import zio.{ Fiber, Ref, Runtime, Schedule, Task, Unsafe, ZIO }

import java.util.concurrent.TimeUnit
import scala.concurrent.duration._

class BackgroundRefreshFSMSpec extends AnyFunSpec with Matchers with Eventually with BeforeAndAfterAll {
  override implicit def patienceConfig: PatienceConfig = PatienceConfig(timeout = 3.seconds)

  val runtime = Runtime.default

  it("refreshes the list when the tick is sent") {
    Unsafe.unsafe { implicit unsafe =>
      val f = testFixture()
      import f._

      val initialState = runtime.unsafe.run(fsm.getState()).getOrThrow()
      initialState.isEmpty shouldBe true

      runtime.unsafe.run(fsm.refresh()).getOrThrow()

      val nextState = runtime.unsafe.run(fsm.getState()).getOrThrow()
      nextState shouldBe List(0)
    }
  }

  it("auto refreshes the list") {
    Unsafe.unsafe { implicit unsafe =>
      val f = testFixture(introduceFailure = true)
      import f._

      forkTask(fsm.refreshContinually(200.millis)) {
        eventually {
          val nextState = runtime.unsafe.run(fsm.getState()).getOrThrow()
          nextState shouldBe List(0, 1, 2)
        }
      }
    }
  }

  def testFixture(introduceFailure: Boolean = false, initialValue: List[Int] = List.empty)(implicit unsafe: Unsafe) = new {
    val dummyError = new Exception("dummy")
    val counter: Ref[Int] = runtime.unsafe.run(Ref.make(0)).getOrThrow()

    val bgRefreshWithFailure = new BackgroundRefreshInterpreter {
      override def refresh(state: List[Int]): Task[List[Int]] = {
        for {
          counterValue <- counter.getAndUpdate(_ + 1)
          result <- if (counterValue <= 2) {
            ZIO.fail(dummyError)
          } else {
            super.refresh(state)
          }
        } yield result
      }
    }
    val bgRefreshWithSuccess = new BackgroundRefreshInterpreter
    implicit val bgRefresh = if (introduceFailure) bgRefreshWithFailure else bgRefreshWithSuccess
    val ref = runtime.unsafe.run(Ref.make(initialValue)).getOrThrow()
    val fsm = new BackgroundRefreshFSM(ref)
  }

  private def forkTask[A, T](task: Task[A])(callback: => T)(implicit unsafe: Unsafe): T = {
    val fiber = runtime.unsafe.fork(task)
    try {
      callback
    } finally {
      runtime.unsafe.run(fiber.interrupt).getOrThrow()
    }
  }

}
