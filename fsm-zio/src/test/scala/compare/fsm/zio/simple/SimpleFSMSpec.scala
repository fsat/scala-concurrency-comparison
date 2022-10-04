package compare.fsm.zio.simple

import compare.fsm.zio.simple.SimpleFSMSpec.CounterFSM
import compare.fsm.zio.simple.SimpleFSMSpec.CounterFSM.Message
import org.scalatest.{ BeforeAndAfterAll, Inside }
import org.scalatest.concurrent.{ Eventually, ScalaFutures }
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import zio.logging.backend.SLF4J
import zio.{ IO, Runtime, Schedule, Task, Unsafe, ZIO }

import java.util.concurrent.TimeUnit
import scala.concurrent.duration._

object SimpleFSMSpec {
  object CounterFSM {
    object Message {
      final case class GetStateRequest() extends Request
      final case class GetStateResponse(value: Int) extends Response

      final case class IncrementRequest() extends Request

      sealed trait Request extends Message
      sealed trait Response extends Message

    }
    sealed trait Message extends Product with Serializable
  }
  class CounterFSM extends FSM[Int, Message.Request, Message.Response] {
    override def apply(state: Int, message: Message.Request): Task[(Int, Option[Message.Response])] = {
      ZIO.attempt {
        message match {
          case _: Message.IncrementRequest => (state + 1, None)
          case _: Message.GetStateRequest => (state, Some(Message.GetStateResponse(state)))
        }
      }
    }
  }
}

class SimpleFSMSpec extends AnyFunSpec with Matchers with Eventually with BeforeAndAfterAll with Inside with ScalaFutures {
  override implicit def patienceConfig: PatienceConfig = PatienceConfig(timeout = 3.seconds)

  val slf4j = SLF4J.slf4j
  val runtimeLayers = Runtime.removeDefaultLoggers ++ slf4j
  val runtime = Unsafe.unsafe { implicit unsafe =>
    Runtime.unsafe.fromLayer(runtimeLayers)
  }

  it("increments the counter") {
    Unsafe.unsafe { implicit unsafe =>
      val engine = runtime.unsafe.run(Engine.create(0, new CounterFSM)).getOrThrow()
      runtime.unsafe.runToFuture(engine.startProcessingLoop().repeat(Schedule.spaced(zio.Duration(10, TimeUnit.MILLISECONDS)))).future

      val getStateResponse1 = runtime.unsafe.runToFuture(engine.ask(CounterFSM.Message.GetStateRequest())).future
      getStateResponse1.futureValue.get shouldBe CounterFSM.Message.GetStateResponse(0)

      runtime.unsafe.runToFuture(engine.tell(CounterFSM.Message.IncrementRequest())).future

      val getStateResponse2 = runtime.unsafe.runToFuture(engine.ask(CounterFSM.Message.GetStateRequest())).future
      getStateResponse2.futureValue.get shouldBe CounterFSM.Message.GetStateResponse(1)
    }
  }
}
