package example.compare.fsm.zio.nested.physical

import example.compare.fsm.zio.nested.physical.PhysicalResourceFSM.{ Message, MessageSelf, RuntimeDependencies, State }
import fsm.zio.FSMContext
import zio._

class PhysicalResourceFailureFSM()(implicit deps: RuntimeDependencies) {

  private[physical] def apply(state: State.FailureState, message: Message.Request, ctx: FSMContext[Message.Request]): UIO[State] = {
    message match {
      case r: Message.CreateOrUpdateRequest =>
        ZIO.succeed {
          state.existing match {
            case None => State.CreatingState(r)
            case Some((id, resource)) => State.UpdatingState(id, resource, r)
          }
        }

      case r: Message.GetStatusRequest =>
        for {
          _ <- r.replyTo.succeed(Message.GetStatusResponse.Failure(state.error))
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

