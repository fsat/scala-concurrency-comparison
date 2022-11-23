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
    mailboxSize: Int = 32000): UIO[FSMRef.Local[MessageRequest]] = {
    for {
      mailbox <- Queue.dropping[PendingMessage](mailboxSize)
      s <- Ref.make(state)
      engine = new Engine(mailbox, s, fsm)

      // Run the queue processing loop in parallel in the background
      parallelScope <- Scope.makeWith(ExecutionStrategy.Parallel)
      _ <- processMessage(engine)
        .forever
        .forkIn(parallelScope)
    } yield new FSMRef.Local(engine)
  }

  private def processMessage[State, MessageRequest](engine: Engine[State, MessageRequest]): Task[Unit] = {
    import engine._
    val t = for {
      ctx <- ZIO.succeed(new FSMContext(new FSMRef.Self(engine)))
      pendingMessage <- mailbox.take
      s <- state.get
      stateNext <- pendingMessage match {
        case m: PendingMessage.Tell[MessageRequest @unchecked] => fsm.apply(s, m.request, ctx)
        case m: PendingMessage.Ask[MessageRequest @unchecked] => fsm.apply(s, m.request, ctx)
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

  private[zio] def tell(message: MessageRequest): UIO[Unit] = {
    for {
      _ <- mailbox.offer(PendingMessage.Tell(message))
    } yield ()
  }

  private[zio] def ask[MessageResponse](createMessage: Promise[Throwable, MessageResponse] => MessageRequest): Task[MessageResponse] = {
    for {
      p <- Promise.make[Throwable, MessageResponse]
      m <- ZIO.attempt(createMessage(p))
      _ <- mailbox.offer(PendingMessage.Ask(m))
      result <- p.await
    } yield result
  }

  def stop(): UIO[Unit] = {
    for {
      _ <- mailbox.takeAll
      _ <- mailbox.shutdown
    } yield ()
  }
}
