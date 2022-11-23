package example.compare.fsm.zio.nested.logical

import example.compare.fsm.zio.nested.events.ExampleEvent.LogicalResourceEvent
import example.compare.fsm.zio.nested.events.{ EventsAlgebra, EventsInterpreter, ExampleEvent }
import example.compare.fsm.zio.nested.logical.LogicalResourceFSM.{ Message, RuntimeDependencies, State }
import example.compare.fsm.zio.nested.logical.interpreter.{ LogicalResource, LogicalResourceAlgebra }
import example.compare.fsm.zio.nested.physical.PhysicalResourceFSM
import fsm.zio.{ FSM, FSMContext, FSMRef }
import zio.{ Promise, Task, UIO }
import zio.stream.UStream

object LogicalResourceFSM {
  object Message {
    final case class CreateOrUpdateRequest(
      endpointName: String,
      logicalResource: LogicalResource,
      replyTo: Promise[Throwable, CreateOrUpdateResponse]) extends Request

    object CreateOrUpdateResponse {
      final case class Accepted(request: CreateOrUpdateRequest) extends CreateOrUpdateResponse
      final case class Busy(request: CreateOrUpdateRequest) extends CreateOrUpdateResponse
    }
    sealed trait CreateOrUpdateResponse

    sealed trait Request extends Message
  }
  object MessageSelf {
    object FromPhysicalResourceFSM {
      final case class CreateOrUpdateResponse(payload: PhysicalResourceFSM.Message.CreateOrUpdateResponse) extends MessageSelf
    }
  }
  sealed trait MessageSelf extends Message.Request
  sealed trait Message extends Product with Serializable

  object State {
    final case class InitialState() extends State
    final case class CreatingState(
      request: Message.CreateOrUpdateRequest,
      physicalResource: FSMRef.Local[PhysicalResourceFSM.Message.Request],
      isSetup: Boolean) extends State
    final case class UpdatingState() extends State
    object RunningState {
      final case class DownloadingPartitionsState(physicalResource: FSMRef.Local[PhysicalResourceFSM.Message.Request]) extends State
    }
    final case class RunningState(physicalResource: FSMRef.Local[PhysicalResourceFSM.Message.Request]) extends State
    final case class FailureState(physicalResource: Option[FSMRef.Local[PhysicalResourceFSM.Message.Request]], error: Throwable) extends State
  }
  sealed trait State extends Product with Serializable

  final case class RuntimeDependencies(
    logicalResource: LogicalResourceAlgebra[Task],
    events: EventsAlgebra[UIO, EventsInterpreter.ScopedUIO, UStream, LogicalResourceEvent])(implicit val physicalResourceDeps: PhysicalResourceFSM.RuntimeDependencies)
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
