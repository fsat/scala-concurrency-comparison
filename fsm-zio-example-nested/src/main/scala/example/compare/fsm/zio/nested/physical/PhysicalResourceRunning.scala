package example.compare.fsm.zio.nested.physical

import example.compare.fsm.zio.nested.physical.PhysicalResourceFSM.{ Message, MessageSelf, RuntimeDependencies, State }
import fsm.zio.FSMContext
import zio._

import scala.util.{ Failure, Success }

class PhysicalResourceRunning()(implicit deps: RuntimeDependencies) {
  import deps._

  private[physical] def apply(state: State.DownloadingArtifactsState, message: Message.Request, ctx: FSMContext[Message.Request]): URIO[Scope, State] = {
    def downloadArtifacts(): Task[Int] = {
      for {
        artifacts <- physicalResource.listArtifacts(state.physicalResource)
        downloadCount <- artifacts
          .mapZIOPar(downloadArtifactsParallelism) { artifact =>
            for {
              selfMessage <- physicalResource.downloadArtifact(state.physicalResource, artifact)
                .foldZIO(
                  err => ZIO.succeed(MessageSelf.DownloadingArtifactsState.DownloadSingleArtifactComplete(Failure(err))),
                  v => ZIO.succeed(MessageSelf.DownloadingArtifactsState.DownloadSingleArtifactComplete(Success(v))))
              _ <- ctx.self.tell(selfMessage)
            } yield selfMessage
          }
          .runFold(0) { (count, selfMessage) =>
            if (selfMessage.result.isSuccess) count + 1 else count
          }
      } yield downloadCount
    }

    def goToRunningState(): UIO[State.RunningState] = {
      ZIO.succeed(State.RunningState(state.id, state.physicalResource))
    }

    for {
      _ <- ctx.setup(state, state.isSetupDone) {
        for {
          _ <- ctx.pipeToSelfAsync(downloadArtifacts())(MessageSelf.DownloadingArtifactsState.DownloadArtifactsComplete)
        } yield state.copy(isSetupDone = true)
      }
      nextState <- message match {
        case r: MessageSelf.DownloadingArtifactsState.DownloadSingleArtifactComplete =>
          // TODO: event + logs
          goToRunningState()

        case r: MessageSelf.DownloadingArtifactsState.DownloadArtifactsComplete =>
          r.totalDownloaded match {
            case Success(totalCompleted) =>
              // TODO: logs
              goToRunningState()

            case Failure(error) =>
              // TODO: logs
              goToRunningState()
          }

        case r: Message.CreateOrUpdateRequest =>
          for {
            _ <- r.replyTo.tell(Message.CreateOrUpdateResponse.Busy())
          } yield state

        case r: Message.GetStatusRequest =>
          for {
            _ <- r.replyTo.succeed(Message.GetStatusResponse.DownloadingArtifacts())
          } yield state

        case _: MessageSelf.InitialState.FindEndpointComplete |
          _: MessageSelf.InitialState.PhysicalResourceCreateComplete |
          _: MessageSelf.InitialState.PhysicalResourceUpdateComplete =>
          ZIO.succeed(state)

      }
    } yield nextState
  }

  private[physical] def apply(state: State.RunningState, message: Message.Request, ctx: FSMContext[Message.Request]): URIO[Scope, State] = {
    message match {
      case r: Message.CreateOrUpdateRequest =>
        ZIO.succeed(State.UpdatingState(state.id, state.physicalResource, r, isSetupDone = false))

      case r: Message.GetStatusRequest =>
        for {
          _ <- r.replyTo.succeed(Message.GetStatusResponse.Running())
        } yield state

      case _: MessageSelf.InitialState.FindEndpointComplete |
        _: MessageSelf.InitialState.PhysicalResourceCreateComplete |
        _: MessageSelf.InitialState.PhysicalResourceUpdateComplete |
        _: MessageSelf.DownloadingArtifactsState.DownloadSingleArtifactComplete |
        _: MessageSelf.DownloadingArtifactsState.DownloadArtifactsComplete =>
        ZIO.succeed(state)
    }
  }
}
