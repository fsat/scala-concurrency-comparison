package example.compare.fsm.zio.nested.logical

import example.compare.fsm.zio.nested.logical.LogicalResourceFSM.{ Message, RuntimeDependencies, State }
import example.compare.fsm.zio.nested.physical.PhysicalResourceFSM
import fsm.zio.FSMContext
import zio._

class LogicalResourceInitial()(implicit deps: RuntimeDependencies) {
  private[logical] def apply(state: State.InitialState, message: Message.Request, ctx: FSMContext[Message.Request]): UIO[State] = {
    import deps._

    message match {
      case m: Message.CreateOrUpdateRequest =>
        for {
          physicalResourceFSM <- ctx.createFSM(PhysicalResourceFSM.State.InitialState(), new PhysicalResourceFSM())
          _ <- m.replyTo.succeed(Message.CreateOrUpdateResponse.Accepted(m))
        } yield State.CreatingState(m, physicalResourceFSM, isSetup = false)
    }
  }
}
