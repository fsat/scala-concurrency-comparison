package compare.fsm.zio.simple

import fsm.zio.Engine
import zio.test.{ ZIOSpecDefault, assertTrue }

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

        // Need to repeatedly call until value > 0 since `.tell` performs async operation
        getStateResponse2 <- engine.ask(CounterFSM.Message.GetStateRequest).repeatUntil(_.value > 0)
        r <- assertTrue(getStateResponse2 == CounterFSM.Message.GetStateResponse(1))
      } yield r
    })
}
