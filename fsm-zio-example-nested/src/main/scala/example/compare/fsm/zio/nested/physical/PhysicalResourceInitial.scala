package example.compare.fsm.zio.nested.physical

import example.compare.fsm.zio.nested.physical.PhysicalResourceFSM._
import fsm.zio.FSMContext
import zio._

import scala.util.{ Failure, Success }

class PhysicalResourceInitial()(implicit deps: RuntimeDependencies) {
  import deps._

  private[physical] def apply(state: State.InitialState, message: Message.Request, ctx: FSMContext[Message.Request]): UIO[State] = {
    message match {
      case r: Message.CreateOrUpdateRequest =>
        ZIO.succeed(State.InitialState.FindEndpointState(r))

      case r: Message.GetStatusRequest =>
        for {
          _ <- r.replyTo.succeed(Message.GetStatusResponse.Initial())
        } yield state

      case _: MessageSelf.InitialState.FindEndpointComplete |
        _: MessageSelf.InitialState.PhysicalResourceCreateComplete |
        _: MessageSelf.InitialState.PhysicalResourceUpdateComplete |
        _: MessageSelf.DownloadingArtifactsState.DownloadSingleArtifactComplete |
        _: MessageSelf.DownloadingArtifactsState.DownloadArtifactsComplete =>
        ZIO.succeed(state)
    }
  }

  private[physical] def apply(state: State.InitialState.FindEndpointState, message: Message.Request, ctx: FSMContext[Message.Request]): UIO[State] = {
    for {
      _ <- ctx.pipeToSelfAsync(physicalResource.find(state.request.endpointName))(MessageSelf.InitialState.FindEndpointComplete)
      nextState <- message match {
        case r: MessageSelf.InitialState.FindEndpointComplete =>
          r.result match {
            case Success(Some((existingResourceId, existingResouce))) => ZIO.succeed(State.UpdatingState(existingResourceId, existingResouce, state.request))
            case Success(None) => ZIO.succeed(State.CreatingState(state.request))
            case Failure(e) =>
              for {
                _ <- state.request.replyTo.fail(e)
              } yield State.FailureState(e, None)
          }

        case r: Message.CreateOrUpdateRequest =>
          for {
            _ <- r.replyTo.succeed(Message.CreateOrUpdateResponse.Busy())
          } yield state

        case r: Message.GetStatusRequest =>
          for {
            _ <- r.replyTo.succeed(Message.GetStatusResponse.Initial())
          } yield state

        case _: MessageSelf.InitialState.FindEndpointComplete |
          _: MessageSelf.InitialState.PhysicalResourceCreateComplete |
          _: MessageSelf.InitialState.PhysicalResourceUpdateComplete |
          _: MessageSelf.DownloadingArtifactsState.DownloadSingleArtifactComplete |
          _: MessageSelf.DownloadingArtifactsState.DownloadArtifactsComplete =>
          ZIO.succeed(state)
      }
    } yield nextState
  }
}
