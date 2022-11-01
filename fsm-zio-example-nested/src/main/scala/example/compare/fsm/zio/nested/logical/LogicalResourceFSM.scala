package example.compare.fsm.zio.nested.logical

import example.compare.fsm.zio.nested.events.ExampleEvent.LogicalResourceEvent
import example.compare.fsm.zio.nested.events.{EventsAlgebra, EventsInterpreter}
import example.compare.fsm.zio.nested.logical.LogicalResourceFSM.{Message, RuntimeDependencies, State}
import fsm.zio.{FSM, FSMContext}
import zio.UIO
import zio.stream.UStream

object LogicalResourceFSM {
  object Message {
    sealed trait Request extends Message
  }
  sealed trait Message extends Product with Serializable

  object State {
  }
  sealed trait State extends Product with Serializable

  final case class RuntimeDependencies(
    events: EventsAlgebra[UIO, EventsInterpreter.ScopedUIO, UStream, LogicalResourceEvent])
}

class LogicalResourceFSM()(implicit deps: RuntimeDependencies) extends FSM[State, Message.Request] {
  override def apply(state: State, message: Message.Request, ctx: FSMContext[Message.Request]): UIO[State] = ???
}
