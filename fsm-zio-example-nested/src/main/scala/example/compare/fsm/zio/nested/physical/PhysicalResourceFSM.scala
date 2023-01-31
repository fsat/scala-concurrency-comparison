package example.compare.fsm.zio.nested.physical

import example.compare.fsm.zio.nested.events.ExampleEvent.PhysicalResourceEvent
import example.compare.fsm.zio.nested.events.{ EventsAlgebra, EventsInterpreter }
import example.compare.fsm.zio.nested.physical.PhysicalResourceFSM._
import example.compare.fsm.zio.nested.physical.interpreter.PhysicalResourceAlgebra.ArtifactDownloadLocation
import example.compare.fsm.zio.nested.physical.interpreter.{ PhysicalResource, PhysicalResourceAlgebra }
import fsm.zio.{ FSM, FSMContext, FSMRef }
import zio._
import zio.stream._

import scala.util.Try

object PhysicalResourceFSM {
  object Message {
    final case class CreateOrUpdateRequest(
      endpointName: String,
      physicalResource: PhysicalResource,
      replyTo: FSMRef[CreateOrUpdateResponse]) extends Request
    object CreateOrUpdateResponse {
      final case class Creating(id: PhysicalResource.Id) extends CreateOrUpdateResponse
      final case class Updating(id: PhysicalResource.Id) extends CreateOrUpdateResponse
      final case class Busy() extends CreateOrUpdateResponse
      final case class Failure(error: Throwable) extends CreateOrUpdateResponse
    }
    sealed trait CreateOrUpdateResponse extends Product with Serializable

    final case class GetStatusRequest(replyTo: Promise[Throwable, GetStatusResponse]) extends Request
    object GetStatusResponse {
      final case class Initial() extends GetStatusResponse
      final case class Creating() extends GetStatusResponse
      final case class Updating() extends GetStatusResponse
      final case class DownloadingArtifacts() extends GetStatusResponse
      final case class Running() extends GetStatusResponse
      final case class Failure(error: Throwable) extends GetStatusResponse
    }
    sealed trait GetStatusResponse extends Product with Serializable

    sealed trait Request extends Message
  }
  sealed trait Message extends Product with Serializable

  object MessageSelf {
    object InitialState {
      final case class FindEndpointComplete(result: Try[Option[(PhysicalResource.Id, PhysicalResource)]]) extends MessageSelf
      final case class PhysicalResourceCreateComplete(result: Try[PhysicalResource.Id]) extends MessageSelf
      final case class PhysicalResourceUpdateComplete(result: Try[PhysicalResource.Id]) extends MessageSelf
    }

    object DownloadingArtifactsState {
      final case class DownloadSingleArtifactComplete(result: Try[ArtifactDownloadLocation]) extends MessageSelf
      final case class DownloadArtifactsComplete(totalDownloaded: Try[Int]) extends MessageSelf
    }
  }
  sealed trait MessageSelf extends Message.Request

  object State {
    object InitialState {
      final case class FindEndpointState(
        request: Message.CreateOrUpdateRequest,
        isSetupDone: Boolean) extends State
    }
    final case class InitialState() extends State
    final case class CreatingState(
      request: Message.CreateOrUpdateRequest,
      isSetupDone: Boolean) extends State
    final case class UpdatingState(
      id: PhysicalResource.Id,
      existing: PhysicalResource,
      request: Message.CreateOrUpdateRequest,
      isSetupDone: Boolean) extends State
    final case class DownloadingArtifactsState(
      id: PhysicalResource.Id,
      physicalResource: PhysicalResource,
      isSetupDone: Boolean) extends State
    final case class RunningState(id: PhysicalResource.Id, physicalResource: PhysicalResource) extends State
    final case class FailureState(error: Throwable, existing: Option[(PhysicalResource.Id, PhysicalResource)]) extends State
  }
  sealed trait State extends Product with Serializable

  final case class RuntimeDependencies(
    downloadArtifactsParallelism: Int,
    physicalResource: PhysicalResourceAlgebra[Task, ZStream],
    events: EventsAlgebra[UIO, EventsInterpreter.ScopedUIO, UStream, PhysicalResourceEvent])
}

class PhysicalResourceFSM()(implicit deps: RuntimeDependencies) extends FSM[State, Message.Request] {
  import deps._

  override def apply(state: State, message: Message.Request, ctx: FSMContext[Message.Request]): URIO[Scope, State] = {
    for {
      nextState <- state match {
        case s: State.InitialState => new PhysicalResourceInitial().apply(s, message, ctx)
        case s: State.InitialState.FindEndpointState => new PhysicalResourceInitial().apply(s, message, ctx)
        case s: State.CreatingState => new PhysicalResourceCreateOrUpdate().apply(s, message, ctx)
        case s: State.UpdatingState => new PhysicalResourceCreateOrUpdate().apply(s, message, ctx)
        case s: State.DownloadingArtifactsState => new PhysicalResourceRunning().apply(s, message, ctx)
        case s: State.RunningState => new PhysicalResourceRunning().apply(s, message, ctx)
        case s: State.FailureState => new PhysicalResourceFailure().apply(s, message, ctx)
      }
      _ <- events.publish(PhysicalResourceEvent.StateTransition(state, nextState))
    } yield nextState
  }

}
