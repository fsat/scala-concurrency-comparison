package example.compare.fsm.zio.nested.physical

import fsm.zio.{ FSM, FSMContext }
import PhysicalResourceFSM._
import example.compare.fsm.zio.nested.physical.interpreter.{ PhysicalResource, PhysicalResourceAlgebra }
import zio.{ Task, _ }

object PhysicalResourceFSM {
  object Message {
    final case class CreateOrUpdateRequest(endpointName: String, replyTo: Promise[Throwable, CreateOrUpdateResponse]) extends Request
    object CreateOrUpdateResponse {
      final case class Creating() extends CreateOrUpdateResponse
      final case class Updating() extends CreateOrUpdateResponse
      final case class Busy() extends CreateOrUpdateResponse
      final case class Failure() extends CreateOrUpdateResponse
    }
    sealed trait CreateOrUpdateResponse extends Product with Serializable

    final case class GetStatusRequest(replyTo: Promise[Throwable, GetStatusResponse]) extends Request
    object GetStatusResponse {
      final case class Initial() extends GetStatusResponse
      final case class Creating() extends GetStatusResponse
      final case class Updating() extends GetStatusResponse
      final case class DownloadingArtifacts() extends GetStatusResponse
      final case class Running() extends GetStatusResponse
      final case class Failure() extends GetStatusResponse
    }
    sealed trait GetStatusResponse extends Product with Serializable

    sealed trait Request extends Message
  }
  sealed trait Message extends Product with Serializable

  object State {
    final case class InitialState() extends State
    final case class CreatingState(update: PhysicalResource) extends State
    final case class UpdatingState(id: PhysicalResource.Id, existing: PhysicalResource, update: PhysicalResource) extends State
    final case class DownloadingArtifactsState(id: PhysicalResource.Id) extends State
    final case class RunningState(id: PhysicalResource.Id) extends State
    final case class FailureState(error: Throwable, id: Option[PhysicalResource.Id]) extends State
  }
  sealed trait State extends Product with Serializable

  final case class RuntimeDependencies(
    physicalResource: PhysicalResourceAlgebra[Task])
}

class PhysicalResourceFSM()(implicit deps: RuntimeDependencies) extends FSM[State, Message.Request] {
  import deps._

  override def apply(state: State, message: Message.Request, ctx: FSMContext[Message.Request]): UIO[State] =
    state match {
      case s: State.InitialState => apply(s, message, ctx)
      case s: State.CreatingState => apply(s, message, ctx)
      case s: State.UpdatingState => apply(s, message, ctx)
      case s: State.DownloadingArtifactsState => apply(s, message, ctx)
      case s: State.RunningState => apply(s, message, ctx)
      case s: State.FailureState => apply(s, message, ctx)
    }

  private[physical] def apply(state: State.InitialState, message: Message.Request, ctx: FSMContext[Message.Request]): UIO[State] = {
    message match {
      case r: Message.CreateOrUpdateRequest =>
        for {
          isEndpointPresent <- physicalResource.find(r.endpointName)
        } yield state

        ???
      case r: Message.GetStatusRequest =>
        for {
          _ <- r.replyTo.succeed(Message.GetStatusResponse.Initial())
        } yield state
    }
  }

  private[physical] def apply(state: State.CreatingState, message: Message.Request, ctx: FSMContext[Message.Request]): UIO[State] = {
    message match {
      case r: Message.CreateOrUpdateRequest =>
        for {
          _ <- r.replyTo.succeed(Message.CreateOrUpdateResponse.Busy())
        } yield state

      case r: Message.GetStatusRequest =>
        for {
          _ <- r.replyTo.succeed(Message.GetStatusResponse.Creating())
        } yield state
    }
  }

  private[physical] def apply(state: State.UpdatingState, message: Message.Request, ctx: FSMContext[Message.Request]): UIO[State] = {
    message match {
      case r: Message.CreateOrUpdateRequest =>
        for {
          _ <- r.replyTo.succeed(Message.CreateOrUpdateResponse.Busy())
        } yield state

      case r: Message.GetStatusRequest =>
        for {
          _ <- r.replyTo.succeed(Message.GetStatusResponse.Updating())
        } yield state
    }
  }

  private[physical] def apply(state: State.DownloadingArtifactsState, message: Message.Request, ctx: FSMContext[Message.Request]): UIO[State] = {
    message match {
      case r: Message.CreateOrUpdateRequest =>
        for {
          _ <- r.replyTo.succeed(Message.CreateOrUpdateResponse.Busy())
        } yield state

      case r: Message.GetStatusRequest =>
        for {
          _ <- r.replyTo.succeed(Message.GetStatusResponse.DownloadingArtifacts())
        } yield state
    }
  }

  private[physical] def apply(state: State.RunningState, message: Message.Request, ctx: FSMContext[Message.Request]): UIO[State] = {
    message match {
      case r: Message.CreateOrUpdateRequest => ???
      case r: Message.GetStatusRequest =>
        for {
          _ <- r.replyTo.succeed(Message.GetStatusResponse.Running())
        } yield state
    }
  }

  private[physical] def apply(state: State.FailureState, message: Message.Request, ctx: FSMContext[Message.Request]): UIO[State] = {
    message match {
      case r: Message.CreateOrUpdateRequest =>
        state.id match {
          case None => apply(State.InitialState(), r, ctx)
          case Some(v) => apply(State.RunningState(v), r, ctx)
        }

      case r: Message.GetStatusRequest =>
        for {
          _ <- r.replyTo.succeed(Message.GetStatusResponse.Failure())
        } yield state
    }
  }
}
