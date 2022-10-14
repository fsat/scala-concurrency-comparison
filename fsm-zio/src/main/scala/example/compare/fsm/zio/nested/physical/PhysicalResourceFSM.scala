package example.compare.fsm.zio.nested.physical

import fsm.zio.{ FSM, FSMContext }
import PhysicalResourceFSM._
import example.compare.fsm.zio.nested.physical.interpreter.PhysicalResourceAlgebra.ArtifactDownloadLocation
import example.compare.fsm.zio.nested.physical.interpreter.{ PhysicalResource, PhysicalResourceAlgebra }
import zio.stream.ZStream
import zio.{ Task, _ }

import java.net.URL
import scala.util.Try

object PhysicalResourceFSM {
  object Message {
    final case class CreateOrUpdateRequest(
      endpointName: String,
      physicalResource: PhysicalResource,
      replyTo: Promise[Throwable, CreateOrUpdateResponse]) extends Request
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
      final case class Failure() extends GetStatusResponse
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
      final case class FindEndpointState(request: Message.CreateOrUpdateRequest) extends State
    }
    final case class InitialState() extends State
    final case class CreatingState(request: Message.CreateOrUpdateRequest) extends State
    final case class UpdatingState(id: PhysicalResource.Id, existing: PhysicalResource, request: Message.CreateOrUpdateRequest) extends State
    final case class DownloadingArtifactsState(id: PhysicalResource.Id, physicalResource: PhysicalResource) extends State
    final case class RunningState(id: PhysicalResource.Id, physicalResource: PhysicalResource) extends State
    final case class FailureState(error: Throwable, existing: Option[PhysicalResource]) extends State
  }
  sealed trait State extends Product with Serializable

  final case class RuntimeDependencies(
    downloadArtifactsParallelism: Int,
    physicalResource: PhysicalResourceAlgebra[Task, ZStream])
}

class PhysicalResourceFSM()(implicit deps: RuntimeDependencies) extends FSM[State, Message.Request] {
  import deps._

  override def apply(state: State, message: Message.Request, ctx: FSMContext[Message.Request]): UIO[State] =
    state match {
      case s: State.InitialState => new PhysicalResourceInitialFSM().apply(s, message, ctx)
      case s: State.InitialState.FindEndpointState => new PhysicalResourceInitialFSM().apply(s, message, ctx)
      case s: State.CreatingState => new PhysicalResourceCreateOrUpdateFSM().apply(s, message, ctx)
      case s: State.UpdatingState => new PhysicalResourceCreateOrUpdateFSM().apply(s, message, ctx)
      case s: State.DownloadingArtifactsState => new PhysicalResourceRunningFSM().apply(s, message, ctx)
      case s: State.RunningState => ???
      case s: State.FailureState => ???
    }

}
