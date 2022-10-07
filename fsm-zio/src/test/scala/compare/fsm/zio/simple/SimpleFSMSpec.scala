package compare.fsm.zio.simple

import compare.fsm.zio.simple.SimpleFSMSpec.CounterFSM
import compare.fsm.zio.simple.SimpleFSMSpec.CounterFSM.Message
import org.scalatest.concurrent.{ Eventually, ScalaFutures }
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{ BeforeAndAfterAll, Inside }
import zio.logging.backend.SLF4J
import zio.{ Runtime, Task, Unsafe, ZIO }

import scala.concurrent.duration._

object SimpleFSMSpec {
  object CounterFSM {
    object Message {
      final case class GetStateRequest() extends Request[GetStateResponse]
      final case class GetStateResponse(value: Int) extends Response

      final case class IncrementRequest() extends Request[Nothing]

      sealed trait Request[+_] extends Message
      sealed trait Response extends Message

    }
    sealed trait Message extends Product with Serializable
  }
  class CounterFSM extends FSM[Int, Message.Request, Message.Response] {
    override def apply(state: Int, message: Message.Request[_]): Task[(Int, Option[Message.Response])] = {
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
      val t = for {
        engine <- Engine.create(0, new CounterFSM)

        getStateResponse1 <- engine.ask(CounterFSM.Message.GetStateRequest())
        _ <- ZIO.attempt {
          getStateResponse1.get shouldBe CounterFSM.Message.GetStateResponse(0)
        }

        _ <- engine.tell(CounterFSM.Message.IncrementRequest())

        getStateResponse2 <- engine.ask(CounterFSM.Message.GetStateRequest())
        _ <- ZIO.attempt {
          getStateResponse2.get shouldBe CounterFSM.Message.GetStateResponse(1)
        }
      } yield ()

      val _ = runtime.unsafe.run(t).getOrThrow()
    }
  }
}
