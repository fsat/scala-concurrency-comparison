package compare.bgrefresh.zio

import compare.bgrefresh.zio.interpreter.BackgroundRefreshInterpreter
import org.scalatest.concurrent.Eventually
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import zio.{ Ref, Runtime, Unsafe }

class BackgroundRefreshFSMSpec extends AnyFunSpec with Matchers with Eventually {
  val runtime = Runtime.default
  implicit val bgRefreshInterpreter = new BackgroundRefreshInterpreter

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

  def testFixture(initialValue: List[Int] = List.empty)(implicit unsafe: Unsafe) = new {
    val ref = runtime.unsafe.run(Ref.make(initialValue)).getOrThrow()
    val fsm = new BackgroundRefreshFSM(ref)
  }

}
