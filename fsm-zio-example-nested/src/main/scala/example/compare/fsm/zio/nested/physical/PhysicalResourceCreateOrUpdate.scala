package example.compare.fsm.zio.nested.physical

import example.compare.fsm.zio.nested.physical.PhysicalResourceFSM._
import fsm.zio.FSMContext
import zio._

import scala.util.{ Failure, Success }

class PhysicalResourceCreateOrUpdate()(implicit deps: RuntimeDependencies) {

  private[physical] def apply(state: State.CreatingState, message: Message.Request, ctx: FSMContext[Message.Request]): URIO[Scope, State] = {
    for {
      _ <- ctx.setup(state, state.isSetupDone) {
        for {
          _ <- ctx.pipeToSelfAsync(deps.physicalResource.create(state.request.endpointName, state.request.physicalResource))(MessageSelf.InitialState.PhysicalResourceCreateComplete)
        } yield state.copy(isSetupDone = true)
      }

      nextState <- message match {
        case r: MessageSelf.InitialState.PhysicalResourceCreateComplete =>
          r.result match {
            case Success(pid) =>
              for {
                _ <- state.request.replyTo.tell(Message.CreateOrUpdateResponse.Creating(pid))
              } yield State.DownloadingArtifactsState(pid, state.request.physicalResource, isSetupDone = false)

            case Failure(error) =>
              for {
                _ <- state.request.replyTo.tell(Message.CreateOrUpdateResponse.Failure(error))
              } yield State.FailureState(error, None)
          }

        case r: Message.CreateOrUpdateRequest =>
          for {
            _ <- r.replyTo.tell(Message.CreateOrUpdateResponse.Busy())
          } yield state

        case r: Message.GetStatusRequest =>
          for {
            _ <- r.replyTo.succeed(Message.GetStatusResponse.Creating())
          } yield state

        case _: MessageSelf.InitialState.FindEndpointComplete |
          _: MessageSelf.InitialState.PhysicalResourceUpdateComplete |
          _: MessageSelf.DownloadingArtifactsState.DownloadSingleArtifactComplete |
          _: MessageSelf.DownloadingArtifactsState.DownloadArtifactsComplete =>
          ZIO.succeed(state)
      }
    } yield nextState
  }

  private[physical] def apply(state: State.UpdatingState, message: Message.Request, ctx: FSMContext[Message.Request]): URIO[Scope, State] = {
    for {
      stateWithSetup <- ctx.setup(state, state.isSetupDone) {
        for {
          _ <- ctx.pipeToSelfAsync(deps.physicalResource.update(state.request.endpointName, state.request.physicalResource))(MessageSelf.InitialState.PhysicalResourceUpdateComplete)
        } yield state.copy(isSetupDone = true)
      }

      nextState <- message match {
        case r: MessageSelf.InitialState.PhysicalResourceUpdateComplete =>
          r.result match {
            case Success(pid) =>
              for {
                _ <- state.request.replyTo.tell(Message.CreateOrUpdateResponse.Updating(pid))
              } yield State.DownloadingArtifactsState(pid, state.request.physicalResource, isSetupDone = false)

            case Failure(error) =>
              for {
                _ <- state.request.replyTo.tell(Message.CreateOrUpdateResponse.Failure(error))
              } yield State.FailureState(error, Some((state.id, state.existing)))
          }

        case r: Message.CreateOrUpdateRequest =>
          for {
            _ <- r.replyTo.tell(Message.CreateOrUpdateResponse.Busy())
          } yield state

        case r: Message.GetStatusRequest =>
          for {
            _ <- r.replyTo.succeed(Message.GetStatusResponse.Creating())
          } yield stateWithSetup

        case _: MessageSelf.InitialState.FindEndpointComplete |
          _: MessageSelf.InitialState.PhysicalResourceCreateComplete |
          _: MessageSelf.DownloadingArtifactsState.DownloadSingleArtifactComplete |
          _: MessageSelf.DownloadingArtifactsState.DownloadArtifactsComplete =>
          ZIO.succeed(stateWithSetup)
      }
    } yield nextState

  }
}
