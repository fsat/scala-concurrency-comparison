package example.compare.fsm.zio.nested.logical

import example.compare.fsm.zio.nested.logical.LogicalResourceFSM.{ Message, RuntimeDependencies, State }
import fsm.zio.FSMContext
import zio._

class LogicalResourceRunning()(implicit deps: RuntimeDependencies) {
  private[logical] def apply(state: State.RunningState.DownloadingPartitionsState, message: Message.Request, ctx: FSMContext[Message.Request]): UIO[State] = {
    for {
      nextState <- message match {
        case m: Message.CreateOrUpdateRequest =>
          for {
            _ <- m.replyTo.succeed(Message.CreateOrUpdateResponse.Busy(m))
          } yield state
      }
    } yield nextState
  }
  private[logical] def apply(state: State.RunningState, message: Message.Request, ctx: FSMContext[Message.Request]): UIO[State] = {
    for {
      _ <- message match {
        case m: Message.CreateOrUpdateRequest =>
          // TODO
          ZIO.succeed(state)
      }
    } yield ???
  }
}
