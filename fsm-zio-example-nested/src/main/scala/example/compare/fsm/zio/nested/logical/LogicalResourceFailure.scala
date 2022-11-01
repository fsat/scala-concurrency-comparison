package example.compare.fsm.zio.nested.logical

import example.compare.fsm.zio.nested.logical.LogicalResourceFSM.{ Message, RuntimeDependencies, State }
import fsm.zio.FSMContext
import zio._

class LogicalResourceFailure()(implicit deps: RuntimeDependencies) {
  private[logical] def apply(state: State.FailureState, message: Message.Request, ctx: FSMContext[Message.Request]): UIO[State] = {
    for {
      _ <- message match {
        case m: Message.CreateOrUpdateRequest =>
          // TODO
          ZIO.succeed(state)
      }
    } yield ???
  }
}
