package example.compare.fsm.zio.nested.logical

import example.compare.fsm.zio.nested.events.ExampleEvent.LogicalResourceEvent
import example.compare.fsm.zio.nested.events.{ EventsAlgebra, EventsInterpreter, ExampleEvent }
import example.compare.fsm.zio.nested.logical.LogicalResourceFSM.{ Message, RuntimeDependencies, State }
import example.compare.fsm.zio.nested.physical.PhysicalResourceFSM
import fsm.zio.{ FSM, FSMContext, FSMRef }
import zio.UIO
import zio.stream.UStream

object LogicalResourceFSM {
  object Message {
    sealed trait Request extends Message
  }
  sealed trait Message extends Product with Serializable

  object State {
    final case class InitialState() extends State
    final case class CreatingState() extends State
    final case class UpdatingState() extends State
    object RunningState {
      final case class DownloadingPartitionsState(physicalResource: FSMRef[PhysicalResourceFSM.Message.Request]) extends State
    }
    final case class RunningState(physicalResource: FSMRef[PhysicalResourceFSM.Message.Request]) extends State
    final case class FailureState(physicalResource: Option[FSMRef[PhysicalResourceFSM.Message.Request]], error: Throwable) extends State
  }
  sealed trait State extends Product with Serializable

  final case class RuntimeDependencies(
    events: EventsAlgebra[UIO, EventsInterpreter.ScopedUIO, UStream, LogicalResourceEvent])
}

class LogicalResourceFSM()(implicit deps: RuntimeDependencies) extends FSM[State, Message.Request] {
  override def apply(state: State, message: Message.Request, ctx: FSMContext[Message.Request]): UIO[State] = {
    import deps._

    for {
      nextState <- state match {
        case s: State.InitialState => new LogicalResourceInitial().apply(s, message, ctx)
        case s: State.CreatingState => new LogicalResourceCreate().apply(s, message, ctx)
        case s: State.UpdatingState => new LogicalResourceUpdate().apply(s, message, ctx)
        case s: State.RunningState.DownloadingPartitionsState => new LogicalResourceRunning().apply(s, message, ctx)
        case s: State.RunningState => new LogicalResourceRunning().apply(s, message, ctx)
        case s: State.FailureState => new LogicalResourceFailure().apply(s, message, ctx)
      }
      _ <- events.publish(ExampleEvent.LogicalResourceEvent.StateTransition(state, nextState))
    } yield nextState
  }
}
