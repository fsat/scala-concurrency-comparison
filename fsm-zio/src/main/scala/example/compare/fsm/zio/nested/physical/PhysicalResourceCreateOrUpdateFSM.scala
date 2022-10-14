package example.compare.fsm.zio.nested.physical

import example.compare.fsm.zio.nested.physical.PhysicalResourceFSM._
import fsm.zio.FSMContext
import zio._

import scala.util.{ Failure, Success }

class PhysicalResourceCreateOrUpdateFSM()(implicit deps: RuntimeDependencies) {
  import deps._

  private[physical] def apply(state: State.CreatingState, message: Message.Request, ctx: FSMContext[Message.Request]): UIO[State] = {
    for {
      _ <- ctx.pipeToSelfAsync(deps.physicalResource.create(state.request.endpointName, state.request.physicalResource))(MessageSelf.InitialState.PhysicalResourceCreateComplete)
      nextState <- message match {
        case r: MessageSelf.InitialState.PhysicalResourceCreateComplete =>
          r.result match {
            case Success(pid) =>
              for {
                _ <- state.request.replyTo.succeed(Message.CreateOrUpdateResponse.Creating(pid))
              } yield State.DownloadingArtifactsState(pid, state.request.physicalResource)

            case Failure(error) =>
              for {
                _ <- state.request.replyTo.succeed(Message.CreateOrUpdateResponse.Failure(error))
              } yield State.FailureState(error, None)
          }

        case r: Message.CreateOrUpdateRequest =>
          for {
            _ <- r.replyTo.succeed(Message.CreateOrUpdateResponse.Busy())
          } yield state

        case r: Message.GetStatusRequest =>
          for {
            _ <- r.replyTo.succeed(Message.GetStatusResponse.Creating())
          } yield state

        case _: MessageSelf.InitialState.FindEndpointComplete |
          _: MessageSelf.InitialState.PhysicalResourceUpdateComplete |
          _: MessageSelf.DownloadingArtifactsState.DownloadSingleArtifactComplete |
          _: MessageSelf.DownloadingArtifactsState.DownloadSingleArtifactComplete =>
          ZIO.succeed(state)
      }
    } yield nextState
  }

  private[physical] def apply(state: State.UpdatingState, message: Message.Request, ctx: FSMContext[Message.Request]): UIO[State] = {
    for {
      _ <- ctx.pipeToSelfAsync(deps.physicalResource.update(state.request.endpointName, state.request.physicalResource))(MessageSelf.InitialState.PhysicalResourceUpdateComplete)
      nextState <- message match {
        case r: MessageSelf.InitialState.PhysicalResourceUpdateComplete =>
          r.result match {
            case Success(pid) =>
              for {
                _ <- state.request.replyTo.succeed(Message.CreateOrUpdateResponse.Updating(pid))
              } yield State.DownloadingArtifactsState(pid, state.request.physicalResource)

            case Failure(error) =>
              for {
                _ <- state.request.replyTo.succeed(Message.CreateOrUpdateResponse.Failure(error))
              } yield State.FailureState(error, Some(state.existing))
          }

        case r: Message.CreateOrUpdateRequest =>
          for {
            _ <- r.replyTo.succeed(Message.CreateOrUpdateResponse.Busy())
          } yield state

        case r: Message.GetStatusRequest =>
          for {
            _ <- r.replyTo.succeed(Message.GetStatusResponse.Creating())
          } yield state

        case _: MessageSelf.InitialState.FindEndpointComplete |
          _: MessageSelf.InitialState.PhysicalResourceCreateComplete |
          _: MessageSelf.DownloadingArtifactsState.DownloadSingleArtifactComplete |
          _: MessageSelf.DownloadingArtifactsState.DownloadSingleArtifactComplete =>
          ZIO.succeed(state)
      }
    } yield nextState

  }
}
