package example.compare.fsm.zio.simple

import example.compare.fsm.zio.simple.CounterFSM.{ Message, State }
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

  object State {
    final case class Counter(value: Int) extends State
  }
  sealed trait State extends Product with Serializable
}

class CounterFSM extends FSM[State, Message.Request] {
  override def apply(state: State, message: Message.Request): Task[State] = {
    state match {
      case sc: State.Counter => apply(sc, message)
    }
  }

  private def apply(state: State.Counter, message: Message.Request): Task[State] = {
    message match {
      case _: Message.IncrementRequest => ZIO.attempt(State.Counter(state.value + 1))
      case r: Message.GetStateRequest =>
        for {
          _ <- r.reply.succeed(GetStateResponse(state.value))
        } yield state
    }
  }
}
