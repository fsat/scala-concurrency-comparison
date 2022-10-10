package example.compare.fsm.zio.nested.physical

import example.compare.fsm.zio.nested.physical.PhysicalResourceFSM._
import fsm.zio.{ FSM, FSMContext }
import zio._

import scala.util.{ Failure, Success }

class PhysicalResourceInitialFSM()(implicit deps: RuntimeDependencies) {
  import deps._

  private[physical] def apply(state: State.InitialState, message: Message.Request, ctx: FSMContext[Message.Request]): UIO[State] = {
    message match {
      case r: Message.CreateOrUpdateRequest =>
        for {
          _ <- ctx.pipeToSelfAsync(physicalResource.find(r.endpointName))(MessageSelf.InitialState.FindEndpointComplete)
        } yield State.InitialState.FindEndpointState(r)

      case r: Message.GetStatusRequest =>
        for {
          _ <- r.replyTo.succeed(Message.GetStatusResponse.Initial())
        } yield state

      case _: MessageSelf.InitialState.FindEndpointComplete |
        _: MessageSelf.InitialState.PhysicalResourceCreateComplete |
        _: MessageSelf.InitialState.PhysicalResourceUpdateComplete =>
        ZIO.succeed(state)
    }
  }

  private[physical] def apply(state: State.InitialState.FindEndpointState, message: Message.Request, ctx: FSMContext[Message.Request]): UIO[State] = {
    message match {
      case r: MessageSelf.InitialState.FindEndpointComplete =>
        r.result match {
          case Success(Some(existing)) =>
            for {
              _ <- ctx.pipeToSelfAsync(deps.physicalResource.update(state.request.endpointName, state.request.physicalResource))(MessageSelf.InitialState.PhysicalResourceUpdateComplete)
            } yield State.UpdatingState(existing, state.request)
          case Success(None) =>
            for {
              _ <- ctx.pipeToSelfAsync(deps.physicalResource.create(state.request.endpointName, state.request.physicalResource))(MessageSelf.InitialState.PhysicalResourceCreateComplete)
            } yield State.CreatingState(state.request)
          case Failure(e) =>
            for {
              _ <- state.request.replyTo.fail(e)
            } yield State.FailureState(e, None)
        }

      case r: Message.CreateOrUpdateRequest =>
        for {
          _ <- r.replyTo.succeed(Message.CreateOrUpdateResponse.Busy())
        } yield state

      case r: Message.GetStatusRequest =>
        for {
          _ <- r.replyTo.succeed(Message.GetStatusResponse.Initial())
        } yield state

      case _: MessageSelf.InitialState.FindEndpointComplete |
        _: MessageSelf.InitialState.PhysicalResourceCreateComplete |
        _: MessageSelf.InitialState.PhysicalResourceUpdateComplete =>
        ZIO.succeed(state)
    }
  }
}
