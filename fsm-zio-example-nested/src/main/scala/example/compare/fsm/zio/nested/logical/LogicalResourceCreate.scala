package example.compare.fsm.zio.nested.logical

import example.compare.fsm.zio.nested.logical.LogicalResourceCreate.createPhysicalResource
import example.compare.fsm.zio.nested.logical.LogicalResourceFSM.{ Message, RuntimeDependencies, State }
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
        val attempt = for {
          // TODO: We need a long running exchange between logical & physical resource actor
          reply <- state.physicalResource.ask(PhysicalResourceFSM.Message.CreateOrUpdateRequest(endpointName, physicalResource, _))
        } yield state.copy(isSetup = true)
        
        attempt.flatMapError { err =>
          // TODO: logging + event
          ZIO.succeed(State.FailureState(physicalResource = None, error = err))
        }
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
