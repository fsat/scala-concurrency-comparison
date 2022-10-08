package fsm.zio

import fsm.zio.Engine.PendingMessage
import zio._

object Engine {
  object PendingMessage {
    final case class Tell[MessageRequest](request: MessageRequest) extends PendingMessage
    final case class Ask[MessageRequest](request: MessageRequest) extends PendingMessage
  }
  sealed trait PendingMessage extends Product with Serializable

  def create[State, MessageRequest](
    state: State,
    fsm: FSM[State, MessageRequest],
    mailboxSize: Int = 32000): Task[Engine[State, MessageRequest]] = {
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

  private def processMessage[State, MessageRequest](engine: Engine[State, MessageRequest]): Task[Unit] = {
    import engine._
    val t = for {
      pendingMessage <- mailbox.take
      s <- state.get
      stateNext <- pendingMessage match {
        case m: PendingMessage.Tell[MessageRequest @unchecked] => fsm.apply(s, m.request)
        case m: PendingMessage.Ask[MessageRequest @unchecked] => fsm.apply(s, m.request)
      }
      _ <- state.set(stateNext)
    } yield ()

    t
  }

}

class Engine[State, MessageRequest](
  private[zio] val mailbox: Queue[PendingMessage],
  private[zio] val state: Ref[State],
  private[zio] val fsm: FSM[State, MessageRequest]) {
  def tell(message: MessageRequest): UIO[Unit] = {
    for {
      _ <- mailbox.offer(PendingMessage.Tell(message))
    } yield ()
  }

  def ask[MessageResponse](createMessage: Promise[Throwable, MessageResponse] => MessageRequest): Task[MessageResponse] = {
    for {
      p <- Promise.make[Throwable, MessageResponse]
      m <- ZIO.attempt(createMessage(p))
      _ <- mailbox.offer(PendingMessage.Ask(m))
      result <- p.await
    } yield result
  }

}
