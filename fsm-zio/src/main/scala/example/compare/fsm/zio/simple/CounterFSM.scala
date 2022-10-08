package example.compare.fsm.zio.simple

import example.compare.fsm.zio.simple.CounterFSM.Message
import example.compare.fsm.zio.simple.CounterFSM.Message.GetStateResponse
import fsm.zio.FSM
import zio._

object CounterFSM {
  object Message {
    final case class GetStateRequest(reply: Promise[Throwable, GetStateResponse]) extends Request
    final case class GetStateResponse(value: Int) extends Response

    final case class IncrementRequest() extends Request

    sealed trait Request extends Message

    sealed trait Response extends Message

  }

  sealed trait Message extends Product with Serializable
}

class CounterFSM extends FSM[Int, Message.Request] {
  override def apply(state: Int, message: Message.Request): Task[Int] = {
    message match {
      case _: Message.IncrementRequest => ZIO.attempt(state + 1)
      case r: Message.GetStateRequest =>
        for {
          _ <- r.reply.succeed(GetStateResponse(state))
        } yield state
    }
  }
}
