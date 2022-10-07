package example.compare.fsm.zio.simple

import example.compare.fsm.zio.simple.CounterFSM.Message
import fsm.zio.FSM
import zio._

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
