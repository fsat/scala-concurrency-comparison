package compare.fsm.zio.simple

import example.compare.fsm.zio.simple.CounterFSM
import zio.durationInt
import zio.test._

object SimpleFSMSpec extends ZIOSpecDefault {

  override def spec = suite("simple fsm") {
    test("increments the counter") {
      for {
        engine <- Engine.create(0, new CounterFSM)

        //        _ <- TestClock.adjust(100.millis)

        getStateResponse1 <- engine.ask(CounterFSM.Message.GetStateRequest())
        _ <- assertTrue(getStateResponse1.get == CounterFSM.Message.GetStateResponse(0))

        //        _ <- TestClock.adjust(100.millis)

        _ <- engine.tell(CounterFSM.Message.IncrementRequest())

        //        _ <- TestClock.adjust(100.millis)

        getStateResponse2 <- engine.ask(CounterFSM.Message.GetStateRequest())
        r <- assertTrue(getStateResponse2.get == CounterFSM.Message.GetStateResponse(1))
      } yield r
    }
  }
}
