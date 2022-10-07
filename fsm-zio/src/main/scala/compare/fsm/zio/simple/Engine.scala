package compare.fsm.zio.simple

import compare.fsm.zio.simple.Engine.PendingMessage
import zio._

object Engine {
  object PendingMessage {
    final case class Tell[MessageRequest](request: MessageRequest) extends PendingMessage
    final case class Ask[MessageRequest, MessageResponse](
      request: MessageRequest,
      replyTo: Promise[Throwable, Option[MessageResponse]]) extends PendingMessage
  }
  sealed trait PendingMessage extends Product with Serializable

  def create[State, MessageRequest[+_], MessageResponse](
    state: State,
    fsm: FSM[State, MessageRequest, MessageResponse],
    mailboxSize: Int = 32000): Task[Engine[State, MessageRequest, MessageResponse]] = {
    for {
      mailbox <- Queue.dropping[PendingMessage](mailboxSize)
      s <- Ref.make(state)
      engine = new Engine(mailbox, s, fsm)

      // Run the queue processing loop in parallel in the background
      parallelScope <- Scope.makeWith(ExecutionStrategy.Parallel)
      _ <- processMessage(engine)
        .forever
        .forkIn(parallelScope)
    } yield engine
  }

  private def processMessage[State, MessageRequest[+_], MessageResponse](engine: Engine[State, MessageRequest, MessageResponse]): Task[Unit] = {
    import engine._
    val t = for {
      pendingMessage <- mailbox.take
      s <- state.get
      _ <- pendingMessage match {
        case m: PendingMessage.Tell[MessageRequest[_] @unchecked] =>
          for {
            r <- fsm.apply(s, m.request)
            (stateNext, _) = r
            _ <- state.set(stateNext)
          } yield ()

        case m: PendingMessage.Ask[MessageRequest[_] @unchecked, MessageResponse @unchecked] =>
          for {
            r <- fsm.apply(s, m.request)
            (stateNext, response) = r
            _ <- m.replyTo.succeed(response)
            _ <- state.set(stateNext)
          } yield ()
      }
    } yield ()

    t
  }

}

class Engine[State, MessageRequest[+_], MessageResponse](
  private[simple] val mailbox: Queue[PendingMessage],
  private[simple] val state: Ref[State],
  private[simple] val fsm: FSM[State, MessageRequest, MessageResponse]) {
  def tell(message: MessageRequest[Nothing]): UIO[Unit] = {
    for {
      _ <- mailbox.offer(PendingMessage.Tell(message))
    } yield ()
  }

  def ask[T <: MessageResponse](message: MessageRequest[T]): Task[Option[T]] = {
    for {
      p <- Promise.make[Throwable, Option[T]]
      _ <- mailbox.offer(PendingMessage.Ask(message, p))
      result <- p.await
    } yield result
  }

}
