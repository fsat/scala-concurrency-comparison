package example.compare.fsm.zio.nested.logical

import example.compare.fsm.zio.nested.logical.LogicalResourceFSM.{ Message, RuntimeDependencies, State }
import fsm.zio.FSMContext
import zio._

class LogicalResourceCreate()(implicit deps: RuntimeDependencies) {
  private[logical] def apply(state: State.CreatingState, message: Message.Request, ctx: FSMContext[Message.Request]): UIO[State] = {
    message match {
      case m: Message.CreateOrUpdateRequest =>
        for {
          _ <- m.replyTo.succeed(Message.CreateOrUpdateResponse.Busy(m))
        } yield state
    }
  }
}
