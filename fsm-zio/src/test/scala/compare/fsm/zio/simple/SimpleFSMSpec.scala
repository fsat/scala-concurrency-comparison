package compare.fsm.zio.simple

import example.compare.fsm.zio.simple.CounterFSM
import fsm.zio.Engine
import zio._
import zio.test._

object SimpleFSMSpec extends ZIOSpecDefault {

  override def spec = suite("simple fsm")(
    test("increments the counter") {
      for {
        engine <- Engine.create(CounterFSM.State.Counter(0), new CounterFSM)

        getStateResponse1 <- engine.ask(CounterFSM.Message.GetStateRequest)
        _ <- assertTrue(getStateResponse1 == CounterFSM.Message.GetStateResponse(0))

        _ <- engine.tell(CounterFSM.Message.IncrementRequest())

        getStateResponse2 <- engine.ask(CounterFSM.Message.GetStateRequest)
        r <- assertTrue(getStateResponse2 == CounterFSM.Message.GetStateResponse(1))
      } yield r
    },
    test("self message to increment the counter") {
      for {
        engine <- Engine.create(CounterFSM.State.Counter(0), new CounterFSM)

        getStateResponse1 <- engine.ask(CounterFSM.Message.GetStateRequest)
        _ <- assertTrue(getStateResponse1 == CounterFSM.Message.GetStateResponse(0))

        _ <- engine.tell(CounterFSM.Message.SelfIncrementRequest())

        getStateResponse2 <- engine.ask(CounterFSM.Message.GetStateRequest)
        r <- assertTrue(getStateResponse2 == CounterFSM.Message.GetStateResponse(1))
      } yield r
    })
}
