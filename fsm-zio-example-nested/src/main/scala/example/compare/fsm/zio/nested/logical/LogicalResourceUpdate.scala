package example.compare.fsm.zio.nested.logical

import example.compare.fsm.zio.nested.logical.LogicalResourceFSM.{ Message, RuntimeDependencies, State }
import fsm.zio.FSMContext
import zio.UIO

class LogicalResourceUpdate()(implicit deps: RuntimeDependencies) {
  private[logical] def apply(state: State.UpdatingState, message: Message.Request, ctx: FSMContext[Message.Request]): UIO[State] = ???
}
