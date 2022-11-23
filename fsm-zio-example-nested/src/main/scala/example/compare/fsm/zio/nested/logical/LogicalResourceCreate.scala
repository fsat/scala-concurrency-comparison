package example.compare.fsm.zio.nested.logical

import example.compare.fsm.zio.nested.logical.LogicalResourceCreate.createPhysicalResource
import example.compare.fsm.zio.nested.logical.LogicalResourceFSM.{ Message, MessageSelf, RuntimeDependencies, State }
import example.compare.fsm.zio.nested.logical.interpreter.LogicalResource
import example.compare.fsm.zio.nested.physical.PhysicalResourceFSM
import example.compare.fsm.zio.nested.physical.interpreter.PhysicalResource
import fsm.zio.FSMContext
import zio._

object LogicalResourceCreate {
  def createPhysicalResource(logicalResource: LogicalResource): PhysicalResource = {
    // TODO: implement proper
    new PhysicalResource()
  }
}

class LogicalResourceCreate()(implicit deps: RuntimeDependencies) {
  private[logical] def apply(state: State.CreatingState, message: Message.Request, ctx: FSMContext[Message.Request]): UIO[State] = {
    for {
      nextState <- ctx.setup(state, state.isSetup) {
        val endpointName = state.request.endpointName
        val physicalResource = createPhysicalResource(state.request.logicalResource)
        for {
          _ <- state.physicalResource.tell(
            PhysicalResourceFSM.Message.CreateOrUpdateRequest(
              endpointName,
              physicalResource,
              ctx.self.map(MessageSelf.FromPhysicalResourceFSM.CreateOrUpdateResponse)))
        } yield state.copy(isSetup = true)
      }

      result <- message match {
        case m: Message.CreateOrUpdateRequest =>
          for {
            _ <- m.replyTo.succeed(Message.CreateOrUpdateResponse.Busy(m))
          } yield nextState
      }
    } yield result
  }
}
